package com.viplove.licadvisornative.model

/**
 * Helper class for auto-generated document metadata from OCR/text extraction.
 * This is used during the upload process to preview and edit metadata before final upload.
 */
data class DocumentMetadata(
    // Auto-extracted title from document's first heading or prominent text
    val suggestedTitle: String = "",
    
    // Auto-detected category based on keyword matching
    // e.g., "Policy Forms", "Claim Forms", "Registration Forms", "General"
    val detectedCategory: String = "",
    
    // Auto-generated tags from key terms in the document
    val extractedTags: List<String> = emptyList(),
    
    // Detected language: "hindi", "english", or "both"
    val detectedLanguage: String = "english",
    
    // Full extracted text from the document
    val extractedText: String = "",
    
    // Confidence score of the extraction (0.0 to 1.0)
    val confidence: Float = 0.0f,
    
    // Any errors or warnings during extraction
    val warnings: List<String> = emptyList()
) {
    /**
     * Returns true if extraction was successful with reasonable confidence
     */
    fun isReliable(): Boolean = confidence >= 0.7f && extractedText.isNotBlank()
    
    /**
     * Returns formatted preview text for display
     */
    fun getPreviewText(maxLength: Int = 200): String {
        return if (extractedText.length > maxLength) {
            extractedText.take(maxLength) + "..."
        } else {
            extractedText
        }
    }
}

/**
 * Categories for forms and circulars
 */
object DocumentCategories {
    const val POLICY_FORMS = "Policy Forms"
    const val CLAIM_FORMS = "Claim Forms"
    const val REGISTRATION_FORMS = "Registration Forms"
    const val SURRENDER_FORMS = "Surrender Forms"
    const val LOAN_FORMS = "Loan Forms"
    const val ULIP_FORMS = "ULIP Forms"
    const val GENERAL_CIRCULARS = "General Circulars"
    const val POLICY_UPDATES = "Policy Updates"
    const val ANNOUNCEMENTS = "Announcements"
    const val MEDICAL_FORMS = "Medical Forms"
    const val OTHER = "Other"
    
    val ALL_FORM_CATEGORIES = listOf(
        POLICY_FORMS,
        CLAIM_FORMS,
        REGISTRATION_FORMS,
        SURRENDER_FORMS,
        LOAN_FORMS,
        LOAN_FORMS,
        ULIP_FORMS,
        MEDICAL_FORMS,
        OTHER
    )
    
    val ALL_CIRCULAR_CATEGORIES = listOf(
        GENERAL_CIRCULARS,
        POLICY_UPDATES,
        ANNOUNCEMENTS,
        OTHER
    )
}

/**
 * Result wrapper for upload/download progress
 */
sealed class DocumentOperationProgress {
    data object Idle : DocumentOperationProgress()
    data class Processing(val message: String) : DocumentOperationProgress()
    data class Uploading(val progress: Int, val totalBytes: Long) : DocumentOperationProgress()
    data class Downloading(val progress: Int, val totalBytes: Long) : DocumentOperationProgress()
    data class Success(val message: String, val documentId: String? = null) : DocumentOperationProgress()
    data class Error(val message: String, val throwable: Throwable? = null) : DocumentOperationProgress()
}
