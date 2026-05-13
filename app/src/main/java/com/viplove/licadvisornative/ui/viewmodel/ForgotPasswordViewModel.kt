package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.ForgotPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    sealed class ForgotPasswordState {
        object Idle : ForgotPasswordState()
        object Loading : ForgotPasswordState()
        data class Success(val message: String) : ForgotPasswordState()
        data class Error(val message: String) : ForgotPasswordState()
    }

    private val _uiState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val uiState: StateFlow<ForgotPasswordState> = _uiState

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordState.Error("Please enter your email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ForgotPasswordState.Loading
            try {
                val response = ApiClient.api.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful) {
                    _uiState.value = ForgotPasswordState.Success(
                        response.body()?.message ?: "Password reset link sent! Please check your email."
                    )
                } else {
                    _uiState.value = ForgotPasswordState.Error("Unable to send reset link. Please check your email address.")
                }
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordState.Error("Network error. Please check your connection.")
            }
        }
    }
}
