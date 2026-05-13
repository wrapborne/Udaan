package com.viplove.licadvisornative.domain

import android.app.Application
import android.net.Uri
import com.viplove.licadvisornative.model.Form
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.toForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class FormsRepository(private val application: Application) {

    private val api = ApiClient.api

    fun uploadForm(fileUri: Uri, title: String, category: String): Flow<UploadProgress> = flow {
        emit(UploadProgress.Processing("Preparing upload..."))
        try {
            val contentResolver = application.contentResolver
            val stream = contentResolver.openInputStream(fileUri)
            if (stream == null) {
                emit(UploadProgress.Error("Could not open file. Please check file permissions."))
                return@flow
            }

            emit(UploadProgress.Processing("Uploading..."))

            val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
            val fileName = fileUri.lastPathSegment ?: "file"
            val bytes = stream.readBytes()
            stream.close()

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadForm(titleBody, categoryBody, filePart)
            if (response.isSuccessful) {
                val id = response.body()?.id ?: ""
                emit(UploadProgress.Success("Form uploaded successfully", id))
            } else {
                emit(UploadProgress.Error("Upload failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(UploadProgress.Error("Upload failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    fun getAllForms(): Flow<List<Form>> = flow {
        try {
            val response = api.getForms()
            if (response.isSuccessful) {
                emit(response.body()?.map { it.toForm() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun searchForms(
        searchQuery: String = "",
        category: String? = null
    ): Flow<List<Form>> = flow {
        try {
            val response = api.getForms()
            if (response.isSuccessful) {
                var list = response.body()?.map { it.toForm() } ?: emptyList()
                if (!category.isNullOrBlank()) {
                    list = list.filter { it.category.equals(category, ignoreCase = true) }
                }
                if (searchQuery.isNotBlank()) {
                    val lq = searchQuery.lowercase()
                    list = list.filter {
                        it.title.lowercase().contains(lq) ||
                        it.category.lowercase().contains(lq)
                    }
                }
                emit(list)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteForm(formId: String, fileExtension: String = ""): Result<Unit> {
        return try {
            val response = api.deleteForm(formId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDownloadUrl(formId: String): String? = null
}

sealed class UploadProgress {
    data object Idle : UploadProgress()
    data class Processing(val message: String) : UploadProgress()
    data class Uploading(val progress: Int, val totalBytes: Long) : UploadProgress()
    data class Success(val message: String, val documentId: String) : UploadProgress()
    data class Error(val message: String, val throwable: Throwable? = null) : UploadProgress()
}
