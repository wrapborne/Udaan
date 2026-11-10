package com.viplove.licadvisornative.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.viplove.licadvisornative.BuildConfig
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.BrandNavyContainer
import com.viplove.licadvisornative.ui.theme.TextPrimary
import com.viplove.licadvisornative.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What goes on a shareable graphic. */
data class GraphicSpec(
    val mainImageUrl: String,
    val footerImageUrl: String?,
    val profileImage: Any?,
    val name: String,
    val role: String,
    val phone: String
)

/**
 * Positions on the square graphic as fractions of its side, shared by the on-screen preview
 * and [GraphicRenderer] so what the user sees is what gets shared.
 */
object GraphicLayout {
    const val PADDING = 0.05f
    const val AVATAR = 0.17f
    const val AVATAR_RING = 0.008f
    const val TEXT_GAP = 0.03f
    const val NAME_TEXT = 0.05f
    const val ROLE_TEXT = 0.034f
    const val PHONE_TEXT = 0.036f
    const val LINE_GAP = 0.006f
}

object GraphicRenderer {

    /** Output size in pixels; square, which suits WhatsApp status, Instagram and Facebook posts. */
    const val SIZE = 1080

    class RenderException(message: String) : Exception(message)

    suspend fun render(context: Context, spec: GraphicSpec): Bitmap {
        val main = loadBitmap(context, spec.mainImageUrl)
            ?: throw RenderException("Couldn't load the template image. Check your connection and try again.")
        val footer = spec.footerImageUrl?.takeIf { it.isNotBlank() }?.let { loadBitmap(context, it) }
        val profile = spec.profileImage?.let { loadBitmap(context, it) }

        return withContext(Dispatchers.Default) {
            val size = SIZE.toFloat()
            val output = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            canvas.drawBitmap(main, centerCropSource(main, 1f), RectF(0f, 0f, size, size), paint)

            footer?.let {
                val height = size * it.height / it.width
                canvas.drawBitmap(it, null, RectF(0f, size - height, size, size), paint)
            }

            val padding = size * GraphicLayout.PADDING
            val avatar = size * GraphicLayout.AVATAR
            val ring = size * GraphicLayout.AVATAR_RING
            val avatarRect = RectF(padding, size - padding - avatar, padding + avatar, size - padding)
            paint.color = android.graphics.Color.WHITE
            canvas.drawOval(avatarRect, paint)
            val inner = RectF(avatarRect).apply { inset(ring, ring) }
            canvas.save()
            canvas.clipPath(Path().apply { addOval(inner, Path.Direction.CW) })
            paint.color = BrandNavyContainer.toArgb()
            canvas.drawRect(inner, paint)
            profile?.let { canvas.drawBitmap(it, centerCropSource(it, 1f), inner, paint) }
            canvas.restore()

            val textLeft = avatarRect.right + size * GraphicLayout.TEXT_GAP
            val textWidth = size - padding - textLeft
            val lines = listOf(
                textPaint(size * GraphicLayout.NAME_TEXT, TextPrimary.toArgb(), bold = true) to spec.name,
                textPaint(size * GraphicLayout.ROLE_TEXT, TextSecondary.toArgb(), bold = false) to spec.role,
                textPaint(size * GraphicLayout.PHONE_TEXT, BrandGold.toArgb(), bold = false) to spec.phone
            )
            val lineGap = size * GraphicLayout.LINE_GAP
            val blockHeight = lines.sumOf { (p, _) -> (p.fontMetrics.descent - p.fontMetrics.ascent).toDouble() }.toFloat() +
                lineGap * (lines.size - 1)
            var top = avatarRect.centerY() - blockHeight / 2
            lines.forEach { (textPaint, text) ->
                val fitted = TextUtils.ellipsize(text, textPaint, textWidth, TextUtils.TruncateAt.END).toString()
                canvas.drawText(fitted, textLeft, top - textPaint.fontMetrics.ascent, textPaint)
                top += textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent + lineGap
            }
            output
        }
    }

    /** Writes the graphic to a shareable PNG and returns its content URI. */
    suspend fun writeShareable(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_graphics").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "lic_udaan_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
    }

    /**
     * Saves the graphic to Pictures/LIC Udaan. Returns false on Android 9 and older, where this
     * needs a storage permission the app doesn't hold; callers fall back to the share sheet.
     */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "lic_udaan_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LIC Udaan")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: throw IllegalStateException("No output stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    private suspend fun loadBitmap(context: Context, data: Any): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(SIZE)
            // FILL keeps the shorter side at least SIZE, so the square crop isn't upscaled.
            .scale(Scale.FILL)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
        return result.drawable.toBitmap()
    }

    /** The source rectangle that fills a target of [aspect] (width / height) without distortion. */
    private fun centerCropSource(bitmap: Bitmap, aspect: Float): Rect {
        val sourceAspect = bitmap.width.toFloat() / bitmap.height
        return if (sourceAspect > aspect) {
            val width = (bitmap.height * aspect).toInt()
            val left = (bitmap.width - width) / 2
            Rect(left, 0, left + width, bitmap.height)
        } else {
            val height = (bitmap.width / aspect).toInt()
            val top = (bitmap.height - height) / 2
            Rect(0, top, bitmap.width, top + height)
        }
    }

    private fun textPaint(sizePx: Float, color: Int, bold: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
