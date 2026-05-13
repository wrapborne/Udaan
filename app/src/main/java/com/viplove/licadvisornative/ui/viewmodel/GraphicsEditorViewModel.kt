package com.viplove.licadvisornative.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.toUser
import com.viplove.licadvisornative.util.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GraphicsEditorViewModel : ViewModel() {

    data class EditorUiState(
        val user: User? = null,
        val isLoading: Boolean = true,
        val isUploading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api

    init {
        Log.d("GraphicsEditor", "ViewModel initialized. Starting to fetch current user.")
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            try {
                val response = api.me()
                if (response.isSuccessful) {
                    val user = response.body()?.toUser()
                    _uiState.update { it.copy(isLoading = false, user = user) }
                    Log.d("GraphicsEditor", "Successfully loaded user: ${user?.name}")
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load user data.") }
                }
            } catch (e: Exception) {
                Log.e("GraphicsEditor", "An exception occurred while fetching user.", e)
                _uiState.update { it.copy(isLoading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    fun uploadProfilePicture(context: Context, uri: Uri) {
        _uiState.update { it.copy(isUploading = true) }
        viewModelScope.launch {
            try {
                val cachedFile = cacheImageLocally(context, uri)
                if (cachedFile == null) {
                    _uiState.update { it.copy(isUploading = false, error = "Failed to process image.") }
                    return@launch
                }

                val requestBody = cachedFile.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", cachedFile.name, requestBody)

                val response = api.updateProfilePicture(part)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isUploading = false) }
                    fetchCurrentUser()
                } else {
                    _uiState.update { it.copy(isUploading = false, error = "Failed to upload profile picture.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun cacheImageLocally(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val imagePath = File(context.filesDir, "images")
            imagePath.mkdirs()
            val file = File(imagePath, "temp_profile_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e("GraphicsEditor", "Failed to cache image locally", e)
            null
        }
    }
}
