package com.viplove.licadvisornative.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utilities for file validation, thumbnail generation, and file type detection
 */
class FileUploadUtil(private val context: Context) {

    companion object {
        const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L // 20 MB
        const val THUMBNAIL_WIDTH = 300
        const val THUMBNAIL_HEIGHT = 400
        
        val ALLOWED_MIME_TYPES = setOf(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg"
        )
    }

    /**
     * Validate file size
     * @return true if file is within size limit
     */
    suspend fun validateFileSize(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileSize = getFileSize(uri)
            fileSize > 0 && fileSize <= MAX_FILE_SIZE_BYTES
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file size in bytes
     */
    suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Detect MIME type of file
     */
    fun detectMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Validate file type
     * @return true if file type is allowed
     */
    fun validateFileType(uri: Uri): Boolean {
        val mimeType = detectMimeType(uri)
        return mimeType in ALLOWED_MIME_TYPES
    }

    /**
     * Detect if file is PDF or image
     * @return "pdf" or "image" or "unknown"
     */
    fun detectFileType(uri: Uri): String {
        val mimeType = detectMimeType(uri)
        return when {
            mimeType == "application/pdf" -> "pdf"
            mimeType?.startsWith("image/") == true -> "image"
            else -> "unknown"
        }
    }

    /**
     * Get file extension from URI
     */
    fun getFileExtension(uri: Uri): String {
        val mimeType = detectMimeType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
    }

    /**
     * Get file name from URI
     */
    fun getFileName(uri: Uri): String {
        var fileName = "unknown"
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        
        return fileName
    }

    /**
     * Generate thumbnail for PDF (first page) or image
     * @return Bitmap thumbnail or null if generation fails
     */
    suspend fun generateThumbnail(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            when (detectFileType(uri)) {
                "pdf" -> generatePdfThumbnail(uri)
                "image" -> generateImageThumbnail(uri)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate thumbnail from PDF first page
     */
    private fun generatePdfThumbnail(uri: Uri): Bitmap? {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        
        return try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            
            if (parcelFileDescriptor == null) return null
            
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            
            if (pdfRenderer.pageCount == 0) return null
            
            val page = pdfRenderer.openPage(0)
            
            // Calculate dimensions maintaining aspect ratio
            val scale = minOf(
                THUMBNAIL_WIDTH.toFloat() / page.width,
                THUMBNAIL_HEIGHT.toFloat() / page.height
            )
            
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        }
    }

    /**
     * Generate thumbnail from image
     */
    private suspend fun generateImageThumbnail(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            
            // Decode with inJustDecodeBounds to get dimensions
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate sample size
            val sampleSize = calculateInSampleSize(options, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            
            // Decode with sample size
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options2 = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream2, null, options2)
            inputStream2.close()
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate sample size for bitmap loading
     */
    private fun calculateInSampleSize(
        options: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }

    /**
     * Save bitmap to cache directory and return URI
     */
    suspend fun saveThumbnailToCache(bitmap: Bitmap, fileName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "thumbnails")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, "$fileName.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
