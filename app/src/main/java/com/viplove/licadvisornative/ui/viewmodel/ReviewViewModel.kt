package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.toClientDataSheet
import com.viplove.licadvisornative.util.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed class ReviewUiState {
        object Loading : ReviewUiState()
        data class Success(val dataSheet: ClientDataSheet, val userRole: String) : ReviewUiState()
        data class Error(val message: String) : ReviewUiState()
    }

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api
    private val formId: String = savedStateHandle.get<String>("formId")!!

    init {
        loadDataSheet()
    }

    private fun loadDataSheet() {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading
            try {
                val userRole = TokenManager.getUserRole() ?: "advisor"

                val response = api.getDatasheet(formId)
                if (response.isSuccessful) {
                    val sheet = response.body()?.toClientDataSheet()
                    if (sheet != null) {
                        _uiState.value = ReviewUiState.Success(sheet, userRole)
                    } else {
                        _uiState.value = ReviewUiState.Error("Could not find the requested data sheet.")
                    }
                } else {
                    _uiState.value = ReviewUiState.Error("Failed to load data sheet.")
                }
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error(ErrorMapper.map(e))
            }
        }
    }
}
