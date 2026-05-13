package com.viplove.licadvisornative.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Utility for extracting text from PDF documents using PDFBox Android
 */
class PdfTextExtractor(private val context: Context) {

    init {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Extract text content from a PDF file
     * 
     * @param uri URI of the PDF file
     * @return Result containing extracted text or error message
     */
    suspend fun extractTextFromPdf(uri: Uri): PdfExtractionResult = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var document: PDDocument? = null
        
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext PdfExtractionResult.Error("Failed to open PDF file")
            
            document = PDDocument.load(inputStream)
            
            if (document.isEncrypted) {
                return@withContext PdfExtractionResult.Error("PDF is encrypted. Please provide an unencrypted version.")
            }
            
            val numberOfPages = document.numberOfPages
            
            if (numberOfPages == 0) {
                return@withContext PdfExtractionResult.Error("PDF has no pages")
            }
            
            // Extract text from all pages
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            
            if (text.isBlank()) {
                // PDF might be image-based (scanned document)
                PdfExtractionResult.Error(
                    "No text found in PDF. This might be a scanned document. " +
                    "Please convert to image format (PNG/JPG) for OCR processing."
                )
            } else {
                PdfExtractionResult.Success(
                    text = text,
                    pageCount = numberOfPages
                )
            }
            
        } catch (e: Exception) {
            PdfExtractionResult.Error("Failed to extract text from PDF: ${e.message}")
        } finally {
            document?.close()
            inputStream?.close()
        }
    }

    /**
     * Get page count without extracting text
     */
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var document: PDDocument? = null
        
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            document = inputStream?.let { PDDocument.load(it) }
            document?.numberOfPages ?: 0
        } catch (e: Exception) {
            0
        } finally {
            document?.close()
            inputStream?.close()
        }
    }
}

/**
 * Result of PDF text extraction
 */
sealed class PdfExtractionResult {
    data class Success(
        val text: String,
        val pageCount: Int
    ) : PdfExtractionResult()
    
    data class Error(val message: String) : PdfExtractionResult()
}
