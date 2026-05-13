// File: app/src/main/java/com/viplove/licadvisornative/util/FileUploadManager.kt
package com.viplove.licadvisornative.util

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.PremiumSummary
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.BatchPoliciesRequest
import com.viplove.licadvisornative.network.BatchSummariesRequest
import com.viplove.licadvisornative.network.BatchUpdatePaymentDatesRequest
import com.viplove.licadvisornative.network.CheckDuplicatesRequest
import com.viplove.licadvisornative.network.CheckSummaryDuplicatesRequest
import com.viplove.licadvisornative.network.PaymentDateUpdate
import com.viplove.licadvisornative.network.toPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class UploadResult {
    data class RequiresConfirmation(val pendingItems: List<Any>, val duplicates: List<String>) : UploadResult()
    object Success : UploadResult()
    data class Error(val message: String) : UploadResult()
}

object FileUploadManager {

    private const val TAG = "FileUploadManager"
    private val api = ApiClient.api

    suspend fun processProposals(uri: Uri, contentResolver: ContentResolver, adminId: String): UploadResult {
        return try {
            val fileContent = readTextFromUri(uri, contentResolver)
            val parsedPolicies = ProposalParser.parse(fileContent)
            if (parsedPolicies.isNotEmpty()) {
                val policyNumbers = parsedPolicies.map { it.policyNumber }
                // Check for duplicates via API
                val response = api.checkPolicyDuplicates(CheckDuplicatesRequest(policyNumbers))
                val existingPolicyNumbers = if (response.isSuccessful) {
                    response.body()?.duplicates ?: emptyList()
                } else {
                    emptyList()
                }

                if (existingPolicyNumbers.isEmpty()) {
                    uploadPolicies(parsedPolicies, adminId, overwrite = true, duplicates = emptyList())
                    UploadResult.Success
                } else {
                    UploadResult.RequiresConfirmation(parsedPolicies, existingPolicyNumbers)
                }
            } else {
                UploadResult.Error("No valid policies found in the file.")
            }
        } catch (e: Exception) {
            UploadResult.Error("Error processing file: ${e.message}")
        }
    }

    suspend fun uploadPolicies(policies: List<Policy>, adminId: String, overwrite: Boolean, duplicates: List<String>) {
        val policiesToUpload = if (overwrite) {
            policies
        } else {
            policies.filter { it.policyNumber !in duplicates }
        }
        if (policiesToUpload.isEmpty()) return

        // Upload in chunks of 50
        policiesToUpload.chunked(50).forEach { chunk ->
            val payloads = chunk.map { policy ->
                policy.adminId = adminId
                policy.toPayload()
            }
            api.batchStorePolicies(BatchPoliciesRequest(policies = payloads, overwrite = overwrite))
        }
    }

    /**
     * Reads the file and determines the format.
     * Always parses for agent premium summaries.
     * If it detects the detailed format, it also parses for individual policy payment
     * dates and triggers an update via API.
     */
    suspend fun processPremiumSummary(uri: Uri, contentResolver: ContentResolver, adminId: String): UploadResult {
        return try {
            val mimeType = contentResolver.getType(uri)
            val fileContent = if (mimeType == "application/pdf") {
                readTextFromPdfUri(uri, contentResolver)
            } else {
                readTextFromUri(uri, contentResolver)
            }

            // Check for detailed format and update policies
            if (fileContent.contains("DEVELOPMENT OFFICER FIRST YEAR PREMIUM INCOME STATEMENT")) {
                Log.d(TAG, "Detailed premium report detected. Parsing policy payment dates.")
                val policyUpdates = PremiumSummaryParser.parsePolicyPaymentDetails(fileContent)
                if (policyUpdates.isNotEmpty()) {
                    updatePoliciesWithPaymentDates(policyUpdates)
                }
            }

            // Always parse for agent summaries
            val parsedSummaries = PremiumSummaryParser.parse(fileContent)
            if (parsedSummaries.isNotEmpty()) {
                val reportMonth = parsedSummaries.first().reportMonth
                // Check for duplicates via API
                val response = api.checkSummaryDuplicates(CheckSummaryDuplicatesRequest(reportMonth))
                val existingAgencyCodes = if (response.isSuccessful) {
                    response.body()?.existingAgencyCodes ?: emptyList()
                } else {
                    emptyList()
                }
                val duplicateCodes = parsedSummaries
                    .filter { it.agencyCode in existingAgencyCodes }
                    .map { "${it.reportMonth}-${it.agencyCode}" }

                if (duplicateCodes.isNotEmpty()) {
                    UploadResult.RequiresConfirmation(parsedSummaries, duplicateCodes)
                } else {
                    uploadConfirmedSummaries(parsedSummaries, adminId, overwrite = true, duplicates = emptyList())
                    UploadResult.Success
                }
            } else {
                UploadResult.Error("No valid agent premium summaries found in the file.")
            }
        } catch (e: Exception) {
            UploadResult.Error("Error processing summary file: ${e.message}")
        }
    }

    /**
     * Updates matching policies with the latest paid date via API batch endpoint.
     */
    private suspend fun updatePoliciesWithPaymentDates(policyUpdates: List<PolicyPaymentInfo>) {
        if (policyUpdates.isEmpty()) return

        Log.d(TAG, "Starting batch update for ${policyUpdates.size} policies.")
        val updates = policyUpdates.map { PaymentDateUpdate(it.policyNumber, it.dateOfCollection) }

        // Upload in chunks of 50
        updates.chunked(50).forEach { chunk ->
            try {
                api.batchUpdatePaymentDates(BatchUpdatePaymentDatesRequest(chunk))
            } catch (e: Exception) {
                Log.e(TAG, "Error updating policy payment dates.", e)
            }
        }
    }

    suspend fun uploadConfirmedSummaries(summaries: List<PremiumSummary>, adminId: String, overwrite: Boolean, duplicates: List<String>) {
        val summariesToUpload = if (overwrite) {
            summaries
        } else {
            summaries.filter { "${it.reportMonth}-${it.agencyCode}" !in duplicates }
        }
        if (summariesToUpload.isEmpty()) return

        // Upload in chunks of 50
        summariesToUpload.chunked(50).forEach { chunk ->
            val payloads = chunk.map { summary ->
                summary.adminId = adminId
                summary.toPayload()
            }
            api.batchStoreSummaries(BatchSummariesRequest(summaries = payloads, overwrite = overwrite))
        }
    }

    private suspend fun readTextFromUri(uri: Uri, contentResolver: ContentResolver): String = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }

    private suspend fun readTextFromPdfUri(uri: Uri, contentResolver: ContentResolver): String = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                PDFTextStripper().getText(document)
            }
        } ?: ""
    }
}
