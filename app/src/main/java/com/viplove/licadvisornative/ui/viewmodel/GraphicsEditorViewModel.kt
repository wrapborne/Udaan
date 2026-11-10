package com.viplove.licadvisornative.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.toUser
import com.viplove.licadvisornative.util.ErrorMapper
import com.viplove.licadvisornative.util.GraphicRenderer
import com.viplove.licadvisornative.util.GraphicSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class GraphicsEditorViewModel : ViewModel() {

    enum class ExportAction { SHARE, SAVE }

    data class EditorUiState(
        val user: User? = null,
        val isLoading: Boolean = true,
        val isUploading: Boolean = false,
        /** The photo the user just picked, shown at once instead of waiting for the upload. */
        val localProfileImage: File? = null,
        /** The export in progress, if any. Only one runs at a time. */
        val exporting: ExportAction? = null
    ) {
        val profileImage: Any? get() = localProfileImage ?: user?.profilePictureUrl?.takeIf { it.isNotEmpty() }
    }

    sealed interface EditorEvent {
        data class Message(val text: String) : EditorEvent
        data class Share(val uri: Uri) : EditorEvent
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val api = ApiClient.api

    init {
        fetchCurrentUser()
    }

    fun retry() = fetchCurrentUser()

    private fun fetchCurrentUser() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val response = api.me()
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, user = response.body()?.toUser()) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(EditorEvent.Message("Failed to load your profile."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user", e)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(EditorEvent.Message(ErrorMapper.map(e)))
            }
        }
    }

    fun uploadProfilePicture(context: Context, uri: Uri) {
        if (_uiState.value.isUploading) return
        _uiState.update { it.copy(isUploading = true) }
        viewModelScope.launch {
            try {
                val cachedFile = cacheImageLocally(context, uri)
                if (cachedFile == null) {
                    _uiState.update { it.copy(isUploading = false) }
                    _events.send(EditorEvent.Message("Couldn't read the selected photo."))
                    return@launch
                }
                val previous = _uiState.value.localProfileImage
                _uiState.update { it.copy(localProfileImage = cachedFile) }
                previous?.delete()

                val bytes = withContext(Dispatchers.IO) { cachedFile.readBytes() }
                val part = MultipartBody.Part.createFormData("image", cachedFile.name, bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                val response = api.updateProfilePicture(part)
                if (response.isSuccessful) {
                    val url = response.body()?.url
                    // The photo is stored at a fixed path, so its URL doesn't change; drop the
                    // cached copy or the old photo keeps showing elsewhere in the app.
                    url?.let { forgetCachedImage(context, it) }
                    _uiState.update { state ->
                        state.copy(isUploading = false, user = state.user?.copy(profilePictureUrl = url ?: state.user.profilePictureUrl))
                    }
                    _events.send(EditorEvent.Message("Profile photo updated."))
                } else {
                    _uiState.update { it.copy(isUploading = false) }
                    _events.send(EditorEvent.Message("Failed to upload profile photo. It's shown here but not saved."))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false) }
                _events.send(EditorEvent.Message(ErrorMapper.map(e)))
            }
        }
    }

    /** Renders the graphic at full size, then shares it or saves it to the gallery. */
    fun export(context: Context, spec: GraphicSpec, action: ExportAction) {
        var started = false
        _uiState.update { state ->
            if (state.exporting != null) state else state.copy(exporting = action).also { started = true }
        }
        if (!started) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val bitmap = GraphicRenderer.render(appContext, spec)
                when (action) {
                    ExportAction.SHARE -> _events.send(EditorEvent.Share(GraphicRenderer.writeShareable(appContext, bitmap)))
                    ExportAction.SAVE -> if (GraphicRenderer.saveToGallery(appContext, bitmap)) {
                        _events.send(EditorEvent.Message("Saved to Pictures › LIC Udaan."))
                    } else {
                        // Older Android needs a storage permission; the share sheet can still save it.
                        _events.send(EditorEvent.Share(GraphicRenderer.writeShareable(appContext, bitmap)))
                    }
                }
            } catch (e: GraphicRenderer.RenderException) {
                _events.send(EditorEvent.Message(e.message ?: "Couldn't create the graphic."))
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _events.send(EditorEvent.Message("Couldn't create the graphic. Please try again."))
            } finally {
                _uiState.update { it.copy(exporting = null) }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun forgetCachedImage(context: Context, url: String) {
        context.imageLoader.diskCache?.remove(url)
        context.imageLoader.memoryCache?.clear()
    }

    private suspend fun cacheImageLocally(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val file = File(dir, "temp_profile_${UUID.randomUUID()}.jpg")
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> file.outputStream().use { source.copyTo(it) } }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache image locally", e)
            null
        }
    }

    override fun onCleared() {
        _uiState.value.localProfileImage?.delete()
        super.onCleared()
    }

    private companion object {
        const val TAG = "GraphicsEditor"
    }
}
