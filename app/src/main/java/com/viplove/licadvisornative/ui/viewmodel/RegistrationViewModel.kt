package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel : ViewModel() {

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        data class Success(val message: String) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    fun registerUser(
        name: String,
        phone: String,
        email: String,
        userCode: String,
        adminDoCode: String,
        roleDisplayName: String,
        password: String,
        confirmPass: String,
        advisorRoleName: String
    ) {
        if (name.isBlank() || phone.isBlank() || email.isBlank() || userCode.isBlank() || password.isBlank()) {
            _registrationState.value = RegistrationState.Error("All fields are required.")
            return
        }
        if (password.length < 6) {
            _registrationState.value = RegistrationState.Error("Password must be at least 6 characters long.")
            return
        }
        if (password != confirmPass) {
            _registrationState.value = RegistrationState.Error("Passwords do not match.")
            return
        }
        val role = if (roleDisplayName == advisorRoleName) "advisor" else "admin"
        if (role == "advisor" && adminDoCode.isBlank()) {
            _registrationState.value = RegistrationState.Error("Development Officer's DO Code is required.")
            return
        }

        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                val response = ApiClient.api.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        password = password,
                        phone = phone,
                        role = role,
                        userCode = userCode,
                        adminDoCode = adminDoCode.ifBlank { null }
                    )
                )
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Registration successful! Waiting for approval."
                    _registrationState.value = RegistrationState.Success(message)
                } else {
                    val message = when (response.code()) {
                        409 -> response.errorBody()?.string()?.let {
                            if (it.contains("agency_code", ignoreCase = true)) "This agency code is already registered."
                            else if (it.contains("do_code", ignoreCase = true)) "This DO code is already registered."
                            else "This account already exists."
                        } ?: "This account already exists."
                        404 -> "No admin found with the provided DO code."
                        422 -> "Invalid input. Please check your details."
                        else -> "Registration failed. Please try again."
                    }
                    _registrationState.value = RegistrationState.Error(message)
                }
            } catch (e: Exception) {
                val message = e.message ?: ""
                val errorMsg = when {
                    message.contains("network", ignoreCase = true) ||
                    message.contains("connection", ignoreCase = true) ->
                        "Network error. Please check your internet connection and try again."
                    else -> e.message ?: "An unexpected error occurred. Please try again."
                }
                _registrationState.value = RegistrationState.Error(errorMsg)
            }
        }
    }
}
