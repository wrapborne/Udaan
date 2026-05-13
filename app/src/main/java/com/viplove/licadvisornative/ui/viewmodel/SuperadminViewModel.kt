package com.viplove.licadvisornative.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.toUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SuperadminViewModel : ViewModel() {

    sealed class UserListUiState {
        object Loading : UserListUiState()
        data class Success(val users: List<User>) : UserListUiState()
        data class Error(val message: String) : UserListUiState()
    }

    private val _uiState = MutableStateFlow<UserListUiState>(UserListUiState.Loading)
    val uiState: StateFlow<UserListUiState> = _uiState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        fetchAdmins()
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            try {
                val response = ApiClient.api.me()
                if (response.isSuccessful) {
                    _currentUser.value = response.body()?.toUser()
                }
            } catch (e: Exception) {
                // Ignore — non-critical
            }
        }
    }

    private fun fetchAdmins() {
        viewModelScope.launch {
            _uiState.value = UserListUiState.Loading
            try {
                val response = ApiClient.api.getAllAdmins()
                if (response.isSuccessful) {
                    val adminList = response.body()?.map { it.toUser() } ?: emptyList()
                    _uiState.value = UserListUiState.Success(adminList)
                } else {
                    _uiState.value = UserListUiState.Error("Failed to fetch admins.")
                }
            } catch (e: Exception) {
                _uiState.value = UserListUiState.Error(e.message ?: "Failed to fetch admins.")
            }
        }
    }

    fun approveAdmin(uid: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.api.approveUser(uid)
                if (response.isSuccessful) {
                    fetchAdmins()
                }
            } catch (e: Exception) {
                // Refresh list anyway
                fetchAdmins()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { ApiClient.api.logout() } catch (_: Exception) {}
            TokenManager.clearAll()
        }
    }
}
