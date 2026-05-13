package com.viplove.licadvisornative.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.BuildConfig
import com.viplove.licadvisornative.model.GraphicFooter
import com.viplove.licadvisornative.model.GraphicTemplate
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.toGraphicFooter
import com.viplove.licadvisornative.network.toGraphicTemplate
import com.viplove.licadvisornative.util.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GraphicsViewModel(application: Application) : AndroidViewModel(application) {

    data class GraphicsUiState(
        val templates: List<GraphicTemplate> = emptyList(),
        val footers: List<GraphicFooter> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val isUploading: Boolean = false,
        val selectedImageUri: Uri? = null
    )

    private val _uiState = MutableStateFlow(GraphicsUiState())
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api

    init {
        fetchTemplatesForCurrentUser()
        fetchFooters()
    }

    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            Log.d("GraphicsUpload_VM", "onImageSelected called with URI: $uri")
            val cachedUri = cacheImageLocally(context, uri)
            Log.d("GraphicsUpload_VM", "Image caching finished. Cached URI: $cachedUri")
            _uiState.update { it.copy(selectedImageUri = cachedUri) }
        }
    }

    private suspend fun cacheImageLocally(context: Context, uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val imagePath = File(context.filesDir, "images")
                imagePath.mkdirs()
                val file = File(imagePath, "temp_graphic_${UUID.randomUUID()}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                val authority = "${BuildConfig.APPLICATION_ID}.provider"
                FileProvider.getUriForFile(context, authority, file)
            } catch (e: Exception) {
                Log.e("GraphicsUpload_VM", "Failed to cache image", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = "Failed to process selected image.") }
                }
                null
            }
        }
    }

    fun uploadGraphicTemplate(name: String, visibleToRole: String) {
        val uri = _uiState.value.selectedImageUri
        if (uri == null) {
            _uiState.update { it.copy(error = "No image has been selected.") }
            return
        }
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Template name cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isUploading = true) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    _uiState.update { it.copy(isUploading = false, error = "Could not open image file.") }
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) { stream.readBytes().also { stream.close() } }
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", "template.jpg", requestBody)
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val roleBody = visibleToRole.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadGraphicsTemplate(nameBody, imagePart, roleBody)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isUploading = false, selectedImageUri = null) }
                    fetchTemplatesForCurrentUser()
                } else {
                    _uiState.update { it.copy(isUploading = false, error = "Upload failed: ${response.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    fun uploadGraphicFooter(name: String) {
        val uri = _uiState.value.selectedImageUri
        if (uri == null) {
            _uiState.update { it.copy(error = "No image has been selected for the footer.") }
            return
        }
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Footer name cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isUploading = true) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    _uiState.update { it.copy(isUploading = false, error = "Could not open image file.") }
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) { stream.readBytes().also { stream.close() } }
                val requestBody = bytes.toRequestBody("image/png".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", "footer.png", requestBody)
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val roleBody = "all".toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadGraphicsFooter(nameBody, imagePart, roleBody)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isUploading = false, selectedImageUri = null) }
                    fetchFooters()
                } else {
                    _uiState.update { it.copy(isUploading = false, error = "Upload failed: ${response.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun fetchTemplatesForCurrentUser() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val userRole = TokenManager.getUserRole() ?: ""
                val response = api.getGraphicsTemplates()
                if (response.isSuccessful) {
                    val all = response.body()?.map { it.toGraphicTemplate() } ?: emptyList()
                    val filtered = when (userRole) {
                        "admin" -> all.filter { it.visibleToRole == "all" || it.visibleToRole == "admin" }
                        "advisor" -> all.filter { it.visibleToRole == "all" || it.visibleToRole == "advisor" }
                        else -> all
                    }
                    _uiState.update { it.copy(isLoading = false, templates = filtered) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load templates.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun fetchFooters() {
        viewModelScope.launch {
            try {
                val response = api.getGraphicsFooters()
                if (response.isSuccessful) {
                    val footers = response.body()?.map { it.toGraphicFooter() } ?: emptyList()
                    _uiState.update { it.copy(footers = footers) }
                } else {
                    _uiState.update { it.copy(error = "Failed to load footers.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    fun deleteGraphicTemplate(template: GraphicTemplate) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val response = api.deleteGraphicsTemplate(template.id)
                if (response.isSuccessful) {
                    fetchTemplatesForCurrentUser()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to delete template.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    fun deleteGraphicFooter(footer: GraphicFooter) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val response = api.deleteGraphicsFooter(footer.id)
                if (response.isSuccessful) {
                    fetchFooters()
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to delete footer.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorMapper.map(e)) }
            }
        }
    }
}
