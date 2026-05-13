package com.viplove.licadvisornative.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface LicApi {

    // ── Auth ──

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/lookup-by-code")
    suspend fun lookupEmailByCode(@Body request: LookupByCodeRequest): Response<LookupEmailResponse>

    @GET("auth/me")
    suspend fun me(): Response<ApiUser>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<AuthResponse>

    // ── Users ──

    @GET("users/agents")
    suspend fun getMyAgents(): Response<List<ApiUser>>

    @GET("users/admins")
    suspend fun getAllAdmins(): Response<List<ApiUser>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<ApiUser>

    @PUT("users/{id}/approve")
    suspend fun approveUser(@Path("id") id: String): Response<MessageResponse>

    @PUT("users/{id}/start-date")
    suspend fun updateStartDate(
        @Path("id") id: String,
        @Body request: UpdateStartDateRequest
    ): Response<ApiUser>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<MessageResponse>

    @PUT("users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiUser>

    @Multipart
    @POST("users/profile-picture")
    suspend fun updateProfilePicture(
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>

    // ── Policies ──

    @GET("policies")
    suspend fun getPolicies(
        @Query("agent_code") agentCode: String? = null
    ): Response<List<ApiPolicy>>

    @POST("policies/batch")
    suspend fun batchStorePolicies(@Body request: BatchPoliciesRequest): Response<BatchResult>

    @POST("policies/check-duplicates")
    suspend fun checkPolicyDuplicates(@Body request: CheckDuplicatesRequest): Response<DuplicatesResponse>

    @POST("policies/batch-update-payment-dates")
    suspend fun batchUpdatePaymentDates(@Body request: BatchUpdatePaymentDatesRequest): Response<BatchUpdateResult>

    // ── Datasheets ──

    @GET("datasheets")
    suspend fun getDatasheets(
        @Query("is_draft") isDraft: Boolean? = null,
        @Query("is_archived") isArchived: Boolean? = null
    ): Response<List<ApiDatasheet>>

    @GET("datasheets/{id}")
    suspend fun getDatasheet(@Path("id") id: String): Response<ApiDatasheet>

    @POST("datasheets")
    suspend fun createDatasheet(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiDatasheet>

    @PUT("datasheets/{id}")
    suspend fun updateDatasheet(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiDatasheet>

    @DELETE("datasheets/{id}")
    suspend fun deleteDatasheet(@Path("id") id: String): Response<MessageResponse>

    // ── Premium Summaries ──

    @GET("premium-summaries")
    suspend fun getPremiumSummaries(
        @Query("report_month") reportMonth: String? = null
    ): Response<List<ApiPremiumSummary>>

    @POST("premium-summaries/batch")
    suspend fun batchStoreSummaries(@Body request: BatchSummariesRequest): Response<BatchResult>

    @POST("premium-summaries/check-duplicates")
    suspend fun checkSummaryDuplicates(@Body request: CheckSummaryDuplicatesRequest): Response<SummaryDuplicatesResponse>

    // ── Graphics ──

    @GET("graphics-templates")
    suspend fun getGraphicsTemplates(): Response<List<ApiGraphicsTemplate>>

    @Multipart
    @POST("graphics-templates")
    suspend fun uploadGraphicsTemplate(
        @Part("name") name: RequestBody,
        @Part image: MultipartBody.Part,
        @Part("visible_to_role") visibleToRole: RequestBody
    ): Response<ApiGraphicsTemplate>

    @DELETE("graphics-templates/{id}")
    suspend fun deleteGraphicsTemplate(@Path("id") id: String): Response<MessageResponse>

    @GET("graphics-footers")
    suspend fun getGraphicsFooters(): Response<List<ApiGraphicsFooter>>

    @Multipart
    @POST("graphics-footers")
    suspend fun uploadGraphicsFooter(
        @Part("name") name: RequestBody,
        @Part image: MultipartBody.Part,
        @Part("visible_to_role") visibleToRole: RequestBody
    ): Response<ApiGraphicsFooter>

    @DELETE("graphics-footers/{id}")
    suspend fun deleteGraphicsFooter(@Path("id") id: String): Response<MessageResponse>

    // ── ULIP Policies ──

    @GET("ulip-policies")
    suspend fun getUlipPolicies(): Response<List<ApiUlipPolicy>>

    @POST("ulip-policies")
    suspend fun createUlipPolicy(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiUlipPolicy>

    // ── Circulars ──

    @GET("circulars")
    suspend fun getCirculars(): Response<List<ApiCircular>>

    @Multipart
    @POST("circulars")
    suspend fun uploadCircular(
        @Part("title") title: RequestBody,
        @Part("category") category: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiCircular>

    @DELETE("circulars/{id}")
    suspend fun deleteCircular(@Path("id") id: String): Response<MessageResponse>

    // ── Forms ──

    @GET("forms")
    suspend fun getForms(): Response<List<ApiForm>>

    @Multipart
    @POST("forms")
    suspend fun uploadForm(
        @Part("title") title: RequestBody,
        @Part("category") category: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiForm>

    @DELETE("forms/{id}")
    suspend fun deleteForm(@Path("id") id: String): Response<MessageResponse>

    // ── File Upload ──

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("folder") folder: RequestBody? = null
    ): Response<UploadResponse>

    // ── Config ──

    @GET("config/{key}")
    suspend fun getConfig(@Path("key") key: String): Response<ApiConfig>
}
