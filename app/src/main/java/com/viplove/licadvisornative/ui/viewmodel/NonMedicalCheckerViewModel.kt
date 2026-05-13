// File: app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/NonMedicalCheckerViewModel.kt
package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.viplove.licadvisornative.util.CheckerInput
import com.viplove.licadvisornative.util.NonMedicalCheckerLogic
import com.viplove.licadvisornative.util.RemoteConfigManager // Import the manager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NonMedicalCheckerViewModel : ViewModel() {

    // This state holds all the user's inputs and the final result
    data class UiState(
        val age: String = "30",
        val income: String = "500000",
        val qualification: String = "Graduate",
        val profession: String = "Employed",
        val isResidentIndian: Boolean = true,
        val isMinor: Boolean = false,
        val isStudent: Boolean = false,
        val planNumber: String = NonMedicalCheckerLogic.getAvailablePlans().firstOrNull() ?: "",
        val resultCategory: String? = null,
        val resultSaLimit: Int? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    /**
     * When the ViewModel is created, initialize and fetch the latest Remote Config values.
     */
    init {
        RemoteConfigManager.initializeAndFetch()
    }

    // Functions to update the state when the user changes an input
    fun onAgeChange(value: String) { _uiState.update { it.copy(age = value) } }
    fun onIncomeChange(value: String) { _uiState.update { it.copy(income = value) } }
    fun onQualificationChange(value: String) { _uiState.update { it.copy(qualification = value) } }
    fun onProfessionChange(value: String) { _uiState.update { it.copy(profession = value) } }
    fun onIsResidentChange(value: Boolean) { _uiState.update { it.copy(isResidentIndian = value) } }
    fun onIsMinorChange(value: Boolean) { _uiState.update { it.copy(isMinor = value) } }
    fun onIsStudentChange(value: Boolean) { _uiState.update { it.copy(isStudent = value) } }
    fun onPlanNumberChange(value: String) { _uiState.update { it.copy(planNumber = value) } }

    // The main function that performs the check
    fun checkEligibility() {
        val currentState = _uiState.value
        val input = CheckerInput(
            age = currentState.age.toIntOrNull() ?: 0,
            income = currentState.income.toIntOrNull() ?: 0,
            qualification = currentState.qualification,
            profession = currentState.profession,
            isResidentIndian = currentState.isResidentIndian,
            isMinor = currentState.isMinor,
            isStudent = currentState.isStudent,
            planNumber = currentState.planNumber
        )

        val category = NonMedicalCheckerLogic.determineCategory(input)
        val limit = if (category != "Ineligible") {
            NonMedicalCheckerLogic.getSaLimits(category, input.age, input.planNumber)
        } else {
            0
        }

        _uiState.update { it.copy(resultCategory = category, resultSaLimit = limit) }
    }
}
