package com.viplove.licadvisornative.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UlipPlanUiState(
    val planNumbers: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class UlipPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ulip_plan_config", android.content.Context.MODE_PRIVATE)
    private val KEY_PLAN_NUMBERS = "planNumbers"

    private val defaultPlanNumbers = listOf("735", "749", "752", "867", "873")

    private val _uiState = MutableStateFlow(UlipPlanUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPlanNumbers()
    }

    private fun loadPlanNumbers() {
        val saved = prefs.getStringSet(KEY_PLAN_NUMBERS, null)
        val numbers = if (saved != null) saved.toList().sorted() else defaultPlanNumbers
        _uiState.update { it.copy(isLoading = false, planNumbers = numbers) }
    }

    fun addPlanNumber(planNumber: String) {
        if (planNumber.isBlank()) return
        val current = _uiState.value.planNumbers.toMutableList()
        val trimmed = planNumber.trim()
        if (trimmed !in current) {
            current.add(trimmed)
            val sorted = current.sorted()
            prefs.edit().putStringSet(KEY_PLAN_NUMBERS, sorted.toSet()).apply()
            _uiState.update { it.copy(planNumbers = sorted) }
        }
    }

    fun removePlanNumber(planNumber: String) {
        val current = _uiState.value.planNumbers.toMutableList()
        current.remove(planNumber)
        prefs.edit().putStringSet(KEY_PLAN_NUMBERS, current.toSet()).apply()
        _uiState.update { it.copy(planNumbers = current.sorted()) }
    }
}
