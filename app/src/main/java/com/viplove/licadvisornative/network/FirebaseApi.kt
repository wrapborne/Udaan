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
import kotlinx.coroutines.tasks.await
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
        val normalized = request.code.trim()
        val docs = firestore.collection("users").get().await().documents
        val match = docs.firstOrNull {
            it.getString("agencyCode").equals(normalized, ignoreCase = true) ||
                it.getString("doCode").equals(normalized, ignoreCase = true)
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
                !agentCode.isNullOrBlank() -> policy.agentCode.equals(agentCode, ignoreCase = true)
                current.role == "superadmin" -> true
                current.role == "admin" -> policy.adminId == current.id
                else -> policy.agentCode.equals(current.agencyCode, ignoreCase = true)
            }
        }
        return Response.success(filtered.sortedByDescending { it.doc ?: 0L })
    }

    suspend fun batchStorePolicies(request: BatchPoliciesRequest): Response<BatchResult> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val duplicates = mutableListOf<String>()
        request.policies.chunked(50).forEach { chunk ->
            chunk.forEach { payload ->
                val existing = findDocumentByField("policies", "policyNumber", payload.policyNumber)
                if (existing != null && !request.overwrite) {
                    duplicates += payload.policyNumber
                } else {
                    val target = existing ?: firestore.collection("policies").document()
                    target.set(
                        mapOf(
                            "policyNumber" to payload.policyNumber,
                            "plan" to payload.plan.orEmpty(),
                            "mode" to payload.mode.orEmpty(),
                            "doc" to payload.doc,
                            "premium" to payload.premium,
                            "agentCode" to payload.agentCode,
                            "adminId" to current.adminRootId(),
                            "shortName" to payload.shortName.orEmpty(),
                            "enachDate" to payload.enachDate.orEmpty(),
                            "agentName" to payload.agentName.orEmpty(),
                            "isAnanda" to payload.isAnanda,
                            "isUlip" to false,
                            "createdAt" to System.currentTimeMillis()
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
            findDocumentByField("policies", "policyNumber", policyNumber) != null
        }
        return Response.success(DuplicatesResponse(duplicates = duplicates))
    }

    suspend fun batchUpdatePaymentDates(request: BatchUpdatePaymentDatesRequest): Response<BatchUpdateResult> {
        var updated = 0
        request.updates.forEach { change ->
            val doc = findDocumentByField("policies", "policyNumber", change.policyNumber)
            if (doc != null) {
                doc.set(mapOf("lastPremiumPaidDate" to change.lastPremiumPaidDate), SetOptions.merge()).await()
                updated++
            }
        }
        return Response.success(BatchUpdateResult(updated = updated))
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
        val docs = firestore.collection("premiumSummaries").get().await().documents
        val filtered = docs.map { it.toApiPremiumSummary() }.filter { summary ->
            val ownerPass = when (current.role) {
                "superadmin" -> true
                "admin" -> summary.adminId == current.id
                else -> summary.agencyCode.equals(current.agencyCode, ignoreCase = true)
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
            val existing = firestore.collection("premiumSummaries").get().await().documents.firstOrNull {
                it.getString("reportMonth") == payload.reportMonth &&
                    it.getString("agencyCode").equals(payload.agencyCode, ignoreCase = true)
            }
            val key = "${payload.reportMonth}-${payload.agencyCode}"
            if (existing != null && !request.overwrite) {
                duplicates += key
            } else {
                val ref = existing?.reference ?: firestore.collection("premiumSummaries").document()
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
        val existingCodes = firestore.collection("premiumSummaries").get().await().documents
            .filter { it.getString("reportMonth") == request.reportMonth }
            .mapNotNull { it.getString("agencyCode") }
            .distinct()
        return Response.success(SummaryDuplicatesResponse(existingAgencyCodes = existingCodes))
    }

    suspend fun getGraphicsTemplates(): Response<List<ApiGraphicsTemplate>> {
        val templates = firestore.collection("graphicsTemplates").get().await().documents
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
        val ref = firestore.collection("graphicsTemplates").document()
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
        deleteAssetDocument("graphicsTemplates", id)
        return Response.success(MessageResponse("Template deleted successfully."))
    }

    suspend fun getGraphicsFooters(): Response<List<ApiGraphicsFooter>> {
        val footers = firestore.collection("graphicsFooters").get().await().documents
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
        val ref = firestore.collection("graphicsFooters").document()
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
        deleteAssetDocument("graphicsFooters", id)
        return Response.success(MessageResponse("Footer deleted successfully."))
    }

    suspend fun getUlipPolicies(): Response<List<ApiUlipPolicy>> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val policies = firestore.collection("ulipPolicies").get().await().documents
            .map { it.toApiUlipPolicy() }
            .filter {
                when (current.role) {
                    "superadmin" -> true
                    "admin" -> it.adminId == current.id
                    else -> it.agentCode.equals(current.agencyCode, ignoreCase = true)
                }
            }
        return Response.success(policies)
    }

    suspend fun createUlipPolicy(body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiUlipPolicy> {
        val current = currentUserProfile() ?: return errorResponse(401, "Not authenticated.")
        val ref = firestore.collection("ulipPolicies").document()
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
        val doc = firestore.collection("config").document(key).get().await()
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
        return ApiUser(
            id = id,
            email = getString("email").orEmpty(),
            name = getString("name").orEmpty(),
            phone = getString("phone").orEmpty(),
            role = getString("role").orEmpty(),
            isApproved = getBoolean("isApproved") ?: false,
            agencyCode = getString("agencyCode"),
            doCode = getString("doCode"),
            adminId = getString("adminId"),
            profilePicturePath = getString("profilePictureUrl"),
            startDate = get("startDate")?.toString()
        )
    }

    private fun DocumentSnapshot.toApiPolicy(): ApiPolicy {
        return ApiPolicy(
            id = id,
            policyNumber = getString("policyNumber").orEmpty(),
            plan = getString("plan"),
            mode = getString("mode"),
            doc = getLong("doc"),
            premium = getDouble("premium") ?: 0.0,
            agentCode = getString("agentCode").orEmpty(),
            adminId = getString("adminId").orEmpty(),
            shortName = getString("shortName"),
            enachDate = getString("enachDate"),
            agentName = getString("agentName"),
            isAnanda = getBoolean("isAnanda") ?: false,
            lastPremiumPaidDate = getLong("lastPremiumPaidDate")
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
