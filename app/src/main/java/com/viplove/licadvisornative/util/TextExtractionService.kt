package com.viplove.licadvisornative.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Service for extracting text from images using Google ML Kit
 * Supports both Hindi (Devanagari) and English (Latin) scripts
 */
class TextExtractionService(private val context: Context) {

    // Latin script recognizer for English text
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Devanagari script recognizer for Hindi text
    private val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    /**
     * Extract text from an image using ML Kit
     * Tries both Latin and Devanagari recognizers and combines results
     * 
     * @param uri URI of the image file (PNG/JPG)
     * @return Result containing extracted text and confidence score
     */
    suspend fun extractTextFromImage(uri: Uri): ExtractionResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            
            // Try Latin (English) recognition first
            val latinText = try {
                val result = latinRecognizer.process(image).await()
                result.text
            } catch (e: Exception) {
                ""
            }
            
            // Try Devanagari (Hindi) recognition
            val devanagariText = try {
                val result = devanagariRecognizer.process(image).await()
                result.text
            } catch (e: Exception) {
                ""
            }
            
            // Combine results - prefer the one with more text
            val finalText = when {
                latinText.length > devanagariText.length -> latinText
                devanagariText.isNotBlank() -> devanagariText
                else -> latinText
            }
            
            // Calculate confidence based on text length and clarity
            val confidence = calculateConfidence(finalText)
            
            val detectedLanguage = detectLanguage(latinText, devanagariText)
            
            if (finalText.isBlank()) {
                ExtractionResult.Error("No text found in image. Please ensure the image is clear and contains readable text.")
            } else {
                ExtractionResult.Success(
                    text = finalText,
                    confidence = confidence,
                    language = detectedLanguage
                )
            }
            
        } catch (e: IOException) {
            ExtractionResult.Error("Failed to read image file: ${e.message}")
        } catch (e: Exception) {
            ExtractionResult.Error("Text extraction failed: ${e.message}")
        }
    }

    /**
     * Detect the primary language of the extracted text
     */
    private fun detectLanguage(latinText: String, devanagariText: String): String {
        return when {
            latinText.isNotBlank() && devanagariText.isNotBlank() -> "both"
            devanagariText.isNotBlank() -> "hindi"
            latinText.isNotBlank() -> "english"
            else -> "unknown"
        }
    }

    /**
     * Calculate confidence score based on text characteristics
     * Higher confidence for longer, more structured text
     */
    private fun calculateConfidence(text: String): Float {
        if (text.isBlank()) return 0f
        
        var score = 0f
        
        // Base score from text length
        score += when {
            text.length > 500 -> 0.5f
            text.length > 200 -> 0.4f
            text.length > 100 -> 0.3f
            text.length > 50 -> 0.2f
            else -> 0.1f
        }
        
        // Bonus for having multiple words
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        score += when {
            words.size > 50 -> 0.3f
            words.size > 20 -> 0.2f
            words.size > 10 -> 0.1f
            else -> 0.05f
        }
        
        // Bonus for having line breaks (structured document)
        val lines = text.lines().filter { it.isNotBlank() }
        score += when {
            lines.size > 10 -> 0.2f
            lines.size > 5 -> 0.1f
            else -> 0.05f
        }
        
        return score.coerceIn(0f, 1f)
    }

    /**
     * Clean up resources
     */
    fun close() {
        latinRecognizer.close()
        devanagariRecognizer.close()
    }
}

/**
 * Result of text extraction operation
 */
sealed class ExtractionResult {
    data class Success(
        val text: String,
        val confidence: Float,
        val language: String
    ) : ExtractionResult()
    
    data class Error(val message: String) : ExtractionResult()
}
