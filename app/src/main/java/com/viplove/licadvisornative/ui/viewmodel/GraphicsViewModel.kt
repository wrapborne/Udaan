package com.viplove.licadvisornative.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.GraphicFooter
import com.viplove.licadvisornative.model.GraphicTemplate
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.toGraphicFooter
import com.viplove.licadvisornative.network.toGraphicTemplate
import com.viplove.licadvisornative.util.ErrorMapper
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

class GraphicsViewModel(application: Application) : AndroidViewModel(application) {

    enum class GraphicKind { TEMPLATE, FOOTER }

    data class GraphicsUiState(
        val templates: List<GraphicTemplate> = emptyList(),
        val footers: List<GraphicFooter> = emptyList(),
        val isLoading: Boolean = true,
        val templateDraft: GraphicUploadDraft = GraphicUploadDraft(),
        val footerDraft: GraphicUploadDraft = GraphicUploadDraft()
    ) {
        fun draft(kind: GraphicKind) = if (kind == GraphicKind.TEMPLATE) templateDraft else footerDraft
    }

    private val _uiState = MutableStateFlow(GraphicsUiState())
    val uiState = _uiState.asStateFlow()

    /** One-shot messages (errors and confirmations). Each one is shown once, even if repeated. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val api = ApiClient.api

    init {
        fetchTemplatesForCurrentUser()
        fetchFooters()
    }

    fun refresh() {
        fetchTemplatesForCurrentUser()
        fetchFooters()
    }

    fun onNameChanged(kind: GraphicKind, name: String) = updateDraft(kind) { it.withName(name) }

    fun onRoleChanged(role: String) = updateDraft(GraphicKind.TEMPLATE) { it.withRole(role) }

    fun onImageSelected(context: Context, kind: GraphicKind, uri: Uri) {
        if (_uiState.value.draft(kind).isUploading) return
        viewModelScope.launch {
            val picked = copyToAppStorage(context, uri)
            if (picked == null) {
                _messages.send("Couldn't read the selected image. Try another one.")
                return@launch
            }
            var replaced: PickedImage? = null
            var accepted = false
            updateDraft(kind) { draft ->
                if (draft.isUploading) draft else {
                    replaced = draft.image
                    accepted = true
                    draft.withImage(picked)
                }
            }
            if (accepted) replaced?.file?.delete() else picked.file.delete()
        }
    }

    fun clearImage(kind: GraphicKind) {
        var removed: PickedImage? = null
        updateDraft(kind) { draft ->
            if (draft.isUploading) draft else draft.also { removed = it.image }.withImage(null)
        }
        removed?.file?.delete()
    }

    fun upload(kind: GraphicKind) {
        val current = _uiState.value.draft(kind)
        if (current.image == null) {
            viewModelScope.launch { _messages.send("Select an image first.") }
            return
        }
        if (current.name.isBlank()) {
            viewModelScope.launch { _messages.send("Enter a name first.") }
            return
        }
        var started: GraphicUploadDraft? = null
        updateDraft(kind) { draft -> draft.startUpload()?.also { started = it } ?: draft }
        val draft = started ?: return
        val image = draft.image ?: return

        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { image.file.readBytes() }
                val label = if (kind == GraphicKind.TEMPLATE) "template" else "footer"
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    "$label.${image.extension}",
                    bytes.toRequestBody(image.mimeType.toMediaTypeOrNull())
                )
                val nameBody = draft.name.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                val role = if (kind == GraphicKind.TEMPLATE) draft.visibleToRole else GraphicUploadDraft.ROLE_ALL
                val roleBody = role.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = if (kind == GraphicKind.TEMPLATE) {
                    api.uploadGraphicsTemplate(nameBody, imagePart, roleBody)
                } else {
                    api.uploadGraphicsFooter(nameBody, imagePart, roleBody)
                }
                if (response.isSuccessful) {
                    updateDraft(kind) { it.uploadSucceeded() }
                    image.file.delete()
                    _messages.send(if (kind == GraphicKind.TEMPLATE) "Template uploaded." else "Footer uploaded.")
                    if (kind == GraphicKind.TEMPLATE) fetchTemplatesForCurrentUser() else fetchFooters()
                } else {
                    updateDraft(kind) { it.uploadFailed() }
                    _messages.send("Upload failed: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                updateDraft(kind) { it.uploadFailed() }
                _messages.send(ErrorMapper.map(e))
            }
        }
    }

    fun deleteGraphicTemplate(template: GraphicTemplate) {
        viewModelScope.launch {
            try {
                val response = api.deleteGraphicsTemplate(template.id)
                if (response.isSuccessful) {
                    _uiState.update { state -> state.copy(templates = state.templates.filterNot { it.id == template.id }) }
                    _messages.send("Template deleted.")
                } else {
                    _messages.send("Failed to delete template.")
                }
            } catch (e: Exception) {
                _messages.send(ErrorMapper.map(e))
            }
        }
    }

    fun deleteGraphicFooter(footer: GraphicFooter) {
        viewModelScope.launch {
            try {
                val response = api.deleteGraphicsFooter(footer.id)
                if (response.isSuccessful) {
                    _uiState.update { state -> state.copy(footers = state.footers.filterNot { it.id == footer.id }) }
                    _messages.send("Footer deleted.")
                } else {
                    _messages.send("Failed to delete footer.")
                }
            } catch (e: Exception) {
                _messages.send(ErrorMapper.map(e))
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
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.send("Failed to load templates.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _messages.send(ErrorMapper.map(e))
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
                    _messages.send("Failed to load footers.")
                }
            } catch (e: Exception) {
                _messages.send(ErrorMapper.map(e))
            }
        }
    }

    private inline fun updateDraft(kind: GraphicKind, crossinline step: (GraphicUploadDraft) -> GraphicUploadDraft) {
        _uiState.update { state ->
            when (kind) {
                GraphicKind.TEMPLATE -> state.copy(templateDraft = step(state.templateDraft))
                GraphicKind.FOOTER -> state.copy(footerDraft = step(state.footerDraft))
            }
        }
    }

    /** Copies the picked image into app storage, keeping its real MIME type and extension. */
    private suspend fun copyToAppStorage(context: Context, uri: Uri): PickedImage? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val file = File(dir, "$TEMP_PREFIX${UUID.randomUUID()}.$extension")
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> file.outputStream().use { source.copyTo(it) } }
            PickedImage(file, mimeType, extension)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy selected image", e)
            null
        }
    }

    override fun onCleared() {
        val state = _uiState.value
        listOfNotNull(state.templateDraft.image, state.footerDraft.image).forEach { it.file.delete() }
        super.onCleared()
    }

    private companion object {
        const val TAG = "GraphicsViewModel"
        const val TEMP_PREFIX = "temp_graphic_"
    }
}
