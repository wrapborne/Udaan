package com.viplove.licadvisornative.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.viplove.licadvisornative.BuildConfig
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

object AdvisorPolicyPdfSharer {
    private const val MIME_TYPE = "application/pdf"
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private data class ColumnSpec(val title: String, val width: Float)
    private data class DueDetails(val dueFrom: String, val dueDate: String, val status: String)

    suspend fun share(
        context: Context,
        advisor: User?,
        advisorName: String,
        advisorCode: String,
        policies: List<Policy>
    ) {
        try {
            val pdfFile = withContext(Dispatchers.IO) {
                createPdf(context.applicationContext, advisor, advisorName, advisorCode, policies)
            }
            sharePdf(context, advisorName, advisorCode, policies.size, pdfFile)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not create advisor PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createPdf(
        context: Context,
        advisor: User?,
        advisorName: String,
        advisorCode: String,
        policies: List<Policy>
    ): File {
        val shareDir = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val safeCode = advisorCode.ifBlank { "advisor" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(shareDir, "advisor_policy_details_${safeCode}_${fileDateFormat.format(Date())}.pdf")

        PDDocument().use { document ->
            var page = addLandscapePage(document)
            var content = PDPageContentStream(document, page)
            var y = page.mediaBox.height - 36f

            try {
                y = drawHeader(content, page, advisor, advisorName, advisorCode, policies, y)
                y -= 12f

                val columns = listOf(
                    ColumnSpec("#", 24f),
                    ColumnSpec("Name", 108f),
                    ColumnSpec("Policy No", 74f),
                    ColumnSpec("Plan", 82f),
                    ColumnSpec("Mode", 44f),
                    ColumnSpec("Amount", 66f),
                    ColumnSpec("Due From", 70f),
                    ColumnSpec("Due Date", 70f),
                    ColumnSpec("Status", 54f)
                )

                drawTableHeader(content, columns, y)
                y -= 22f

                val sortedPolicies = policies.sortedWith(compareBy<Policy> { it.shortName.lowercase() }.thenBy { it.policyNumber })
                sortedPolicies.forEachIndexed { index, policy ->
                    if (y < 48f) {
                        content.close()
                        page = addLandscapePage(document)
                        content = PDPageContentStream(document, page)
                        y = page.mediaBox.height - 36f
                        drawTableHeader(content, columns, y)
                        y -= 22f
                    }

                    val dueDetails = calculateDueDetails(policy)
                    drawPolicyRow(
                        content = content,
                        columns = columns,
                        y = y,
                        values = listOf(
                            (index + 1).toString(),
                            policy.shortName.ifBlank { "-" },
                            policy.policyNumber.ifBlank { "-" },
                            policy.plan.ifBlank { "-" },
                            policy.mode.ifBlank { "-" },
                            "Rs. ${String.format(Locale.US, "%.2f", policy.premium)}",
                            dueDetails.dueFrom,
                            dueDetails.dueDate,
                            dueDetails.status
                        )
                    )
                    y -= 25f
                }

                y -= 8f
                drawText(
                    content,
                    "Total amount: Rs. ${String.format(Locale.US, "%.2f", policies.sumOf { it.premium })}",
                    36f,
                    max(y, 32f),
                    10f,
                    true
                )
            } finally {
                content.close()
            }

            file.outputStream().use { output -> document.save(output) }
        }

        return file
    }

    private fun addLandscapePage(document: PDDocument): PDPage {
        val page = PDPage(PDRectangle.A4).apply {
            mediaBox = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
        }
        document.addPage(page)
        return page
    }

    private fun drawHeader(
        content: PDPageContentStream,
        page: PDPage,
        advisor: User?,
        advisorName: String,
        advisorCode: String,
        policies: List<Policy>,
        startY: Float
    ): Float {
        var y = startY
        drawText(content, "Advisor Policy Details", 36f, y, 16f, true)
        drawText(content, "Generated: ${displayDateFormat.format(Date())}", page.mediaBox.width - 170f, y, 9f, false)
        y -= 24f

        drawText(content, "Advisor: ${advisorName.ifBlank { "Unknown Advisor" }}", 36f, y, 10f, true)
        drawText(content, "Code: ${advisorCode.ifBlank { "-" }}", 320f, y, 10f, false)
        drawText(content, "Policies: ${policies.size}", 470f, y, 10f, false)
        drawText(content, "Total: Rs. ${String.format(Locale.US, "%.2f", policies.sumOf { it.premium })}", 575f, y, 10f, false)
        y -= 16f

        advisor?.email?.takeIf { it.isNotBlank() }?.let {
            drawText(content, "Email: $it", 36f, y, 9f, false)
        }
        advisor?.phone?.takeIf { it.isNotBlank() }?.let {
            drawText(content, "Phone: $it", 320f, y, 9f, false)
        }
        return y
    }

    private fun drawTableHeader(content: PDPageContentStream, columns: List<ColumnSpec>, y: Float) {
        var x = 36f
        drawLine(content, 36f, y + 6f, 804f, y + 6f)
        columns.forEach { column ->
            drawText(content, column.title, x + 3f, y - 8f, 8f, true)
            x += column.width
        }
        drawLine(content, 36f, y - 16f, 804f, y - 16f)
    }

    private fun drawPolicyRow(content: PDPageContentStream, columns: List<ColumnSpec>, y: Float, values: List<String>) {
        var x = 36f
        values.forEachIndexed { index, value ->
            val width = columns[index].width - 6f
            val lines = wrap(value, width, 7f).take(2)
            lines.forEachIndexed { lineIndex, line ->
                drawText(content, line, x + 3f, y - (lineIndex * 9f), 7f, false)
            }
            x += columns[index].width
        }
        drawLine(content, 36f, y - 17f, 804f, y - 17f)
    }

    private fun drawText(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float,
        isBold: Boolean
    ) {
        content.beginText()
        content.setFont(if (isBold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, fontSize)
        content.newLineAtOffset(x, y)
        content.showText(safePdfText(text))
        content.endText()
    }

    private fun drawLine(content: PDPageContentStream, startX: Float, startY: Float, endX: Float, endY: Float) {
        content.moveTo(startX, startY)
        content.lineTo(endX, endY)
        content.stroke()
    }

    private fun wrap(text: String, width: Float, fontSize: Float): List<String> {
        val maxChars = max(4, (width / (fontSize * 0.48f)).toInt())
        val clean = text.ifBlank { "-" }
        if (clean.length <= maxChars) return listOf(clean)

        val words = clean.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (candidate.length <= maxChars) {
                current = candidate
            } else {
                if (current.isNotBlank()) lines.add(current)
                current = word.take(maxChars)
            }
        }
        if (current.isNotBlank()) lines.add(current)
        return lines.ifEmpty { listOf(clean.take(maxChars)) }
    }

    private fun calculateDueDetails(policy: Policy): DueDetails {
        val baseTimestamp = when {
            policy.lastPremiumPaidDate != null && policy.lastPremiumPaidDate!! > 0 -> policy.lastPremiumPaidDate!!
            policy.enachDate.isNotBlank() -> parseEnachDate(policy.enachDate) ?: policy.doc
            else -> policy.doc
        }
        if (baseTimestamp <= 0L) return DueDetails("-", "-", "-")

        val monthInterval = when (policy.mode.uppercase(Locale.ROOT)) {
            "YLY", "YEARLY" -> 12
            "HLY", "HALFYEARLY" -> 6
            "QLY", "QUARTERLY" -> 3
            "MLY", "MONTHLY" -> 1
            else -> null
        } ?: return DueDetails(formatDate(baseTimestamp), "-", "-")

        val dueCal = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
        val now = Calendar.getInstance()
        if (policy.lastPremiumPaidDate != null && policy.lastPremiumPaidDate!! > 0) {
            dueCal.add(Calendar.MONTH, monthInterval)
        } else {
            while (!dueCal.after(now)) {
                dueCal.add(Calendar.MONTH, monthInterval)
            }
        }

        val daysLate = ((now.timeInMillis - dueCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        val status = when {
            daysLate > 30 && daysLate <= 365 -> "Late ${daysLate}d"
            daysLate > 365 -> "Lapsed"
            else -> "Due"
        }

        return DueDetails(
            dueFrom = formatDate(baseTimestamp),
            dueDate = formatDate(dueCal.timeInMillis),
            status = status
        )
    }

    private fun parseEnachDate(date: String): Long? {
        return try {
            displayDateFormat.parse(date)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDate(timestamp: Long): String {
        return if (timestamp > 0L) displayDateFormat.format(Date(timestamp)) else "-"
    }

    private fun safePdfText(text: String): String {
        return text.filter { it.code in 32..126 }
    }

    private fun sharePdf(context: Context, advisorName: String, advisorCode: String, policyCount: Int, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider",
            pdfFile
        )
        val message = "Policy details for ${advisorName.ifBlank { advisorCode.ifBlank { "advisor" } }} ($policyCount policies)."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            setPackage(WHATSAPP_PACKAGE)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share advisor PDF"))
        }
    }
}
