package com.viplove.licadvisornative.network

import com.google.gson.annotations.SerializedName

// ── Auth Requests ──

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val role: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("admin_do_code") val adminDoCode: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class LookupByCodeRequest(
    val code: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null
)

data class UpdateStartDateRequest(
    @SerializedName("start_date") val startDate: Long
)

// ── Auth Responses ──

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    val user: ApiUser
)

data class LookupEmailResponse(
    val email: String
)

data class ApiUser(
    val id: String,
    val email: String,
    val name: String,
    val phone: String,
    val role: String,
    @SerializedName("is_approved") val isApproved: Boolean,
    @SerializedName("agency_code") val agencyCode: String?,
    @SerializedName("do_code") val doCode: String?,
    @SerializedName("admin_id") val adminId: String?,
    @SerializedName("profile_picture_path") val profilePicturePath: String?,
    @SerializedName("start_date") val startDate: String?
)

// ── Policy DTOs ──

data class ApiPolicy(
    val id: String,
    @SerializedName("policy_number") val policyNumber: String,
    val plan: String?,
    val mode: String?,
    val doc: Long?,
    val premium: Double,
    @SerializedName("agent_code") val agentCode: String,
    @SerializedName("admin_id") val adminId: String,
    @SerializedName("short_name") val shortName: String?,
    @SerializedName("enach_date") val enachDate: String?,
    @SerializedName("agent_name") val agentName: String?,
    @SerializedName("is_ananda") val isAnanda: Boolean,
    @SerializedName("last_premium_paid_date") val lastPremiumPaidDate: Long?
)

data class BatchPoliciesRequest(
    val policies: List<PolicyPayload>,
    val overwrite: Boolean = true
)

data class PolicyPayload(
    @SerializedName("policy_number") val policyNumber: String,
    val plan: String? = null,
    val mode: String? = null,
    val doc: Long? = null,
    val premium: Double = 0.0,
    @SerializedName("agent_code") val agentCode: String,
    @SerializedName("short_name") val shortName: String? = null,
    @SerializedName("enach_date") val enachDate: String? = null,
    @SerializedName("agent_name") val agentName: String? = null,
    @SerializedName("is_ananda") val isAnanda: Boolean = false
)

data class BatchResult(
    val inserted: Int,
    val duplicates: List<String>
)

data class CheckDuplicatesRequest(
    @SerializedName("policy_numbers") val policyNumbers: List<String>
)

data class DuplicatesResponse(
    val duplicates: List<String>
)

data class BatchUpdatePaymentDatesRequest(
    val updates: List<PaymentDateUpdate>
)

data class PaymentDateUpdate(
    @SerializedName("policy_number") val policyNumber: String,
    @SerializedName("last_premium_paid_date") val lastPremiumPaidDate: Long
)

data class BatchUpdateResult(
    val updated: Int
)

// ── Premium Summary DTOs ──

data class ApiPremiumSummary(
    val id: String,
    @SerializedName("report_month") val reportMonth: String,
    @SerializedName("fp_sch_prem") val fpSchPrem: Double,
    @SerializedName("fy_sch_prem") val fySchPrem: Double,
    @SerializedName("admin_id") val adminId: String,
    @SerializedName("agency_code") val agencyCode: String
)

data class BatchSummariesRequest(
    val summaries: List<SummaryPayload>,
    val overwrite: Boolean = true
)

data class SummaryPayload(
    @SerializedName("report_month") val reportMonth: String,
    @SerializedName("fp_sch_prem") val fpSchPrem: Double,
    @SerializedName("fy_sch_prem") val fySchPrem: Double,
    @SerializedName("agency_code") val agencyCode: String
)

data class CheckSummaryDuplicatesRequest(
    @SerializedName("report_month") val reportMonth: String
)

data class SummaryDuplicatesResponse(
    @SerializedName("existing_agency_codes") val existingAgencyCodes: List<String>
)

// ── Datasheet DTOs ──

data class ApiDatasheet(
    val id: String,
    @SerializedName("created_by_advisor_id") val createdByAdvisorId: String,
    @SerializedName("admin_id") val adminId: String,
    @SerializedName("is_draft") val isDraft: Boolean,
    @SerializedName("is_archived") val isArchived: Boolean,
    @SerializedName("initial_questions") val initialQuestions: Map<String, Any>?,
    @SerializedName("plan_details") val planDetails: Map<String, Any>?,
    @SerializedName("sum_assured") val sumAssured: String?,
    val mode: String?,
    @SerializedName("date_of_commencement") val dateOfCommencement: Long?,
    @SerializedName("nach_date") val nachDate: Long?,
    @SerializedName("is_nach_mandatory") val isNachMandatory: String?,
    @SerializedName("pwb_required") val pwbRequired: Boolean,
    @SerializedName("proposer_details") val proposerDetails: Map<String, Any>?,
    @SerializedName("life_assured_details") val lifeAssuredDetails: Map<String, Any>?,
    @SerializedName("previous_policies") val previousPolicies: List<Map<String, Any>>?,
    @SerializedName("updated_at") val updatedAt: String?
)

// ── Graphics DTOs ──

data class ApiGraphicsTemplate(
    val id: String,
    val name: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("visible_to_role") val visibleToRole: String,
    @SerializedName("created_at") val createdAt: String?
)

data class ApiGraphicsFooter(
    val id: String,
    val name: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("visible_to_role") val visibleToRole: String?,
    @SerializedName("created_at") val createdAt: String?
)

// ── ULIP Policy DTOs ──

data class ApiUlipPolicy(
    val id: String,
    @SerializedName("policy_number") val policyNumber: String,
    @SerializedName("policy_holder_name") val policyHolderName: String,
    val nav: Double,
    val units: Double,
    @SerializedName("fund_value") val fundValue: Double,
    @SerializedName("as_of_date") val asOfDate: Long?,
    @SerializedName("agent_code") val agentCode: String,
    @SerializedName("admin_id") val adminId: String
)

// ── Circular DTOs ──

data class ApiCircular(
    val id: String,
    val title: String,
    val category: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("created_at") val createdAt: String?
)

// ── Form DTOs ──

data class ApiForm(
    val id: String,
    val title: String,
    val category: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("created_at") val createdAt: String?
)

// ── Config ──

data class ApiConfig(
    val key: String,
    val value: Any
)

// ── Upload ──

data class UploadResponse(
    val url: String,
    val path: String
)

// ── Generic ──

data class MessageResponse(
    val message: String
)
