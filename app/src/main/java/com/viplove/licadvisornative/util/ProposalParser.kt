package com.viplove.licadvisornative.util

import android.util.Log
import com.viplove.licadvisornative.model.Policy
import java.text.SimpleDateFormat
import java.util.*

object ProposalParser {

    fun parse(fileContent: String): List<Policy> {
        val policies = mutableListOf<Policy>()
        val lines = fileContent.split('\n')
        var i = 0
        var currentAgencyCode = ""
        var currentAgentName = ""

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.contains("Name of the agent", ignoreCase = true)) {
                currentAgentName = line.split("Name of the agent").getOrNull(1)?.trim()?.removePrefix(":")?.trim() ?: ""
            }

            if (line.contains("Agency Code No.")) {
                currentAgencyCode = line.split(":").getOrNull(1)?.trim() ?: ""
            }

            val parts = line.split('|')
            if (parts.size > 6 && parts[1].trim().matches(Regex("\\d+"))) {
                try {
                    val nextLine = if (i + 1 < lines.size) lines[i + 1].trim().split('|') else emptyList()
                    val policyNo = parts.getOrNull(6)?.trim() ?: ""
                    if (policyNo.isNotEmpty()) {
                        val proposalNo = parts.getOrNull(1)?.trim()?.trimStart('0') ?: ""
                        val shortName = parts.getOrNull(2)?.trim() ?: ""
                        val plan = parts.getOrNull(9)?.trim() ?: ""
                        val mode = parts.getOrNull(10)?.trim() ?: ""
                        val premiumStr = parts.getOrNull(11)?.trim() ?: "0.0"
                        val docStr = nextLine.getOrNull(6)?.trim() ?: ""
                        val docTimestamp = formatDateToTimestamp(docStr)

                        // --- NEW LOGIC TO PARSE DATE OF COMPLETION ---
                        // Based on the file structure, "Date of Compltn." is in the 5th column.
                        val completionStr = parts.getOrNull(5)?.trim() ?: ""
                        val completionTimestamp = formatDateToTimestamp(completionStr)
                        // --- END OF NEW LOGIC ---

                        val isAnanda = proposalNo.matches(Regex("\\d+")) && proposalNo.length == 6

                        val formattedDoc = formatTimestampToDDMMYYYY(docTimestamp)
                        val enachDate = getEnachDate(formattedDoc, mode)

                        policies.add(
                            Policy(
                                policyNumber = policyNo,
                                plan = plan,
                                mode = mode,
                                doc = docTimestamp,
                                dateOfCompletion = completionTimestamp, // Set the new field
                                isAnanda = isAnanda,
                                premium = premiumStr.toDoubleOrNull() ?: 0.0,
                                agentCode = currentAgencyCode,
                                shortName = shortName,
                                enachDate = enachDate,
                                agentName = currentAgentName
                            )
                        )
                    }
                    i += 2
                    continue
                } catch (e: Exception) {
                    Log.e("ProposalParser", "Failed to parse line $i: ${e.message}")
                }
            }
            i++
        }
        return policies
    }

    private fun formatDateToTimestamp(dateStr: String): Long {
        // This helper now supports both YYYYMMDD and DDMMYYYY formats
        val format = when {
            dateStr.length == 8 && dateStr.matches(Regex("\\d+")) -> SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            dateStr.length == 10 && dateStr.contains("/") -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            else -> null
        }
        return try {
            format?.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatTimestampToDDMMYYYY(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) { "" }
    }

    private fun getEnachDate(docStr: String, modeStr: String): String {
        val relevantModes = listOf("m", "mly", "monthly")
        if (docStr.isBlank() || modeStr.isBlank() || modeStr.trim().lowercase() !in relevantModes) {
            return ""
        }
        return try {
            val day = docStr.split("/").firstOrNull()?.toIntOrNull()
            if (day == null) {
                return ""
            }

            when (day) {
                in 1..7 -> "7"
                in 8..15 -> "15"
                in 16..22 -> "22"
                in 23..31 -> "28"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}