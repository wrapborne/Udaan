package com.viplove.licadvisornative.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.viplove.licadvisornative.model.CommissionBillPdfResult
import com.viplove.licadvisornative.model.CommissionRowStatus
import com.viplove.licadvisornative.model.PremiumDuePdfResult
import com.viplove.licadvisornative.model.PremiumPdfImportApplyResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirebaseApi {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage
    private val functions: FirebaseFunctions = Firebase.functions

    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return try {
            val result = auth.signInWithEmailAndPassword(request.email, request.password).await()
            val userDoc = loadCurrentUserDocument() ?: run {
                auth.signOut()
                return errorResponse(404, "User profile not found.")
            }

            val apiUser = userDoc.toApiUser()
            if (!apiUser.isApproved && apiUser.role != "superadmin") {
                auth.signOut()
                return errorResponse(403, "Your account is pending approval.")
            }

            val token = result.user?.getIdToken(true)?.await()?.token.orEmpty()
            Response.success(
                AuthResponse(
                    accessToken = token,
                    tokenType = "Bearer",
                    expiresIn = 3600L,
                    user = apiUser
                )
            )
        } catch (e: Exception) {
            errorResponse(401, e.localizedMessage ?: "Login failed.")
        }
    }

    suspend fun register(request: RegisterRequest): Response<MessageResponse> {
        return try {
            val payload = mapOf(
                "name" to request.name,
                "phone" to request.phone,
                "email" to request.email,
                "password" to request.password,
                "userCode" to request.userCode,
                "adminDoCode" to request.adminDoCode,
                "role" to request.role
            )
            val result = functions
                .getHttpsCallable("registerNewUser")
                .call(payload)
                .await()
            val message = (result.data as? Map<*, *>)?.get("message")?.toString()
                ?: "Registration successful! Please wait for approval."
            Response.success(MessageResponse(message))
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Registration failed."
            val code = when {
                message.contains("already", ignoreCase = true) -> 409
                message.contains("Invalid Development Officer", ignoreCase = true) -> 404
                message.contains("required", ignoreCase = true) || message.contains("invalid", ignoreCase = true) -> 422
                else -> 500
            }
            errorResponse(code, message)
        }
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): Response<MessageResponse> {
        return try {
            auth.sendPasswordResetEmail(request.email).await()
            Response.success(MessageResponse("Password reset link sent! Please check your email."))
        } catch (e: Exception) {
            errorResponse(400, e.localizedMessage ?: "Unable to send reset link.")
        }
    }

    suspend fun lookupEmailByCode(request: LookupByCodeRequest): Response<LookupEmailResponse> {
        val normalized = normalizeCode(request.code)
        val variants = buildCodeVariants(normalized)
        val match = withTimeout(15_000) {
            findUserByCodeVariants("agencyCode", variants)
                ?: findUserByCodeVariants("doCode", variants)
                ?: findUserByCodeVariants("agency_code", variants)
                ?: findUserByCodeVariants("do_code", variants)
                ?: findUserByCodeVariants("userCode", variants)
                ?: findUserByCodeVariants("user_code", variants)
                ?: findUserByCodeVariants("code", variants)
                ?: findUserByNormalizedCode(variants)
        }
        return if (match != null) {
            Response.success(LookupEmailResponse(match.getString("email").orEmpty()))
        } else {
            errorResponse(404, "No account found.")
        }
    }

    suspend fun me(): Response<ApiUser> {
        val userDoc = loadCurrentUserDocument() ?: return errorResponse(401, "Not authenticated.")
        return Response.success(userDoc.toApiUser())
    }

    suspend fun logout(): Response<MessageResponse> {
        auth.signOut()
        return Response.success(MessageResponse("Logged out successfully."))
    }

    suspend fun refreshToken(): Response<AuthResponse> {
        val userDoc = loadCurrentUserDocument() ?: return errorResponse(401, "Not authenticated.")
        val firebaseUser = auth.currentUser ?: return errorResponse(401, "Not authenticated.")
        val token = firebaseUser.getIdToken(true).await().token.orEmpty()
        return Response.success(
            AuthResponse(
                accessToken = token,
                tokenType = "Bearer",
                expiresIn = 3600L,
                user = userDoc.toApiUser()
            )
        )
    }

    suspend fun getMyAgents(): Response<List<ApiUser>> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val allUsers = firestore.collection("users").get().await().documents.map { it.toApiUser() }
        val agents = when (current.role) {
            "superadmin" -> allUsers.filter { it.role == "admin" }
            "admin" -> allUsers.filter { it.role == "advisor" && it.adminId == current.id }
            else -> emptyList()
        }
        return Response.success(agents.sortedBy { it.name.lowercase() })
    }

    suspend fun getAllAdmins(): Response<List<ApiUser>> {
        val admins = firestore.collection("users").get().await().documents
            .map { it.toApiUser() }
            .filter { it.role == "admin" }
            .sortedBy { it.name.lowercase() }
        return Response.success(admins)
    }

    suspend fun getUser(id: String): Response<ApiUser> {
        val doc = firestore.collection("users").document(id).get().await()
        return if (doc.exists()) Response.success(doc.toApiUser()) else errorResponse(404, "User not found.")
    }

    suspend fun approveUser(id: String): Response<MessageResponse> {
        firestore.collection("users").document(id).set(mapOf("isApproved" to true), SetOptions.merge()).await()
        return Response.success(MessageResponse("User approved successfully."))
    }

    suspend fun updateStartDate(id: String, request: UpdateStartDateRequest): Response<ApiUser> {
        val ref = firestore.collection("users").document(id)
        ref.set(mapOf("startDate" to request.startDate), SetOptions.merge()).await()
        val doc = ref.get().await()
        return Response.success(doc.toApiUser())
    }

    suspend fun deleteUser(id: String): Response<MessageResponse> {
        return try {
            functions.getHttpsCallable("deleteUserAccount").call(mapOf("uid" to id)).await()
            Response.success(MessageResponse("User deleted successfully."))
        } catch (_: Exception) {
            firestore.collection("users").document(id).delete().await()
            Response.success(MessageResponse("User profile deleted successfully."))
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Response<ApiUser> {
        val ref = currentUserRef() ?: return errorResponse(401, "Not authenticated.")
        val updates = mutableMapOf<String, Any>()
        request.name?.let { updates["name"] = it }
        request.phone?.let { updates["phone"] = it }
        if (updates.isNotEmpty()) {
            ref.set(updates, SetOptions.merge()).await()
        }
        return Response.success(ref.get().await().toApiUser())
    }

    suspend fun updateProfilePicture(image: MultipartBody.Part): Response<UploadResponse> {
        val user = auth.currentUser ?: return errorResponse(401, "Not authenticated.")
        val bytes = image.bytes()
        val path = "profile_pictures/${user.uid}.jpg"
        val storageRef = storage.reference.child(path)
        storageRef.putBytes(bytes).await()
        val url = storageRef.downloadUrl.await().toString()
        firestore.collection("users").document(user.uid)
            .set(mapOf("profilePictureUrl" to url), SetOptions.merge())
            .await()
        return Response.success(UploadResponse(url = url, path = path))
    }

    suspend fun getPolicies(agentCode: String? = null): Response<List<ApiPolicy>> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val all = firestore.collection("policies").get().await().documents.map { it.toApiPolicy() }
        val filtered = all.filter { policy ->
            when {
                !agentCode.isNullOrBlank() -> codesMatch(policy.agentCode, agentCode)
                current.role == "superadmin" -> true
                current.role == "admin" -> policy.adminId == current.id
                else -> codesMatch(policy.agentCode, current.agencyCode)
            }
        }
        return Response.success(filtered.sortedByDescending { it.doc ?: 0L })
    }

    suspend fun batchStorePolicies(request: BatchPoliciesRequest): Response<BatchResult> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val duplicates = mutableListOf<String>()
        request.policies.chunked(50).forEach { chunk ->
            chunk.forEach { payload ->
                val existing = findPolicyDocumentByNumber(payload.policyNumber)
                if (existing != null && !request.overwrite) {
                    duplicates += payload.policyNumber
                } else {
                    val target = existing ?: firestore.collection("policies").document()
                    target.set(
                        mapOf(
                            "proposalNumber" to payload.proposalNumber,
                            "policyNumber" to payload.policyNumber,
                            "plan" to payload.plan.orEmpty(),
                            "mode" to payload.mode.orEmpty(),
                            "doc" to payload.doc,
                            "dateOfCompletion" to payload.dateOfCompletion,
                            "premium" to payload.premium,
                            "agentCode" to payload.agentCode,
                            "adminId" to current.adminRootId(),
                            "shortName" to payload.shortName.orEmpty(),
                            "enachDate" to payload.enachDate.orEmpty(),
                            "agentName" to payload.agentName.orEmpty(),
                            "isAnanda" to payload.isAnanda,
                            "isUlip" to payload.isUlip,
                            "createdAt" to (payload.createdAt ?: System.currentTimeMillis())
                        ),
                        SetOptions.merge()
                    ).await()
                }
            }
        }
        return Response.success(BatchResult(inserted = request.policies.size - duplicates.size, duplicates = duplicates))
    }

    suspend fun checkPolicyDuplicates(request: CheckDuplicatesRequest): Response<DuplicatesResponse> {
        val duplicates = request.policyNumbers.distinct().filter { policyNumber ->
            findPolicyDocumentByNumber(policyNumber) != null
        }
        return Response.success(DuplicatesResponse(duplicates = duplicates))
    }

    suspend fun batchUpdatePaymentDates(request: BatchUpdatePaymentDatesRequest): Response<BatchUpdateResult> {
        var updated = 0
        request.updates.forEach { change ->
            val doc = findPolicyDocumentByNumber(change.policyNumber)
            if (doc != null) {
                doc.set(mapOf("lastPremiumPaidDate" to change.lastPremiumPaidDate), SetOptions.merge()).await()
                updated++
            }
        }
        return Response.success(BatchUpdateResult(updated = updated))
    }

    suspend fun applyPremiumDueImport(result: PremiumDuePdfResult): Response<PremiumPdfImportApplyResult> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        if (!canImportForAgent(current, result.agentCode)) {
            return errorResponse(403, "You can import only your own advisor PDF.")
        }

        var updatedPolicies = 0
        var reconciledDueItems = 0
        val warnings = result.warnings.toMutableList()
        val importId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val adminId = current.adminRootId()
        val storedAgentCode = appStoredAgentCode(result.agentCode)

        result.rows.forEach { row ->
            val paidPayment = try {
                findPaidPaymentForDue(storedAgentCode, row.policyNumber, row.fupMonth)
            } catch (e: Exception) {
                warnings += "Could not cross-check payment history for policy ${row.policyNumber}, FUP ${row.fupMonth}: ${e.localizedMessage ?: "permission denied"}"
                null
            }
            val isAlreadyPaidByCommission = paidPayment != null
            if (isAlreadyPaidByCommission) reconciledDueItems++

            val dueRef = firestore.collection("premium_due_items")
                .document(dueDocumentId(storedAgentCode, row.policyNumber, row.fupMonth))
            dueRef.set(
                mapOf(
                    "agentCode" to storedAgentCode,
                    "sourceAgentCode" to result.agentCode,
                    "adminId" to adminId,
                    "agentName" to result.agentName,
                    "policyNumber" to row.policyNumber,
                    "policyHolderName" to row.policyHolderName,
                    "dateOfCommencement" to row.dateOfCommencement,
                    "planTerm" to row.planTerm,
                    "mode" to row.mode,
                    "fupMonth" to row.fupMonth,
                    "dueKey" to row.dueKey,
                    "premiumYearType" to row.premiumYearType.name,
                    "isLapsed" to row.isLapsed,
                    "status" to when {
                        isAlreadyPaidByCommission -> "PAID_BY_COMMISSION"
                        row.isLapsed -> "LAPSED"
                        else -> "DUE"
                    },
                    "paidByCommission" to isAlreadyPaidByCommission,
                    "matchedPaymentId" to (paidPayment?.id ?: ""),
                    "matchedPaymentDueDate" to (paidPayment?.getString("dueDate") ?: ""),
                    "matchedPaymentAdjustmentDate" to (paidPayment?.getString("adjustmentDate") ?: ""),
                    "reconciledAt" to if (isAlreadyPaidByCommission) now else 0L,
                    "installmentPremium" to row.installmentPremium,
                    "dueCount" to row.dueCount,
                    "gst" to row.gst,
                    "totalPremium" to row.totalPremium,
                    "estimatedCommission" to row.estimatedCommission,
                    "reportMonth" to result.reportMonth,
                    "sourceImportId" to importId,
                    "importedAt" to now
                ),
                SetOptions.merge()
            ).await()

            if (isAlreadyPaidByCommission) {
                val policyDoc = findPolicyDocumentByNumber(row.policyNumber)
                if (policyDoc != null) {
                    val paidDueDate = paidPayment?.getString("dueDate").orEmpty()
                    val adjustmentDate = paidPayment?.getString("adjustmentDate").orEmpty()
                    val paidDueDateMillis = parseDateMillis(paidDueDate)
                    val existingPolicy = policyDoc.get().await()
                    val existingPaidDateMillis = existingPolicy.getLong("lastPremiumPaidDate")
                        ?: existingPolicy.getLong("last_premium_paid_date")
                    val updates = mutableMapOf<String, Any>(
                        "policyStatus" to "ACTIVE",
                        "lastDueImportId" to importId,
                        "lastDueImportAt" to now,
                        "lastPaymentImportAt" to now
                    )
                    if (paidDueDateMillis != null && (existingPaidDateMillis == null || paidDueDateMillis >= existingPaidDateMillis)) {
                        updates["lastPaidDueDate"] = paidDueDate
                        updates["lastPaymentAdjustmentDate"] = adjustmentDate
                        updates["lastPremiumPaidDate"] = paidDueDateMillis
                    }
                    policyDoc.set(updates, SetOptions.merge()).await()
                    updatedPolicies++
                } else {
                    warnings += "Due row ${row.policyNumber} matched a paid commission row, but policy was not found."
                }
                warnings += "Policy ${row.policyNumber} FUP ${row.fupMonth} was already paid in commission history, so due row was reconciled as paid."
            } else if (row.isLapsed) {
                val policyDoc = findPolicyDocumentByNumber(row.policyNumber)
                if (policyDoc != null) {
                    policyDoc.set(
                        mapOf(
                            "policyStatus" to "LAPSED",
                            "lastDueImportId" to importId,
                            "lastDueImportAt" to now
                        ),
                        SetOptions.merge()
                    ).await()
                    updatedPolicies++
                } else {
                    warnings += "Policy ${row.policyNumber} was not found while marking lapsed."
                }
            }
        }

        return Response.success(
            PremiumPdfImportApplyResult(
                importedRows = result.rows.size,
                updatedPolicies = updatedPolicies,
                reconciledDueItems = reconciledDueItems,
                warnings = warnings.distinct()
            )
        )
    }

    suspend fun applyCommissionBillImport(result: CommissionBillPdfResult): Response<PremiumPdfImportApplyResult> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        if (!canImportForAgent(current, result.agentCode)) {
            return errorResponse(403, "You can import only your own advisor PDF.")
        }

        var updatedPolicies = 0
        var clearedDueItems = 0
        var reversalRows = 0
        val warnings = result.warnings.toMutableList()
        val importId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val adminId = current.adminRootId()
        val storedAgentCode = appStoredAgentCode(result.agentCode)

        for (row in result.rows) {
            val paymentRef = firestore.collection("premium_payment_history")
                .document(paymentDocumentId(storedAgentCode, row.policyNumber, row.dueDate, row.adjustmentDate, row.status.name))
            try {
                paymentRef.set(
                    mapOf(
                        "agentCode" to storedAgentCode,
                        "sourceAgentCode" to result.agentCode,
                        "adminId" to adminId,
                        "agentName" to result.agentName,
                        "policyNumber" to row.policyNumber,
                        "policyHolderName" to row.policyHolderName,
                        "planTerm" to row.planTerm,
                        "dueDate" to row.dueDate,
                        "riskDate" to row.riskDate,
                        "cbo" to row.cbo,
                        "adjustmentDate" to row.adjustmentDate,
                        "premium" to row.premium,
                        "commission" to row.commission,
                        "status" to row.status.name,
                        "paymentKey" to row.paymentKey,
                        "reportMonth" to result.reportMonth,
                        "batch" to result.batch,
                        "processedDate" to result.processedDate,
                        "sourceImportId" to importId,
                        "importedAt" to now
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                return errorResponse(403, "Commission import failed while saving payment history for policy ${row.policyNumber}: ${e.localizedMessage ?: "permission denied"}")
            }

            if (row.status == CommissionRowStatus.COOLING_OFF_REVERSAL) {
                reversalRows++
                val policyDoc = try {
                    findPolicyDocumentByNumber(row.policyNumber)
                } catch (e: Exception) {
                    return errorResponse(403, "Commission import failed while finding reversal policy ${row.policyNumber}: ${e.localizedMessage ?: "permission denied"}")
                }
                if (policyDoc != null) {
                    try {
                        policyDoc.set(
                            mapOf(
                                "policyStatus" to "COOLING_OFF_REVERSAL",
                                "lastReversalDueDate" to row.dueDate,
                                "lastReversalImportId" to importId,
                                "lastReversalImportAt" to now
                            ),
                            SetOptions.merge()
                        ).await()
                    } catch (e: Exception) {
                        return errorResponse(403, "Commission import failed while marking reversal for policy ${row.policyNumber}: ${e.localizedMessage ?: "permission denied"}")
                    }
                    updatedPolicies++
                }
                continue
            }

            val paidDueDateMillis = parseDateMillis(row.dueDate)
            val policyDoc = try {
                findPolicyDocumentByNumber(row.policyNumber)
            } catch (e: Exception) {
                return errorResponse(403, "Commission import failed while finding policy ${row.policyNumber}: ${e.localizedMessage ?: "permission denied"}")
            }
            if (policyDoc != null) {
                val existingPolicy = policyDoc.get().await()
                val existingPaidDateMillis = existingPolicy.getLong("lastPremiumPaidDate")
                    ?: existingPolicy.getLong("last_premium_paid_date")
                val shouldUsePaidDate = paidDueDateMillis != null &&
                    (existingPaidDateMillis == null || paidDueDateMillis >= existingPaidDateMillis)
                val updates = mutableMapOf<String, Any>(
                    "policyStatus" to "ACTIVE",
                    "lastPaymentImportId" to importId,
                    "lastPaymentImportAt" to now
                )
                if (shouldUsePaidDate) {
                    updates["lastPaidDueDate"] = row.dueDate
                    updates["lastPaymentAdjustmentDate"] = row.adjustmentDate
                    updates["lastPremiumPaidDate"] = paidDueDateMillis!!
                } else if (existingPaidDateMillis == null && paidDueDateMillis == null) {
                    updates["lastPaidDueDate"] = row.dueDate
                    updates["lastPaymentAdjustmentDate"] = row.adjustmentDate
                }
                try {
                    policyDoc.set(updates, SetOptions.merge()).await()
                } catch (e: Exception) {
                    return errorResponse(403, "Commission import failed while updating paid status for policy ${row.policyNumber}: ${e.localizedMessage ?: "permission denied"}")
                }
                updatedPolicies++
            } else {
                warnings += "Policy ${row.policyNumber} was not found while applying payment."
            }

            val dueMonth = dueMonthFromDueDate(row.dueDate)
            if (dueMonth.isNotBlank()) {
                try {
                    firestore.collection("premium_due_items")
                        .document(dueDocumentId(storedAgentCode, row.policyNumber, dueMonth))
                        .delete()
                        .await()
                    clearedDueItems++
                } catch (e: Exception) {
                    warnings += "Payment imported for policy ${row.policyNumber}, but the matching due item could not be cleared automatically: ${e.localizedMessage ?: "permission denied"}"
                }
            }
        }

        return Response.success(
            PremiumPdfImportApplyResult(
                importedRows = result.rows.size,
                updatedPolicies = updatedPolicies,
                clearedDueItems = clearedDueItems,
                reconciledDueItems = clearedDueItems,
                reversalRows = reversalRows,
                warnings = warnings.distinct()
            )
        )
    }

    suspend fun getDatasheets(
        isDraft: Boolean? = null,
        isArchived: Boolean? = null
    ): Response<List<ApiDatasheet>> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val docs = firestore.collection("datasheets").get().await().documents
        val filtered = docs.map { it.toApiDatasheet() }.filter { sheet ->
            val visibilityPass = when (current.role) {
                "superadmin" -> true
                "admin" -> sheet.adminId == current.id
                else -> sheet.createdByAdvisorId == current.id
            }
            val draftPass = isDraft == null || sheet.isDraft == isDraft
            val archivedPass = isArchived == null || sheet.isArchived == isArchived
            visibilityPass && draftPass && archivedPass
        }
        return Response.success(filtered.sortedByDescending { it.updatedAt?.toLongOrNull() ?: 0L })
    }

    suspend fun getDatasheet(id: String): Response<ApiDatasheet> {
        val doc = firestore.collection("datasheets").document(id).get().await()
        return if (doc.exists()) Response.success(doc.toApiDatasheet()) else errorResponse(404, "Datasheet not found.")
    }

    suspend fun createDatasheet(body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiDatasheet> {
        val id = body["id"] as? String ?: UUID.randomUUID().toString()
        val payload = body.toMutableMap()
        payload["updated_at"] = System.currentTimeMillis().toString()
        firestore.collection("datasheets").document(id).set(payload, SetOptions.merge()).await()
        return Response.success(firestore.collection("datasheets").document(id).get().await().toApiDatasheet())
    }

    suspend fun updateDatasheet(
        id: String,
        body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiDatasheet> {
        val payload = body.toMutableMap()
        payload["updated_at"] = System.currentTimeMillis().toString()
        firestore.collection("datasheets").document(id).set(payload, SetOptions.merge()).await()
        return Response.success(firestore.collection("datasheets").document(id).get().await().toApiDatasheet())
    }

    suspend fun deleteDatasheet(id: String): Response<MessageResponse> {
        firestore.collection("datasheets").document(id).delete().await()
        return Response.success(MessageResponse("Datasheet deleted successfully."))
    }

    suspend fun getPremiumSummaries(reportMonth: String? = null): Response<List<ApiPremiumSummary>> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val docs = firestore.collection("premium_summaries").get().await().documents
        val filtered = docs.map { it.toApiPremiumSummary() }.filter { summary ->
            val ownerPass = when (current.role) {
                "superadmin" -> true
                "admin" -> summary.adminId == current.id
                else -> codesMatch(summary.agencyCode, current.agencyCode)
            }
            val monthPass = reportMonth == null || summary.reportMonth == reportMonth
            ownerPass && monthPass
        }
        return Response.success(filtered.sortedByDescending { it.reportMonth })
    }

    suspend fun batchStoreSummaries(request: BatchSummariesRequest): Response<BatchResult> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val duplicates = mutableListOf<String>()
        request.summaries.forEach { payload ->
            val existing = firestore.collection("premium_summaries").get().await().documents.firstOrNull {
                it.getString("reportMonth") == payload.reportMonth &&
                    codesMatch(it.getString("agencyCode"), payload.agencyCode)
            }
            val key = "${payload.reportMonth}-${payload.agencyCode}"
            if (existing != null && !request.overwrite) {
                duplicates += key
            } else {
                val ref = existing?.reference ?: firestore.collection("premium_summaries").document()
                ref.set(
                    mapOf(
                        "reportMonth" to payload.reportMonth,
                        "fpSchPrem" to payload.fpSchPrem,
                        "fySchPrem" to payload.fySchPrem,
                        "agencyCode" to payload.agencyCode,
                        "adminId" to current.adminRootId()
                    ),
                    SetOptions.merge()
                ).await()
            }
        }
        return Response.success(BatchResult(inserted = request.summaries.size - duplicates.size, duplicates = duplicates))
    }

    suspend fun checkSummaryDuplicates(request: CheckSummaryDuplicatesRequest): Response<SummaryDuplicatesResponse> {
        val existingCodes = firestore.collection("premium_summaries").get().await().documents
            .filter { it.getString("reportMonth") == request.reportMonth }
            .mapNotNull { it.getString("agencyCode") }
            .distinct()
        return Response.success(SummaryDuplicatesResponse(existingAgencyCodes = existingCodes))
    }

    suspend fun getGraphicsTemplates(): Response<List<ApiGraphicsTemplate>> {
        val templates = firestore.collection("graphic_templates").get().await().documents
            .map { it.toApiGraphicsTemplate() }
            .sortedByDescending { it.createdAt?.toLongOrNull() ?: 0L }
        return Response.success(templates)
    }

    suspend fun uploadGraphicsTemplate(
        name: RequestBody,
        image: MultipartBody.Part,
        visibleToRole: RequestBody
    ): Response<ApiGraphicsTemplate> {
        val title = name.readUtf8()
        val role = visibleToRole.readUtf8()
        val upload = uploadAsset("graphics/templates", image.bytes(), image.extensionOrDefault("jpg"))
        val ref = firestore.collection("graphic_templates").document()
        val createdAt = System.currentTimeMillis().toString()
        ref.set(
            mapOf(
                "name" to title,
                "imageUrl" to upload.url,
                "visibleToRole" to role,
                "storagePath" to upload.path,
                "createdAt" to createdAt
            )
        ).await()
        return Response.success(ref.get().await().toApiGraphicsTemplate())
    }

    suspend fun deleteGraphicsTemplate(id: String): Response<MessageResponse> {
        deleteAssetDocument("graphic_templates", id)
        return Response.success(MessageResponse("Template deleted successfully."))
    }

    suspend fun getGraphicsFooters(): Response<List<ApiGraphicsFooter>> {
        val footers = firestore.collection("graphic_footers").get().await().documents
            .map { it.toApiGraphicsFooter() }
            .sortedByDescending { it.createdAt?.toLongOrNull() ?: 0L }
        return Response.success(footers)
    }

    suspend fun uploadGraphicsFooter(
        name: RequestBody,
        image: MultipartBody.Part,
        visibleToRole: RequestBody
    ): Response<ApiGraphicsFooter> {
        val upload = uploadAsset("graphics/footers", image.bytes(), image.extensionOrDefault("png"))
        val ref = firestore.collection("graphic_footers").document()
        val createdAt = System.currentTimeMillis().toString()
        ref.set(
            mapOf(
                "name" to name.readUtf8(),
                "imageUrl" to upload.url,
                "visibleToRole" to visibleToRole.readUtf8(),
                "storagePath" to upload.path,
                "createdAt" to createdAt
            )
        ).await()
        return Response.success(ref.get().await().toApiGraphicsFooter())
    }

    suspend fun deleteGraphicsFooter(id: String): Response<MessageResponse> {
        deleteAssetDocument("graphic_footers", id)
        return Response.success(MessageResponse("Footer deleted successfully."))
    }

    suspend fun getUlipPolicies(): Response<List<ApiUlipPolicy>> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val policies = firestore.collection("ulip_policies").get().await().documents
            .map { it.toApiUlipPolicy() }
            .filter {
                when (current.role) {
                    "superadmin" -> true
                    "admin" -> it.adminId == current.id
                    else -> codesMatch(it.agentCode, current.agencyCode)
                }
            }
        return Response.success(policies)
    }

    suspend fun createUlipPolicy(body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiUlipPolicy> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val ref = firestore.collection("ulip_policies").document()
        ref.set(
            body.toMutableMap().apply {
                put("adminId", current.adminRootId())
                putIfAbsent("agentCode", current.agencyCode)
                putIfAbsent("asOfDate", System.currentTimeMillis())
            },
            SetOptions.merge()
        ).await()
        return Response.success(ref.get().await().toApiUlipPolicy())
    }

    suspend fun getCirculars(): Response<List<ApiCircular>> {
        val circulars = firestore.collection("circulars").get().await().documents
            .map { it.toApiCircular() }
            .sortedByDescending { it.createdAt?.toLongOrNull() ?: 0L }
        return Response.success(circulars)
    }

    suspend fun uploadCircular(
        title: RequestBody,
        category: RequestBody,
        file: MultipartBody.Part
    ): Response<ApiCircular> {
        val upload = uploadAsset("circulars", file.bytes(), file.extensionOrDefault("pdf"))
        val ref = firestore.collection("circulars").document()
        val createdAt = System.currentTimeMillis().toString()
        ref.set(
            mapOf(
                "title" to title.readUtf8(),
                "category" to category.readUtf8(),
                "fileUrl" to upload.url,
                "fileName" to upload.fileName,
                "storagePath" to upload.path,
                "createdAt" to createdAt
            )
        ).await()
        return Response.success(ref.get().await().toApiCircular())
    }

    suspend fun deleteCircular(id: String): Response<MessageResponse> {
        deleteAssetDocument("circulars", id)
        return Response.success(MessageResponse("Circular deleted successfully."))
    }

    suspend fun getForms(): Response<List<ApiForm>> {
        val forms = firestore.collection("forms").get().await().documents
            .map { it.toApiForm() }
            .sortedByDescending { it.createdAt?.toLongOrNull() ?: 0L }
        return Response.success(forms)
    }

    suspend fun uploadForm(
        title: RequestBody,
        category: RequestBody,
        file: MultipartBody.Part
    ): Response<ApiForm> {
        val upload = uploadAsset("forms", file.bytes(), file.extensionOrDefault("pdf"))
        val ref = firestore.collection("forms").document()
        val createdAt = System.currentTimeMillis().toString()
        ref.set(
            mapOf(
                "title" to title.readUtf8(),
                "category" to category.readUtf8(),
                "fileUrl" to upload.url,
                "fileName" to upload.fileName,
                "storagePath" to upload.path,
                "createdAt" to createdAt
            )
        ).await()
        return Response.success(ref.get().await().toApiForm())
    }

    suspend fun deleteForm(id: String): Response<MessageResponse> {
        deleteAssetDocument("forms", id)
        return Response.success(MessageResponse("Form deleted successfully."))
    }

    suspend fun uploadFile(
        file: MultipartBody.Part,
        folder: RequestBody? = null
    ): Response<UploadResponse> {
        val upload = uploadAsset(folder?.readUtf8().orEmpty().ifBlank { "uploads" }, file.bytes(), file.extensionOrDefault("bin"))
        return Response.success(UploadResponse(url = upload.url, path = upload.path))
    }

    suspend fun getConfig(key: String): Response<ApiConfig> {
        val doc = firestore.collection("app_config").document(key).get().await()
        if (doc.exists()) {
            return Response.success(ApiConfig(key = key, value = doc.get("value") ?: ""))
        }
        return errorResponse(404, "Config not found.")
    }

    private suspend fun loadCurrentUserDocument(): DocumentSnapshot? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.takeIf { it.exists() }
    }

    private suspend fun currentUserProfile(): ApiUser? = loadCurrentUserDocument()?.toApiUser()

    private fun currentUserRef() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }

    private suspend fun findDocumentByField(
        collection: String,
        field: String,
        value: String
    ) = firestore.collection(collection).get().await().documents.firstOrNull {
        it.getString(field).equals(value, ignoreCase = true)
    }?.reference

    private suspend fun findPolicyDocumentByNumber(policyNumber: String) =
        firestore.collection("policies").get().await().documents.firstOrNull {
            it.getString("policyNumber").equals(policyNumber, ignoreCase = true) ||
                it.getString("policy_number").equals(policyNumber, ignoreCase = true)
        }?.reference

    private suspend fun findPaidPaymentForDue(
        agentCode: String,
        policyNumber: String,
        dueMonth: String
    ): DocumentSnapshot? {
        if (dueMonth.isBlank()) return null
        return firestore.collection("premium_payment_history")
            .whereEqualTo("agentCode", agentCode)
            .get()
            .await()
            .documents
            .firstOrNull { payment ->
                payment.getString("policyNumber").equals(policyNumber, ignoreCase = true) &&
                    payment.getString("status") == CommissionRowStatus.PAID.name &&
                    dueMonthFromDueDate(payment.getString("dueDate").orEmpty()) == dueMonth
            }
    }

    private suspend fun findUserByCodeVariants(
        field: String,
        variants: List<String>
    ): DocumentSnapshot? {
        variants.forEach { variant ->
            val snapshot = firestore.collection("users")
                .whereEqualTo(field, variant)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                return snapshot.documents.first()
            }
        }
        return null
    }

    private suspend fun findUserByNormalizedCode(variants: List<String>): DocumentSnapshot? {
        if (variants.isEmpty()) return null
        val users = firestore.collection("users").get().await().documents
        return users.firstOrNull { user ->
            val candidateCodes = listOf(
                user.getString("agencyCode"),
                user.getString("doCode"),
                user.getString("agency_code"),
                user.getString("do_code"),
                user.getString("userCode"),
                user.getString("user_code"),
                user.getString("code")
            )
            candidateCodes.any { candidate ->
                variants.any { variant -> codesMatch(candidate, variant) }
            }
        }
    }

    private fun codesMatch(left: String?, right: String?): Boolean {
        val normalizedLeft = normalizeCode(left)
        val normalizedRight = normalizeCode(right)
        return normalizedLeft.isNotEmpty() && normalizedLeft == normalizedRight
    }

    private fun canImportForAgent(current: ApiUser, agentCode: String): Boolean {
        return when (current.role) {
            "superadmin", "admin" -> true
            else -> codesMatchForImport(current.agencyCode, agentCode)
        }
    }

    private fun codesMatchForImport(left: String?, right: String?): Boolean {
        val normalizedLeft = normalizeCodeForImport(left)
        val normalizedRight = normalizeCodeForImport(right)
        return normalizedLeft.isNotEmpty() && normalizedLeft == normalizedRight
    }

    private fun normalizeCodeForImport(code: String?): String {
        val cleaned = code.orEmpty()
            .trim()
            .replace(" ", "")
            .uppercase(Locale.ROOT)
            .removePrefix("LIC")
        val withoutLeadingZeros = cleaned.trimStart('0')
        return if (withoutLeadingZeros.isNotEmpty()) withoutLeadingZeros else cleaned
    }

    private fun appStoredAgentCode(code: String?): String {
        return code.orEmpty()
            .trim()
            .replace(" ", "")
            .uppercase(Locale.ROOT)
            .removePrefix("LIC")
    }

    private fun dueDocumentId(agentCode: String, policyNumber: String, dueMonth: String): String {
        return listOf(agentCode, policyNumber, dueMonth).joinToString("_").safeDocumentId()
    }

    private fun paymentDocumentId(
        agentCode: String,
        policyNumber: String,
        dueDate: String,
        adjustmentDate: String,
        status: String
    ): String {
        return listOf(agentCode, policyNumber, dueDate, adjustmentDate, status).joinToString("_").safeDocumentId()
    }

    private fun parseDateMillis(date: String): Long? {
        val cleaned = date.trim()
        val patterns = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy")
        patterns.forEach { pattern ->
            val parsed = try {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(cleaned)?.time
            } catch (_: Exception) {
                null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    private fun dueMonthFromDueDate(dueDate: String): String {
        val parts = dueDate.split("/")
        return if (parts.size == 3) "${parts[1]}/${parts[2]}" else ""
    }

    private fun String.safeDocumentId(): String {
        return replace("/", "-")
            .replace("\\", "-")
            .replace("#", "-")
            .replace("?", "-")
            .trim()
    }

    private fun buildCodeVariants(normalizedCode: String): List<String> {
        if (normalizedCode.isBlank()) return emptyList()
        val withLeadingZero = if (normalizedCode.startsWith("0")) normalizedCode else "0$normalizedCode"
        return listOf(normalizedCode, withLeadingZero).distinct()
    }

    private fun normalizeCode(code: String?): String {
        val cleaned = code.orEmpty()
            .trim()
            .replace(" ", "")
            .uppercase(Locale.ROOT)
        if (cleaned.isEmpty()) return ""
        val withoutLeadingZeros = cleaned.trimStart('0')
        return if (withoutLeadingZeros.isNotEmpty()) withoutLeadingZeros else "0"
    }

    private suspend fun uploadAsset(folder: String, bytes: ByteArray, extension: String): AssetUpload {
        val fileName = "${UUID.randomUUID()}.$extension"
        val path = "$folder/$fileName"
        val ref = storage.reference.child(path)
        ref.putBytes(bytes).await()
        val url = ref.downloadUrl.await().toString()
        return AssetUpload(url = url, path = path, fileName = fileName)
    }

    private suspend fun deleteAssetDocument(collection: String, id: String) {
        val ref = firestore.collection(collection).document(id)
        val snapshot = ref.get().await()
        snapshot.getString("storagePath")?.let { storage.reference.child(it).delete().await() }
        ref.delete().await()
    }

    private fun DocumentSnapshot.toApiUser(): ApiUser {
        val agencyCode = getString("agencyCode") ?: getString("agency_code")
        val doCode = getString("doCode") ?: getString("do_code")
        return ApiUser(
            id = id,
            email = getString("email").orEmpty(),
            name = getString("name").orEmpty(),
            phone = getString("phone").orEmpty(),
            role = canonicalizeRole(
                rawRole = getString("role") ?: getString("userRole") ?: getString("user_role"),
                agencyCode = agencyCode,
                doCode = doCode
            ),
            isApproved = (getBoolean("isApproved") ?: getBoolean("is_approved")) ?: false,
            agencyCode = agencyCode,
            doCode = doCode,
            adminId = getString("adminId") ?: getString("admin_id"),
            profilePicturePath = getString("profilePictureUrl")
                ?: getString("profile_picture_url")
                ?: getString("profile_picture_path"),
            startDate = (get("startDate") ?: get("start_date"))?.toString()
        )
    }

    private fun DocumentSnapshot.toApiPolicy(): ApiPolicy {
        val proposalNumber = getString("proposalNumber") ?: getString("proposal_number")
        val policyNumber = (getString("policyNumber") ?: getString("policy_number")).orEmpty()
        val derivedAnanda = sequenceOf(
            proposalNumber?.trim(),
            policyNumber.trim()
        ).filterNotNull().any { it.matches(Regex("[89]\\d{5}")) }
        return ApiPolicy(
            id = id,
            proposalNumber = proposalNumber,
            policyNumber = policyNumber,
            plan = getString("plan"),
            mode = getString("mode"),
            doc = getLong("doc"),
            dateOfCompletion = getLong("dateOfCompletion") ?: getLong("date_of_completion"),
            premium = getDouble("premium") ?: 0.0,
            agentCode = (getString("agentCode") ?: getString("agent_code")).orEmpty(),
            adminId = (getString("adminId") ?: getString("admin_id")).orEmpty(),
            shortName = getString("shortName") ?: getString("short_name"),
            enachDate = getString("enachDate") ?: getString("enach_date"),
            agentName = getString("agentName") ?: getString("agent_name"),
            isAnanda = derivedAnanda ||
                getBoolean("isAnanda") == true ||
                getBoolean("is_ananda") == true ||
                getBoolean("ananda") == true,
            lastPremiumPaidDate = getLong("lastPremiumPaidDate") ?: getLong("last_premium_paid_date"),
            isUlip = getBoolean("isUlip") ?: getBoolean("is_ulip")
                ?: com.viplove.licadvisornative.util.UlipPlanDetector.isUlipPlan(getString("plan")),
            createdAt = getLong("createdAt") ?: getLong("created_at")
        )
    }

    private fun DocumentSnapshot.toApiPremiumSummary(): ApiPremiumSummary {
        return ApiPremiumSummary(
            id = id,
            reportMonth = getString("reportMonth").orEmpty(),
            fpSchPrem = getDouble("fpSchPrem") ?: 0.0,
            fySchPrem = getDouble("fySchPrem") ?: 0.0,
            adminId = getString("adminId").orEmpty(),
            agencyCode = getString("agencyCode").orEmpty()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toApiDatasheet(): ApiDatasheet {
        return ApiDatasheet(
            id = id,
            createdByAdvisorId = (get("created_by_advisor_id") ?: get("createdByAdvisorId") ?: "").toString(),
            adminId = (get("admin_id") ?: get("adminId") ?: "").toString(),
            isDraft = (get("is_draft") as? Boolean) ?: (getBoolean("isDraft") ?: true),
            isArchived = (get("is_archived") as? Boolean) ?: (getBoolean("isArchived") ?: false),
            initialQuestions = (get("initial_questions") as? Map<String, Any>) ?: (get("initialQuestions") as? Map<String, Any>),
            planDetails = (get("plan_details") as? Map<String, Any>) ?: (get("planDetails") as? Map<String, Any>),
            sumAssured = (get("sum_assured") ?: get("sumAssured"))?.toString(),
            mode = getString("mode"),
            dateOfCommencement = getLong("date_of_commencement") ?: getLong("dateOfCommencement"),
            nachDate = getLong("nach_date") ?: getLong("nachDate"),
            isNachMandatory = (get("is_nach_mandatory") ?: get("isNachMandatory"))?.toString(),
            pwbRequired = (get("pwb_required") as? Boolean) ?: (getBoolean("pwbRequired") ?: false),
            proposerDetails = (get("proposer_details") as? Map<String, Any>) ?: (get("proposerDetails") as? Map<String, Any>),
            lifeAssuredDetails = (get("life_assured_details") as? Map<String, Any>) ?: (get("lifeAssuredDetails") as? Map<String, Any>),
            previousPolicies = (get("previous_policies") as? List<Map<String, Any>>) ?: (get("previousPolicies") as? List<Map<String, Any>>),
            updatedAt = (get("updated_at") ?: get("updatedAt"))?.toString()
        )
    }

    private fun DocumentSnapshot.toApiGraphicsTemplate(): ApiGraphicsTemplate {
        return ApiGraphicsTemplate(
            id = id,
            name = getString("name").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
            visibleToRole = getString("visibleToRole").orEmpty(),
            createdAt = get("createdAt")?.toString()
        )
    }

    private fun DocumentSnapshot.toApiGraphicsFooter(): ApiGraphicsFooter {
        return ApiGraphicsFooter(
            id = id,
            name = getString("name").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
            visibleToRole = getString("visibleToRole"),
            createdAt = get("createdAt")?.toString()
        )
    }

    private fun DocumentSnapshot.toApiUlipPolicy(): ApiUlipPolicy {
        return ApiUlipPolicy(
            id = id,
            policyNumber = getString("policyNumber").orEmpty(),
            policyHolderName = getString("policyHolderName").orEmpty(),
            nav = getDouble("nav") ?: 0.0,
            units = getDouble("units") ?: 0.0,
            fundValue = getDouble("fundValue") ?: 0.0,
            asOfDate = getLong("asOfDate"),
            agentCode = getString("agentCode").orEmpty(),
            adminId = getString("adminId").orEmpty()
        )
    }

    private fun DocumentSnapshot.toApiCircular(): ApiCircular {
        return ApiCircular(
            id = id,
            title = getString("title").orEmpty(),
            category = getString("category").orEmpty(),
            fileUrl = getString("fileUrl").orEmpty(),
            fileName = getString("fileName").orEmpty(),
            createdAt = get("createdAt")?.toString()
        )
    }

    private fun DocumentSnapshot.toApiForm(): ApiForm {
        return ApiForm(
            id = id,
            title = getString("title").orEmpty(),
            category = getString("category").orEmpty(),
            fileUrl = getString("fileUrl").orEmpty(),
            fileName = getString("fileName").orEmpty(),
            createdAt = get("createdAt")?.toString()
        )
    }

    private fun RequestBody.readUtf8(): String {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun MultipartBody.Part.bytes(): ByteArray {
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readByteArray()
    }

    private fun MultipartBody.Part.extensionOrDefault(default: String): String {
        val contentDisposition = headers?.get("Content-Disposition").orEmpty()
        val filename = contentDisposition.substringAfter("filename=\"", "").substringBefore("\"", "")
        val extFromName = filename.substringAfterLast('.', "")
        if (extFromName.isNotBlank()) return extFromName

        val mime = body.contentType()?.toString().orEmpty()
        return when {
            mime.contains("png") -> "png"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("pdf") -> "pdf"
            else -> default
        }
    }

    private fun ApiUser.adminRootId(): String = if (role == "admin") id else adminId.orEmpty()

    private fun canonicalizeRole(rawRole: String?, agencyCode: String?, doCode: String?): String {
        return when (rawRole?.trim()?.lowercase(Locale.ROOT)) {
            "advisor", "agent", "financial advisor", "financial_advisor", "fa" -> "advisor"
            "admin", "do", "development officer", "development_officer", "development officer (do)" -> "admin"
            "superadmin", "super admin", "super_admin" -> "superadmin"
            null, "" -> when {
                !doCode.isNullOrBlank() -> "admin"
                !agencyCode.isNullOrBlank() -> "advisor"
                else -> ""
            }
            else -> when {
                !doCode.isNullOrBlank() && rawRole.equals("officer", ignoreCase = true) -> "admin"
                !agencyCode.isNullOrBlank() && rawRole.equals("user", ignoreCase = true) -> "advisor"
                else -> rawRole.trim().lowercase(Locale.ROOT)
            }
        }
    }

    private fun <T> errorResponse(code: Int, message: String): Response<T> {
        return Response.error(
            code,
            """{"message":"${message.escapeJson()}"}""".toResponseBody("application/json".toMediaType())
        )
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private data class AssetUpload(
        val url: String,
        val path: String,
        val fileName: String
    )
}
