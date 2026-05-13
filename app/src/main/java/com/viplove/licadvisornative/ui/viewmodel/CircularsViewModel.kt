package com.viplove.licadvisornative.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.domain.CircularsRepository
import com.viplove.licadvisornative.domain.UploadProgress
import com.viplove.licadvisornative.model.Circular
import com.viplove.licadvisornative.model.DocumentMetadata
import com.viplove.licadvisornative.util.ExtractionResult
import com.viplove.licadvisornative.util.FileUploadUtil
import com.viplove.licadvisornative.util.MetadataGenerator
import com.viplove.licadvisornative.util.PdfExtractionResult
import com.viplove.licadvisornative.util.PdfTextExtractor
import com.viplove.licadvisornative.util.TextExtractionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Circulars (Admin-only)
 * Handles upload with auto-metadata extraction, search, and download
 */
class CircularsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CircularsRepository(application)
    private val fileUploadUtil = FileUploadUtil(application)
    private val textExtractor = TextExtractionService(application)
    private val pdfExtractor = PdfTextExtractor(application)
    private val metadataGenerator = MetadataGenerator()

    // State flows
    private val _circulars = MutableStateFlow<List<Circular>>(emptyList())
    val circulars: StateFlow<List<Circular>> = _circulars.asStateFlow()

    private val _uploadProgress = MutableStateFlow<UploadProgress>(UploadProgress.Idle)
    val uploadProgress: StateFlow<UploadProgress> = _uploadProgress.asStateFlow()

    private val _extractedMetadata = MutableStateFlow<DocumentMetadata?>(null)
    val extractedMetadata: StateFlow<DocumentMetadata?> = _extractedMetadata.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCirculars()
    }

    /**
     * Load all circulars from Firebase
     */
    private fun loadCirculars() {
        viewModelScope.launch {
            try {
                repository.getAllCirculars().collect { circularsList ->
                    _circulars.value = circularsList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load circulars: ${e.message}"
            }
        }
    }

    /**
     * Search circulars with filters
     */
    fun searchCirculars(query: String = _searchQuery.value, category: String? = _selectedCategory.value) {
        viewModelScope.launch {
            try {
                repository.searchCirculars(
                    searchQuery = query,
                    category = category
                ).collect { circularsList ->
                    _circulars.value = circularsList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchCirculars(query)
    }

    /**
     * Update selected category filter
     */
    fun updateCategoryFilter(category: String?) {
        _selectedCategory.value = category
        searchCirculars(category = category)
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        loadCirculars()
    }

    /**
     * Step 1: Extract metadata from selected file
     */
    fun extractMetadataFromFile(fileUri: Uri) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = UploadProgress.Processing("Validating file...")

                // Validate file type
                if (!fileUploadUtil.validateFileType(fileUri)) {
                    _errorMessage.value = "Invalid file type. Only PDF, PNG, and JPG files are allowed."
                    _uploadProgress.value = UploadProgress.Idle
                    return@launch
                }

                // Validate file size
                if (!fileUploadUtil.validateFileSize(fileUri)) {
                    val fileSize = fileUploadUtil.formatFileSize(fileUploadUtil.getFileSize(fileUri))
                    _errorMessage.value = "File size exceeds limit. Maximum 20 MB allowed. Your file: $fileSize"
                    _uploadProgress.value = UploadProgress.Idle
                    return@launch
                }

                _uploadProgress.value = UploadProgress.Processing("Extracting text from document...")

                // Extract text based on file type
                val fileType = fileUploadUtil.detectFileType(fileUri)
                val extractedText: String
                val confidence: Float
                val language: String

                when (fileType) {
                    "pdf" -> {
                        when (val result = pdfExtractor.extractTextFromPdf(fileUri)) {
                            is PdfExtractionResult.Success -> {
                                extractedText = result.text
                                confidence = if (extractedText.length > 100) 0.9f else 0.6f
                                language = detectLanguage(extractedText)
                            }
                            is PdfExtractionResult.Error -> {
                                _errorMessage.value = result.message
                                _uploadProgress.value = UploadProgress.Idle
                                return@launch
                            }
                        }
                    }
                    "image" -> {
                        when (val result = textExtractor.extractTextFromImage(fileUri)) {
                            is ExtractionResult.Success -> {
                                extractedText = result.text
                                confidence = result.confidence
                                language = result.language
                            }
                            is ExtractionResult.Error -> {
                                _errorMessage.value = result.message
                                _uploadProgress.value = UploadProgress.Idle
                                return@launch
                            }
                        }
                    }
                    else -> {
                        _errorMessage.value = "Unsupported file type"
                        _uploadProgress.value = UploadProgress.Idle
                        return@launch
                    }
                }

                // Generate metadata
                _uploadProgress.value = UploadProgress.Processing("Generating metadata...")
                val metadata = metadataGenerator.generateMetadata(extractedText, confidence, language)
                _extractedMetadata.value = metadata
                _uploadProgress.value = UploadProgress.Idle

            } catch (e: Exception) {
                _errorMessage.value = "Metadata extraction failed: ${e.message}"
                _uploadProgress.value = UploadProgress.Idle
            }
        }
    }

    /**
     * Step 2: Upload circular with metadata
     */
    fun uploadCircular(
        fileUri: Uri,
        title: String,
        category: String,
        tags: List<String>,
        description: String,
        userEmail: String,
        userName: String
    ) {
        viewModelScope.launch {
            try {
                repository.uploadCircular(fileUri, title, category).collect { progress ->
                    _uploadProgress.value = progress
                    if (progress is UploadProgress.Success) {
                        _extractedMetadata.value = null
                        loadCirculars()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Upload failed: ${e.message}"
                _uploadProgress.value = UploadProgress.Error("Upload failed: ${e.message}", e)
            }
        }
    }

    /**
     * Delete circular (Admin only)
     */
    fun deleteCircular(circularId: String, fileExtension: String) {
        viewModelScope.launch {
            val result = repository.deleteCircular(circularId, fileExtension)
            if (result.isFailure) {
                _errorMessage.value = "Delete failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Get download URL for a circular
     */
    suspend fun getDownloadUrl(circularId: String): String? {
        return repository.getDownloadUrl(circularId)
    }

    /**
     * Reset upload state
     */
    fun resetUploadState() {
        _uploadProgress.value = UploadProgress.Idle
        _extractedMetadata.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Detect language from text
     */
    private fun detectLanguage(text: String): String {
        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        val hasEnglish = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        
        return when {
            hasHindi && hasEnglish -> "both"
            hasHindi -> "hindi"
            hasEnglish -> "english"
            else -> "unknown"
        }
    }

    override fun onCleared() {
        super.onCleared()
        textExtractor.close()
    }
}
