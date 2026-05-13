package com.viplove.licadvisornative.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.UpdateProfileRequest
import com.viplove.licadvisornative.network.toClientDataSheet
import com.viplove.licadvisornative.network.toPolicy
import com.viplove.licadvisornative.network.toUser
import com.viplove.licadvisornative.util.ErrorMapper
import com.viplove.licadvisornative.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AgentViewModel : BasePolicyViewModel() {

    data class AgentUiState(
        val isLoading: Boolean = true,
        override val allPolicies: List<Policy> = emptyList(),
        override val filteredPolicies: List<Policy> = emptyList(),
        val error: String? = null,
        override val searchQuery: String = "",
        val draftsSearchQuery: String = "",
        override val selectedPlan: String = "All",
        override val selectedMode: String = "All",
        override val startDate: Long? = null,
        override val endDate: Long? = null,
        override val selectedFinancialYear: String = "None",
        override val selectedAppraisalYear: String = "None",
        val availableFinancialYears: List<String> = listOf("None"),
        val availableAppraisalYears: List<String> = listOf("None"),
        val availablePlans: List<String> = listOf("All"),
        val availableModes: List<String> = listOf("All"),
        val drafts: List<ClientDataSheet> = emptyList(),
        val submittedForms: List<ClientDataSheet> = emptyList(),
        val archivedForms: List<ClientDataSheet> = emptyList(),
        override val showOnlyLate: Boolean = false,
        override val currentUser: User? = null
    ) : PolicyFilterableUiState {
        val filteredDrafts: List<ClientDataSheet>
            get() = if (draftsSearchQuery.isBlank()) drafts
            else drafts.filter { it.proposerDetails.name.contains(draftsSearchQuery, ignoreCase = true) }

        val filteredSubmitted: List<ClientDataSheet>
            get() = if (draftsSearchQuery.isBlank()) submittedForms
            else submittedForms.filter { it.proposerDetails.name.contains(draftsSearchQuery, ignoreCase = true) }

        val filteredArchived: List<ClientDataSheet>
            get() = if (draftsSearchQuery.isBlank()) archivedForms
            else archivedForms.filter { it.proposerDetails.name.contains(draftsSearchQuery, ignoreCase = true) }

        val ulipPolicies: List<Policy>
            get() {
                val ulipPlanNumbers = listOf("735", "749", "752", "867", "873")
                return allPolicies.filter { it.plan in ulipPlanNumbers }
            }
    }

    override val _uiState = MutableStateFlow<PolicyFilterableUiState>(AgentUiState())
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api

    init {
        fetchAgentDetails()
        fetchDraftsAndSubmissions()
    }

    fun onDraftsSearchQueryChanged(query: String) {
        _uiState.update { (it as AgentUiState).copy(draftsSearchQuery = query) }
    }

    fun onLateFilterToggled(showLate: Boolean) {
        _uiState.update { (it as AgentUiState).copy(showOnlyLate = showLate) }
        applyFilters()
    }

    fun updateProfileDetails(name: String, phone: String) {
        viewModelScope.launch {
            try {
                val response = api.updateProfile(UpdateProfileRequest(name = name, phone = phone))
                if (response.isSuccessful) {
                    val updatedUser = response.body()?.toUser()
                    if (updatedUser != null) {
                        _uiState.update { (it as AgentUiState).copy(currentUser = updatedUser) }
                    }
                } else {
                    _uiState.update { (it as AgentUiState).copy(error = "Failed to update profile.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    fun uploadProfilePicture(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val file = copyUriToTempFile(context, uri) ?: run {
                    _uiState.update { (it as AgentUiState).copy(error = "Failed to process image.") }
                    return@launch
                }

                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val response = api.updateProfilePicture(part)
                if (response.isSuccessful) {
                    fetchAgentDetails()
                } else {
                    _uiState.update { (it as AgentUiState).copy(error = "Failed to upload profile picture.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    private suspend fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp_profile_${UUID.randomUUID()}.jpg")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                tempFile
            } catch (e: Exception) {
                null
            }
        }
    }

    fun refreshData() {
        fetchMyPolicies()
        fetchDraftsAndSubmissions()
    }

    override fun applyFilters() {
        val currentState = _uiState.value as AgentUiState
        val filtered = getFilteredPolicies(currentState)
        _uiState.update { (it as AgentUiState).copy(filteredPolicies = filtered) }
    }

    fun downloadFormAsSeparatePdfs(form: ClientDataSheet, context: Context) {
        viewModelScope.launch { PdfGenerator.generatePdfs(context, form, combined = false) }
    }

    fun downloadFormAsCombinedPdf(form: ClientDataSheet, context: Context) {
        viewModelScope.launch { PdfGenerator.generatePdfs(context, form, combined = true) }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteDatasheet(draftId)
                if (response.isSuccessful) {
                    fetchDraftsAndSubmissions()
                } else {
                    _uiState.update { (it as AgentUiState).copy(error = "Failed to delete draft.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    fun archiveForm(formId: String) {
        viewModelScope.launch {
            try {
                val response = api.updateDatasheet(formId, mapOf("is_archived" to true))
                if (response.isSuccessful) fetchDraftsAndSubmissions()
                else _uiState.update { (it as AgentUiState).copy(error = "Failed to archive form.") }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    fun restoreForm(formId: String) {
        viewModelScope.launch {
            try {
                val response = api.updateDatasheet(formId, mapOf("is_archived" to false))
                if (response.isSuccessful) fetchDraftsAndSubmissions()
                else _uiState.update { (it as AgentUiState).copy(error = "Failed to restore form.") }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            TokenManager.clearAll()
        }
    }

    private fun fetchAgentDetails() {
        viewModelScope.launch {
            try {
                val response = api.me()
                if (response.isSuccessful) {
                    val user = response.body()?.toUser()
                    val financialYears = generateFinancialYearOptions()
                    val appraisalYears = generateAppraisalYearOptions(user?.startDate)
                    _uiState.update {
                        (it as AgentUiState).copy(
                            currentUser = user,
                            availableFinancialYears = listOf("None") + financialYears,
                            availableAppraisalYears = listOf("None") + appraisalYears
                        )
                    }
                    if ((_uiState.value as AgentUiState).allPolicies.isEmpty()) {
                        fetchMyPolicies()
                    }
                } else {
                    _uiState.update { (it as AgentUiState).copy(error = "Failed to load user details.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun fetchMyPolicies() {
        viewModelScope.launch {
            _uiState.update { (it as AgentUiState).copy(isLoading = true) }
            val agent = (_uiState.value as AgentUiState).currentUser
            val agentCode = agent?.agencyCode
            if (agentCode.isNullOrEmpty()) {
                _uiState.update { (it as AgentUiState).copy(isLoading = false, allPolicies = emptyList(), filteredPolicies = emptyList()) }
                return@launch
            }
            try {
                val response = api.getPolicies(agentCode = agentCode)
                if (response.isSuccessful) {
                    val policyList = response.body()?.map { it.toPolicy() } ?: emptyList()
                    val plans = listOf("All") + policyList.map { it.plan }.distinct().sorted()
                    val modes = mutableListOf("All")
                    modes.addAll(policyList.map { it.mode }.distinct().sorted())
                    if (policyList.any { it.isAnanda }) modes.add("ANANDA")
                    _uiState.update {
                        (it as AgentUiState).copy(
                            isLoading = false, allPolicies = policyList,
                            availablePlans = plans, availableModes = modes.distinct(), error = null
                        )
                    }
                    applyFilters()
                } else {
                    _uiState.update { (it as AgentUiState).copy(isLoading = false, error = "Failed to load policies.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(isLoading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun fetchDraftsAndSubmissions() {
        viewModelScope.launch {
            try {
                val response = api.getDatasheets(isArchived = false)
                if (response.isSuccessful) {
                    val allSheets = response.body()?.map { it.toClientDataSheet() } ?: emptyList()
                    fetchArchivedForms()
                    val drafts = allSheets.filter { it.isDraft }
                    val submitted = allSheets.filter { !it.isDraft }
                    _uiState.update {
                        (it as AgentUiState).copy(drafts = drafts, submittedForms = submitted, error = null, isLoading = false)
                    }
                } else {
                    _uiState.update { (it as AgentUiState).copy(isLoading = false, error = "Failed to load forms.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(isLoading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun fetchArchivedForms() {
        viewModelScope.launch {
            try {
                val response = api.getDatasheets(isArchived = true)
                if (response.isSuccessful) {
                    val archivedForms = response.body()?.map { it.toClientDataSheet() } ?: emptyList()
                    _uiState.update { (it as AgentUiState).copy(archivedForms = archivedForms, error = null) }
                } else {
                    _uiState.update { (it as AgentUiState).copy(error = "Failed to load archived forms.") }
                }
            } catch (e: Exception) {
                _uiState.update { (it as AgentUiState).copy(error = ErrorMapper.map(e)) }
            }
        }
    }
}
