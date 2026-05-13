package com.viplove.licadvisornative.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.PremiumSummary
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.UpdateStartDateRequest
import com.viplove.licadvisornative.network.toClientDataSheet
import com.viplove.licadvisornative.network.toPolicy
import com.viplove.licadvisornative.network.toUser
import com.viplove.licadvisornative.util.FileUploadManager
import com.viplove.licadvisornative.util.PdfGenerator
import com.viplove.licadvisornative.util.PolicyFilter
import com.viplove.licadvisornative.util.UploadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AdminViewModel"

class AdminViewModel : ViewModel() {

    sealed class LeadsUiState {
        object Loading : LeadsUiState()
        data class Success(val leads: List<ClientDataSheet>) : LeadsUiState()
        data class Error(val message: String) : LeadsUiState()
    }

    sealed class AnalyticsUiState {
        object Loading : AnalyticsUiState()
        data class Success(val planCounts: List<PlanCount>, val agentPerformance: List<AgentPerformance>) : AnalyticsUiState()
    }

    sealed class AgentListUiState {
        object Loading : AgentListUiState()
        data class Success(val agents: List<User>) : AgentListUiState()
        data class Error(val message: String) : AgentListUiState()
    }

    data class PlanCount(val planName: String, val count: Int)
    data class AgentPerformance(val agentEmail: String, val policyCount: Int)

    data class MonthlyPremium(val month: String, val premium: Double)

    data class AgentSummary(
        val agencyCode: String,
        val agentName: String = "",
        val totalScheduledPremium: Double,
        val monthlyBreakdown: List<MonthlyPremium> = emptyList()
    )

    enum class SortOrder { ASC, DESC }
    enum class ProposalSortOption { CODE, POLICY_COUNT }
    enum class SummarySortOption { CODE, PREMIUM }

    data class ProposalsUiState(
        val isLoading: Boolean = true,
        override val allPolicies: List<Policy> = emptyList(),
        override val filteredPolicies: List<Policy> = emptyList(),
        val error: String? = null,
        override val searchQuery: String = "",
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
        val pendingUpload: List<Policy>? = null,
        val duplicatePolicies: List<String>? = null,
        val proposalSortOption: ProposalSortOption = ProposalSortOption.CODE,
        val proposalSortOrder: SortOrder = SortOrder.ASC,
        override val showOnlyLate: Boolean = false,
        override val currentUser: User? = null
    ) : BasePolicyViewModel.PolicyFilterableUiState

    data class PremiumSummaryUiState(
        val isLoading: Boolean = true,
        val summariesByMonth: Map<String, List<AgentSummary>> = emptyMap(),
        val availableMonths: List<String> = listOf("All"),
        val selectedMonth: String = "All",
        val monthlyTotalPremium: Double = 0.0,
        val appraisalYearTotalPremium: Double = 0.0,
        val error: String? = null,
        val pendingSummaryUpload: List<PremiumSummary>? = null,
        val duplicateSummaries: List<String>? = null,
        val summarySortOption: SummarySortOption = SummarySortOption.CODE,
        val summarySortOrder: SortOrder = SortOrder.ASC
    )

    private val _leadsUiState = MutableStateFlow<LeadsUiState>(LeadsUiState.Loading)
    val leadsUiState = _leadsUiState.asStateFlow()

    private val _analyticsUiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val analyticsUiState = _analyticsUiState.asStateFlow()

    private val _agentListState = MutableStateFlow<AgentListUiState>(AgentListUiState.Loading)
    val agentListState = _agentListState.asStateFlow()

    private val _premiumSummaryUiState = MutableStateFlow(PremiumSummaryUiState())
    val premiumSummaryUiState = _premiumSummaryUiState.asStateFlow()

    private val _leadSearchQuery = MutableStateFlow("")
    val leadSearchQuery = _leadSearchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<BasePolicyViewModel.PolicyFilterableUiState>(ProposalsUiState())
    val proposalsUiState = _uiState.asStateFlow()

    private val _currentAdminState = MutableStateFlow<User?>(null)
    val currentAdminState = _currentAdminState.asStateFlow()

    private val api = ApiClient.api

    init {
        fetchCurrentAdminDetails {
            fetchMyAgents { agentList ->
                fetchMyPolicies(agentList)
            }
            fetchAgentLeads()
        }
    }

    fun refreshData() {
        fetchCurrentAdminDetails {
            fetchMyAgents { agentList ->
                fetchMyPolicies(agentList)
            }
            fetchAgentLeads()
        }
    }

    private fun applyFilters() {
        val currentState = _uiState.value as ProposalsUiState
        val filtered = PolicyFilter.getFilteredPolicies(currentState.allPolicies, currentState)
        _uiState.update { (it as ProposalsUiState).copy(filteredPolicies = filtered) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { (it as ProposalsUiState).copy(searchQuery = query) }
        applyFilters()
    }

    fun onPlanSelected(plan: String) {
        _uiState.update { (it as ProposalsUiState).copy(selectedPlan = plan) }
        applyFilters()
    }

    fun onModeSelected(mode: String) {
        _uiState.update { (it as ProposalsUiState).copy(selectedMode = mode) }
        applyFilters()
    }

    fun onDateRangeSelected(start: Long?, end: Long?) {
        _uiState.update { (it as ProposalsUiState).copy(startDate = start, endDate = end, selectedFinancialYear = "None", selectedAppraisalYear = "None") }
        applyFilters()
        fetchPremiumSummaries()
    }

    fun onFinancialYearSelected(year: String) {
        val bounds = PolicyFilter.getFinancialYearBounds(year)
        _uiState.update { (it as ProposalsUiState).copy(selectedFinancialYear = year, startDate = bounds?.first, endDate = bounds?.second, selectedAppraisalYear = "None") }
        applyFilters()
        fetchPremiumSummaries()
    }

    fun onAppraisalYearSelected(year: String) {
        val adminStartDate = (_uiState.value as ProposalsUiState).currentUser?.startDate
        if (adminStartDate == null) return
        val bounds = PolicyFilter.getAppraisalYearBounds(year, adminStartDate)
        _uiState.update { (it as ProposalsUiState).copy(selectedAppraisalYear = year, startDate = bounds?.first, endDate = bounds?.second, selectedFinancialYear = "None") }
        applyFilters()
        fetchPremiumSummaries()
    }

    fun onLeadSearchQueryChanged(query: String) {
        _leadSearchQuery.value = query
    }

    fun onLateFilterToggled(showLate: Boolean) {
        _uiState.update { (it as ProposalsUiState).copy(showOnlyLate = showLate) }
        applyFilters()
    }

    fun downloadLeadAsSeparatePdfs(lead: ClientDataSheet, context: android.content.Context) {
        viewModelScope.launch { PdfGenerator.generatePdfs(context, lead, combined = false) }
    }

    fun downloadLeadAsCombinedPdf(lead: ClientDataSheet, context: android.content.Context) {
        viewModelScope.launch { PdfGenerator.generatePdfs(context, lead, combined = true) }
    }

    fun archiveForm(formId: String) {
        viewModelScope.launch {
            try { api.updateDatasheet(formId, mapOf("is_archived" to true)); fetchAgentLeads() } catch (_: Exception) {}
        }
    }

    fun unarchiveForm(formId: String) {
        viewModelScope.launch {
            try { api.updateDatasheet(formId, mapOf("is_archived" to false)); fetchAgentLeads() } catch (_: Exception) {}
        }
    }

    fun approveAgent(agentUid: String) {
        viewModelScope.launch {
            try { api.approveUser(agentUid); fetchMyAgents {} } catch (_: Exception) {}
        }
    }

    fun deleteAgent(agentUid: String) {
        viewModelScope.launch {
            try { api.deleteUser(agentUid); fetchMyAgents {} } catch (_: Exception) {}
        }
    }

    fun updateAgentStartDate(agentUid: String, newStartDate: Long) {
        viewModelScope.launch {
            try { api.updateStartDate(agentUid, UpdateStartDateRequest(newStartDate)); fetchMyAgents {} } catch (_: Exception) {}
        }
    }

    fun updateAdminStartDate(adminUid: String, newStartDate: Long) {
        viewModelScope.launch {
            try { api.updateStartDate(adminUid, UpdateStartDateRequest(newStartDate)); refreshData() } catch (_: Exception) {}
        }
    }

    fun onProposalSortChanged(option: ProposalSortOption) {
        val currentState = _uiState.value as ProposalsUiState
        val newOrder = if (currentState.proposalSortOption == option) {
            if (currentState.proposalSortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
        } else SortOrder.ASC
        _uiState.update { (it as ProposalsUiState).copy(proposalSortOption = option, proposalSortOrder = newOrder) }
    }

    fun onSummarySortChanged(option: SummarySortOption) {
        val currentOrder = _premiumSummaryUiState.value.summarySortOrder
        val newOrder = if (_premiumSummaryUiState.value.summarySortOption == option) {
            if (currentOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
        } else SortOrder.ASC
        _premiumSummaryUiState.update { it.copy(summarySortOption = option, summarySortOrder = newOrder) }
    }

    fun onMonthSelected(month: String) {
        _premiumSummaryUiState.update { it.copy(selectedMonth = month) }
        calculatePremiumTotals()
    }

    fun processAndUploadFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val currentAdminId = TokenManager.getUserId() ?: return@launch
            _uiState.update { (it as ProposalsUiState).copy(isLoading = true, error = null) }
            when (val result = FileUploadManager.processProposals(uri, contentResolver, currentAdminId)) {
                is UploadResult.Success -> fetchMyPolicies((_agentListState.value as? AgentListUiState.Success)?.agents ?: emptyList())
                is UploadResult.RequiresConfirmation -> _uiState.update {
                    (it as ProposalsUiState).copy(isLoading = false, pendingUpload = result.pendingItems as? List<Policy>, duplicatePolicies = result.duplicates)
                }
                is UploadResult.Error -> _uiState.update { (it as ProposalsUiState).copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun processPremiumSummary(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val currentAdminId = TokenManager.getUserId() ?: return@launch
            _premiumSummaryUiState.update { it.copy(isLoading = true, error = null) }
            when (val result = FileUploadManager.processPremiumSummary(uri, contentResolver, currentAdminId)) {
                is UploadResult.Success -> {
                    fetchPremiumSummaries()
                    fetchMyPolicies((_agentListState.value as? AgentListUiState.Success)?.agents ?: emptyList())
                }
                is UploadResult.RequiresConfirmation -> _premiumSummaryUiState.update {
                    it.copy(isLoading = false, pendingSummaryUpload = result.pendingItems as? List<PremiumSummary>, duplicateSummaries = result.duplicates)
                }
                is UploadResult.Error -> _premiumSummaryUiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun confirmPolicyUpload(overwrite: Boolean) {
        val currentState = _uiState.value as ProposalsUiState
        viewModelScope.launch {
            try {
                val adminId = TokenManager.getUserId() ?: return@launch
                FileUploadManager.uploadPolicies(currentState.pendingUpload!!, adminId, overwrite, currentState.duplicatePolicies!!)
                fetchMyPolicies((_agentListState.value as? AgentListUiState.Success)?.agents ?: emptyList())
            } finally {
                _uiState.update { (it as ProposalsUiState).copy(pendingUpload = null, duplicatePolicies = null) }
            }
        }
    }

    fun cancelPolicyUpload() {
        _uiState.update { (it as ProposalsUiState).copy(pendingUpload = null, duplicatePolicies = null, isLoading = false) }
    }

    fun confirmSummaryUpload(overwrite: Boolean) {
        val currentState = _premiumSummaryUiState.value
        viewModelScope.launch {
            try {
                val adminId = TokenManager.getUserId() ?: return@launch
                FileUploadManager.uploadConfirmedSummaries(currentState.pendingSummaryUpload!!, adminId, overwrite, currentState.duplicateSummaries!!)
                fetchPremiumSummaries()
                fetchMyPolicies((_agentListState.value as? AgentListUiState.Success)?.agents ?: emptyList())
            } finally {
                _premiumSummaryUiState.update { it.copy(pendingSummaryUpload = null, duplicateSummaries = null) }
            }
        }
    }

    fun cancelSummaryUpload() {
        _premiumSummaryUiState.update { it.copy(pendingSummaryUpload = null, duplicateSummaries = null, isLoading = false) }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            TokenManager.clearAll()
        }
    }

    private fun fetchCurrentAdminDetails(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.me()
                if (response.isSuccessful) {
                    val adminUser = response.body()?.toUser()
                    _currentAdminState.value = adminUser

                    val financialYears = PolicyFilter.generateFinancialYearOptions()
                    val appraisalYears = PolicyFilter.generateAppraisalYearOptions(adminUser?.startDate)
                    val currentAppraisalYear = PolicyFilter.getCurrentAppraisalYearString(adminUser?.startDate)

                    _uiState.update {
                        (it as ProposalsUiState).copy(
                            currentUser = adminUser,
                            availableFinancialYears = listOf("None") + financialYears,
                            availableAppraisalYears = listOf("None") + appraisalYears,
                            selectedAppraisalYear = currentAppraisalYear
                        )
                    }
                    onAppraisalYearSelected(currentAppraisalYear)
                    onComplete()
                }
            } catch (_: Exception) {}
        }
    }

    private fun fetchMyPolicies(agents: List<User>) {
        viewModelScope.launch {
            _uiState.update { (it as ProposalsUiState).copy(isLoading = true) }
            try {
                val response = api.getPolicies()
                if (response.isSuccessful) {
                    val policyList = response.body()?.map { it.toPolicy() } ?: emptyList()
                    calculateAnalytics(policyList, agents)
                    val plans = PolicyFilter.generatePlanOptions(policyList)
                    val modes = PolicyFilter.generateModeOptions(policyList)
                    _uiState.update {
                        (it as ProposalsUiState).copy(isLoading = false, allPolicies = policyList, availablePlans = plans, availableModes = modes)
                    }
                    applyFilters()
                } else {
                    _uiState.update { (it as ProposalsUiState).copy(isLoading = false, error = "Failed to load policies.") }
                }
            } catch (_: Exception) {
                _uiState.update { (it as ProposalsUiState).copy(isLoading = false, error = "Network error loading policies.") }
            }
        }
    }

    private fun fetchMyAgents(onComplete: (List<User>) -> Unit) {
        viewModelScope.launch {
            _agentListState.value = AgentListUiState.Loading
            try {
                val response = api.getMyAgents()
                if (response.isSuccessful) {
                    val agentList = response.body()?.map { it.toUser() } ?: emptyList()
                    _agentListState.value = AgentListUiState.Success(agentList)
                    onComplete(agentList)
                } else {
                    _agentListState.value = AgentListUiState.Error("Failed to load agents.")
                    onComplete(emptyList())
                }
            } catch (_: Exception) {
                _agentListState.value = AgentListUiState.Error("Network error loading agents.")
                onComplete(emptyList())
            }
        }
    }

    private fun fetchAgentLeads() {
        viewModelScope.launch {
            _leadsUiState.value = LeadsUiState.Loading
            try {
                val response = api.getDatasheets(isDraft = false, isArchived = false)
                if (response.isSuccessful) {
                    _leadsUiState.value = LeadsUiState.Success(response.body()?.map { it.toClientDataSheet() } ?: emptyList())
                } else {
                    _leadsUiState.value = LeadsUiState.Error("Failed to load leads.")
                }
            } catch (_: Exception) {
                _leadsUiState.value = LeadsUiState.Error("Network error loading leads.")
            }
        }
    }

    private fun calculateAnalytics(policies: List<Policy>, agents: List<User>) {
        _analyticsUiState.value = AnalyticsUiState.Loading
        val planCounts = policies.groupBy { it.plan }.map { (plan, list) -> PlanCount(plan, list.size) }.sortedByDescending { it.count }
        val agentPerformance = policies.groupBy { it.agentCode }.mapNotNull { (agentCode, list) ->
            agents.find { it.agencyCode == agentCode }?.let { AgentPerformance(it.email, list.size) }
        }.sortedByDescending { it.policyCount }
        _analyticsUiState.value = AnalyticsUiState.Success(planCounts, agentPerformance)
    }

    private fun fetchPremiumSummaries() {
        viewModelScope.launch {
            _premiumSummaryUiState.update { it.copy(isLoading = true) }
            try {
                val summariesResponse = api.getPremiumSummaries()
                val agentsResponse = api.getMyAgents()
                if (!summariesResponse.isSuccessful) {
                    _premiumSummaryUiState.update { it.copy(isLoading = false, error = "Failed to load summaries.") }
                    return@launch
                }

                val allSummaries = summariesResponse.body()?.map {
                    PremiumSummary(summaryId = it.id, reportMonth = it.reportMonth, fpSchPrem = it.fpSchPrem, fySchPrem = it.fySchPrem, adminId = it.adminId, agencyCode = it.agencyCode)
                } ?: emptyList()

                val agents = agentsResponse.body()?.map { it.toUser() } ?: emptyList()
                val agentNameMap = agents.filter { it.name.isNotBlank() }.associate { it.agencyCode.trim().lowercase() to it.name }
                val policyNameMap = (_uiState.value as ProposalsUiState).allPolicies
                    .filter { it.agentName.isNotBlank() }.associate { it.agentCode.trim().lowercase() to it.agentName }

                val proposalState = _uiState.value as ProposalsUiState
                val startDate = proposalState.startDate
                val endDate = proposalState.endDate
                val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

                fun parseMonthDate(month: String) = try { monthFormat.parse(month) ?: Date(0) } catch (_: Exception) { Date(0) }

                val filteredSummaries = if (startDate != null && endDate != null) {
                    allSummaries.filter { summary ->
                        try { val d = monthFormat.parse(summary.reportMonth); d != null && d.time in startDate until endDate } catch (_: Exception) { false }
                    }
                } else allSummaries

                val agentSummaries = filteredSummaries.groupBy { it.agencyCode }.map { (agencyCode, summaries) ->
                    val monthlyBreakdown = summaries.groupBy { it.reportMonth }
                        .map { (month, ms) -> MonthlyPremium(month, ms.sumOf { it.totalScheduledPremium }) }
                        .sortedBy { parseMonthDate(it.month) }
                    val normalizedCode = agencyCode.trim().lowercase()
                    AgentSummary(
                        agencyCode = agencyCode,
                        agentName = agentNameMap[normalizedCode] ?: policyNameMap[normalizedCode] ?: "",
                        totalScheduledPremium = summaries.sumOf { it.totalScheduledPremium },
                        monthlyBreakdown = monthlyBreakdown
                    )
                }

                val perMonthMap: MutableMap<String, List<AgentSummary>> = mutableMapOf("All" to agentSummaries)
                filteredSummaries.map { it.reportMonth }.distinct().forEach { month ->
                    perMonthMap[month] = filteredSummaries.filter { it.reportMonth == month }.groupBy { it.agencyCode }.map { (agencyCode, ms) ->
                        val normalizedCode = agencyCode.trim().lowercase()
                        AgentSummary(agencyCode, agentNameMap[normalizedCode] ?: policyNameMap[normalizedCode] ?: "", ms.sumOf { it.totalScheduledPremium })
                    }
                }

                val sortedMonths = filteredSummaries.map { it.reportMonth }.distinct().sortedBy { parseMonthDate(it) }
                val months = listOf("All") + sortedMonths

                _premiumSummaryUiState.update {
                    it.copy(isLoading = false, summariesByMonth = perMonthMap, availableMonths = months, selectedMonth = "All")
                }
                calculatePremiumTotals()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching premium summaries", e)
                _premiumSummaryUiState.update { it.copy(isLoading = false, error = "Network error loading summaries.") }
            }
        }
    }

    private fun calculatePremiumTotals() {
        val currentState = _premiumSummaryUiState.value
        val appraisalYearTotal = currentState.summariesByMonth["All"]?.sumOf { it.totalScheduledPremium } ?: 0.0
        val monthlyTotal = if (currentState.selectedMonth == "All") appraisalYearTotal
        else currentState.summariesByMonth[currentState.selectedMonth]?.sumOf { it.totalScheduledPremium } ?: 0.0
        _premiumSummaryUiState.update { it.copy(monthlyTotalPremium = monthlyTotal, appraisalYearTotalPremium = appraisalYearTotal) }
    }
}
