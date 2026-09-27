package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.GmailImportHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GmailImportStatusViewModel : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val items: List<GmailImportHistoryItem> = emptyList(),
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getGmailImportHistory()
                if (response.isSuccessful) {
                    _uiState.value = UiState(items = response.body().orEmpty())
                } else {
                    _uiState.value = UiState(error = "Could not load import history.")
                }
            } catch (error: Exception) {
                _uiState.value = UiState(error = error.localizedMessage ?: "Could not load import history.")
            }
        }
    }
}
