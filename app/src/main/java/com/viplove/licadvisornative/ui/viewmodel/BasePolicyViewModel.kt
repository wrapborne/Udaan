// File: app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/BasePolicyViewModel.kt
package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.viplove.licadvisornative.domain.FilterPoliciesUseCase
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * An abstract base class for ViewModels that need to display and filter a list of policies.
 * This class now uses a dedicated UseCase for filtering logic.
 */
abstract class BasePolicyViewModel : ViewModel() {

    // Instantiate the use case for filtering policies.
    private val filterPoliciesUseCase = FilterPoliciesUseCase()

    // Abstract properties that child ViewModels must implement
    protected abstract val _uiState: MutableStateFlow<PolicyFilterableUiState>
    abstract fun applyFilters()

    /**
     * A generic interface to represent a UI state that can be filtered.
     * Child ViewModel UiState data classes should implement this.
     */
    interface PolicyFilterableUiState {
        val allPolicies: List<Policy>
        val filteredPolicies: List<Policy>
        val searchQuery: String
        val selectedPlan: String
        val selectedMode: String
        val startDate: Long?
        val endDate: Long?
        val selectedFinancialYear: String
        val selectedAppraisalYear: String
        val showOnlyLate: Boolean
        val currentUser: User?
    }

    // --- Common Public Functions (These remain unchanged) ---

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copyState(searchQuery = query) }
        applyFilters()
    }

    fun onPlanSelected(plan: String) {
        _uiState.update { it.copyState(selectedPlan = plan) }
        applyFilters()
    }

    fun onModeSelected(mode: String) {
        _uiState.update { it.copyState(selectedMode = mode) }
        applyFilters()
    }

    fun onDateRangeSelected(start: Long?, end: Long?) {
        _uiState.update { it.copyState(startDate = start, endDate = end, selectedFinancialYear = "None", selectedAppraisalYear = "None") }
        applyFilters()
    }

    fun onFinancialYearSelected(year: String) {
        _uiState.update { it.copyState(selectedFinancialYear = year, startDate = null, endDate = null, selectedAppraisalYear = "None") }
        applyFilters()
    }

    open fun onAppraisalYearSelected(year: String) {
        _uiState.update { it.copyState(selectedAppraisalYear = year, startDate = null, endDate = null, selectedFinancialYear = "None") }
        applyFilters()
    }


    // --- Simplified Protected Helper Function ---

    /**
     * Gets the filtered list of policies by delegating to the FilterPoliciesUseCase.
     */
    protected fun getFilteredPolicies(state: PolicyFilterableUiState): List<Policy> {
        val params = FilterPoliciesUseCase.Params(
            allPolicies = state.allPolicies,
            searchQuery = state.searchQuery,
            selectedPlan = state.selectedPlan,
            selectedMode = state.selectedMode,
            startDate = state.startDate,
            endDate = state.endDate,
            selectedFinancialYear = state.selectedFinancialYear,
            selectedAppraisalYear = state.selectedAppraisalYear,
            showOnlyLate = state.showOnlyLate,
            user = state.currentUser
        )
        return filterPoliciesUseCase.execute(params)
    }

    // --- UI-related Helper Functions (These remain in the ViewModel) ---

    protected open fun generateFinancialYearOptions(): List<String> {
        val options = mutableListOf<String>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        for (i in 0..5) {
            val endYear = currentYear - i
            val startYear = endYear - 1
            options.add("Financial Year $startYear - $endYear")
        }
        return options
    }

    protected open fun generateAppraisalYearOptions(startDateMillis: Long?): List<String> {
        if (startDateMillis == null) return emptyList()
        val options = mutableListOf<String>()
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val today = Calendar.getInstance()
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        while (startCal.before(today) || startCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            val endCal = (startCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
            options.add("Appraisal ${yearFormat.format(startCal.time)} - ${yearFormat.format(endCal.time)}")
            startCal.add(Calendar.YEAR, 1)
        }
        return options.reversed()
    }

    /**
     * A helper extension function to make copying the state more generic.
     */
    private fun PolicyFilterableUiState.copyState(
        allPolicies: List<Policy> = this.allPolicies,
        filteredPolicies: List<Policy> = this.filteredPolicies,
        searchQuery: String = this.searchQuery,
        selectedPlan: String = this.selectedPlan,
        selectedMode: String = this.selectedMode,
        startDate: Long? = this.startDate,
        endDate: Long? = this.endDate,
        selectedFinancialYear: String = this.selectedFinancialYear,
        selectedAppraisalYear: String = this.selectedAppraisalYear,
        currentUser: User? = this.currentUser
    ): PolicyFilterableUiState {
        return when (this) {
            is AdminViewModel.ProposalsUiState -> this.copy(
                allPolicies = allPolicies,
                filteredPolicies = filteredPolicies,
                searchQuery = searchQuery,
                selectedPlan = selectedPlan,
                selectedMode = selectedMode,
                startDate = startDate,
                endDate = endDate,
                selectedFinancialYear = selectedFinancialYear,
                selectedAppraisalYear = selectedAppraisalYear,
                currentUser = currentUser
            )
            is AgentViewModel.AgentUiState -> this.copy(
                allPolicies = allPolicies,
                filteredPolicies = filteredPolicies,
                searchQuery = searchQuery,
                selectedPlan = selectedPlan,
                selectedMode = selectedMode,
                startDate = startDate,
                endDate = endDate,
                selectedFinancialYear = selectedFinancialYear,
                selectedAppraisalYear = selectedAppraisalYear,
                currentUser = currentUser
            )
            else -> this
        }
    }
}
