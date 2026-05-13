package com.viplove.licadvisornative.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.LoginRequest
import com.viplove.licadvisornative.network.LookupByCodeRequest
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.util.CredentialsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    data class LoginCredentials(
        val email: String = "",
        val password: String = "",
        val rememberMe: Boolean = false
    )

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        data class Success(val userRole: String) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
        data class Loaded(val credentials: LoginCredentials) : LoginUiState()
    }

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val context = getApplication<Application>().applicationContext

    init {
        loadSavedCredentials()
    }

    fun loadSavedCredentials() {
        viewModelScope.launch {
            val credentials = CredentialsManager.loadCredentials(context)
            if (credentials.rememberMe) {
                _loginUiState.value = LoginUiState.Loaded(credentials)
            }
        }
    }

    fun loginUser(emailOrCode: String, password: String, rememberMe: Boolean) {
        if (emailOrCode.isBlank() || password.isBlank()) {
            _loginUiState.value = LoginUiState.Error("Email/Code and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            try {
                _loginUiState.value = LoginUiState.Loading

                // Determine if input is email or code
                val email = if (emailOrCode.contains("@")) {
                    emailOrCode
                } else {
                    lookupEmailByCode(emailOrCode)
                }

                if (email.isBlank()) {
                    _loginUiState.value = LoginUiState.Error("No account found with this code.")
                    return@launch
                }

                val response = ApiClient.api.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val authResponse = response.body()!!
                    TokenManager.saveToken(authResponse.accessToken)
                    TokenManager.saveUserId(authResponse.user.id)
                    TokenManager.saveUserRole(authResponse.user.role)

                    if (rememberMe) {
                        CredentialsManager.saveCredentials(
                            context,
                            LoginCredentials(emailOrCode, password, true)
                        )
                    } else {
                        CredentialsManager.clearCredentials(context)
                    }

                    _loginUiState.value = LoginUiState.Success(authResponse.user.role)
                } else {
                    val message = when (response.code()) {
                        401 -> "Invalid email or password."
                        403 -> "Your account is pending approval."
                        else -> "Login failed. Please try again."
                    }
                    _loginUiState.value = LoginUiState.Error(message)
                }
            } catch (e: Exception) {
                _loginUiState.value = LoginUiState.Error("Network error. Please check your connection.")
            }
        }
    }

    private suspend fun lookupEmailByCode(code: String): String {
        return try {
            val response = ApiClient.api.lookupEmailByCode(LookupByCodeRequest(code))
            if (response.isSuccessful) {
                response.body()?.email ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
