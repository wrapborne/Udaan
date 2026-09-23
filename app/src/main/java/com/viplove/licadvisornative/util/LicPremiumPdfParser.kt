package com.viplove.licadvisornative.util

import android.content.ContentResolver
import android.net.Uri
import com.viplove.licadvisornative.model.CommissionBillPdfResult
import com.viplove.licadvisornative.model.CommissionBillPdfRow
import com.viplove.licadvisornative.model.CommissionRowStatus
import com.viplove.licadvisornative.model.LicPremiumPdfParseResult
import com.viplove.licadvisornative.model.LicPremiumPdfType
import com.viplove.licadvisornative.model.PdfRgbColor
import com.viplove.licadvisornative.model.PremiumDuePdfResult
import com.viplove.licadvisornative.model.PremiumDuePdfRow
import com.viplove.licadvisornative.model.PremiumYearType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream
import kotlin.math.abs

object LicPremiumPdfParser {

    private val streamRegex = Regex("stream\\r?\\n(.*?)\\r?\\nendstream", RegexOption.DOT_MATCHES_ALL)
    private val textRegex = Regex("""BT\s+(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+Td\s+\((.*?)\)\s*Tj""", RegexOption.DOT_MATCHES_ALL)
    private val colorRegex = Regex("""(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s+rg""")
    private val filledRectRegex = Regex("""(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s+rg\s+[-\d.]+\s+[-\d.]+\s+[-\d.]+\s+[-\d.]+\s+re\s+f""")

    suspend fun parse(uri: Uri, contentResolver: ContentResolver): LicPremiumPdfParseResult = withContext(Dispatchers.IO) {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext LicPremiumPdfParseResult.Error("Unable to open PDF.")
        parse(bytes)
    }

    fun parse(pdfBytes: ByteArray): LicPremiumPdfParseResult {
        val cells = extractCells(pdfBytes)
        if (cells.isEmpty()) {
            return LicPremiumPdfParseResult.Error("No readable text was found in the PDF.")
        }

        return when (detectType(cells)) {
            LicPremiumPdfType.PREMIUM_DUE_LIST -> LicPremiumPdfParseResult.PremiumDueList(parsePremiumDueList(cells))
            LicPremiumPdfType.COMMISSION_BILL -> LicPremiumPdfParseResult.CommissionBill(parseCommissionBill(cells))
            LicPremiumPdfType.UNKNOWN -> LicPremiumPdfParseResult.Error("This PDF is not a recognized LIC premium due list or commission bill.")
        }
    }

    fun detectType(pdfBytes: ByteArray): LicPremiumPdfType = detectType(extractCells(pdfBytes))

    private fun detectType(cells: List<PdfCell>): LicPremiumPdfType {
        val allText = cells.joinToString(" ") { it.text }
        return when {
            allText.contains("Premium Due List", ignoreCase = true) -> LicPremiumPdfType.PREMIUM_DUE_LIST
            allText.contains("Agency Commission", ignoreCase = true) -> LicPremiumPdfType.COMMISSION_BILL
            else -> LicPremiumPdfType.UNKNOWN
        }
    }

    private fun parsePremiumDueList(cells: List<PdfCell>): PremiumDuePdfResult {
        val warnings = mutableListOf<String>()
        val rows = groupRows(cells)
        val dataRows = rows.mapNotNull { row ->
            val columns = row.columnsByPremiumDueLayout()
            val serial = columns[DueColumn.SERIAL]?.text?.toIntOrNull() ?: return@mapNotNull null
            val policyNumber = columns[DueColumn.POLICY_NUMBER]?.text.orEmpty().onlyDigits()
            if (policyNumber.length < 6) return@mapNotNull null

            val flag = columns[DueColumn.FLAG]?.text.orEmpty().uppercase()
            val yearType = when (flag) {
                "FY" -> PremiumYearType.FIRST_YEAR
                "ST" -> PremiumYearType.RENEWAL
                else -> PremiumYearType.UNKNOWN
            }
            if (yearType == PremiumYearType.UNKNOWN) {
                warnings += "Policy $policyNumber has an unknown premium year flag."
            }

            val colors = row.flatMap { listOfNotNull(it.fillColor, it.textColor) }
            PremiumDuePdfRow(
                serialNumber = serial,
                policyNumber = policyNumber,
                policyHolderName = columns[DueColumn.NAME]?.text.orEmpty().squashSpaces(),
                dateOfCommencement = columns[DueColumn.DOC]?.text.orEmpty(),
                planTerm = columns[DueColumn.PLAN_TERM]?.text.orEmpty(),
                mode = columns[DueColumn.MODE]?.text.orEmpty(),
                fupMonth = columns[DueColumn.FUP]?.text.orEmpty(),
                premiumYearType = yearType,
                isLapsed = row.any { it.fillColor?.isLapsedRed() == true || it.textColor?.isLapsedRed() == true },
                installmentPremium = columns[DueColumn.INSTALLMENT_PREMIUM]?.text.toAmount(),
                dueCount = columns[DueColumn.DUE_COUNT]?.text?.toIntOrNull() ?: 0,
                gst = columns[DueColumn.GST]?.text.toAmount(),
                totalPremium = columns[DueColumn.TOTAL_PREMIUM]?.text.toAmount(),
                estimatedCommission = columns[DueColumn.ESTIMATED_COMMISSION]?.text.toAmount(),
                rowFillColors = colors.distinct()
            )
        }

        return PremiumDuePdfResult(
            branchCode = findAfter(cells, "Branch Code:"),
            agentName = findAfter(cells, "Agent Name :").squashSpaces(),
            agentCode = findAfter(cells, "Agent Code :"),
            reportMonth = cells.firstOrNull { it.text.contains("Premium Due List", ignoreCase = true) }
                ?.text
                ?.substringAfterLast(" For ", "")
                .orEmpty(),
            rows = dataRows.sortedBy { it.serialNumber },
            warnings = warnings.distinct()
        )
    }

    private fun parseCommissionBill(cells: List<PdfCell>): CommissionBillPdfResult {
        val warnings = mutableListOf<String>()
        val rows = groupRows(cells)
        val dataRows = rows.mapNotNull { row ->
            val columns = row.columnsByCommissionLayout()
            val serial = columns[CommissionColumn.SERIAL]?.text?.toIntOrNull() ?: return@mapNotNull null
            val policyNumber = columns[CommissionColumn.POLICY_NUMBER]?.text.orEmpty().onlyDigits()
            val dueDate = columns[CommissionColumn.DUE_DATE]?.text.orEmpty()
            if (policyNumber.length < 6 || !dueDate.contains("/")) return@mapNotNull null

            val premium = columns[CommissionColumn.PREMIUM]?.text.toAmount()
            val commission = columns[CommissionColumn.COMMISSION]?.text.toAmount()
            val status = if (premium < 0.0 || commission < 0.0) {
                CommissionRowStatus.COOLING_OFF_REVERSAL
            } else {
                CommissionRowStatus.PAID
            }

            if (status == CommissionRowStatus.COOLING_OFF_REVERSAL) {
                warnings += "Policy $policyNumber has a cooling-off/reversal row for $dueDate."
            }

            CommissionBillPdfRow(
                serialNumber = serial,
                policyHolderName = columns[CommissionColumn.NAME]?.text.orEmpty().squashSpaces(),
                policyNumber = policyNumber,
                planTerm = columns[CommissionColumn.PLAN_TERM]?.text.orEmpty(),
                dueDate = dueDate,
                riskDate = columns[CommissionColumn.RISK_DATE]?.text.orEmpty(),
                cbo = columns[CommissionColumn.CBO]?.text.orEmpty(),
                adjustmentDate = columns[CommissionColumn.ADJUSTMENT_DATE]?.text.orEmpty(),
                premium = premium,
                commission = commission,
                status = status
            )
        }

        val summaryText = cells.firstOrNull { it.text.contains("Summary of Premium and Commission", ignoreCase = true) }?.text.orEmpty()
        val reportMonth = Regex("""of\s+(\d{2}/\d{4})""").find(summaryText)?.groupValues?.getOrNull(1).orEmpty()
        val batch = Regex("""\(([^)]*batch[^)]*)\)""", RegexOption.IGNORE_CASE)
            .find(summaryText)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        return CommissionBillPdfResult(
            branchCode = findAfter(cells, "Branch Code:"),
            agentName = findAfter(cells, "Agent Name :").squashSpaces(),
            agentCode = findAfter(cells, "Agent Code :"),
            reportMonth = reportMonth,
            batch = batch,
            processedDate = cells.firstOrNull { it.text.contains("Commission Bill Voucher Processed on", ignoreCase = true) }
                ?.text
                ?.substringAfter("Processed on", "")
                ?.trim()
                .orEmpty(),
            rows = dataRows.sortedWith(compareBy<CommissionBillPdfRow> { it.dueDate.toDateSortKey() }.thenBy { it.serialNumber }),
            warnings = warnings.distinct()
        )
    }

    private fun extractCells(pdfBytes: ByteArray): List<PdfCell> {
        val pdfText = pdfBytes.toString(StandardCharsets.ISO_8859_1)
        val cells = mutableListOf<PdfCell>()
        streamRegex.findAll(pdfText).forEachIndexed { pageIndex, match ->
            val compressed = match.groupValues[1].toByteArray(StandardCharsets.ISO_8859_1)
            val streamText = inflateOrNull(compressed)?.toString(StandardCharsets.ISO_8859_1) ?: return@forEachIndexed
            textRegex.findAll(streamText).forEach { textMatch ->
                val rawText = textMatch.groupValues[3].decodePdfLiteralString().trim()
                if (rawText.isBlank()) return@forEach
                val prefix = streamText.substring(maxOf(0, textMatch.range.first - 500), textMatch.range.first)
                cells += PdfCell(
                    pageIndex = pageIndex,
                    x = textMatch.groupValues[1].toFloatOrNull() ?: 0f,
                    y = textMatch.groupValues[2].toFloatOrNull() ?: 0f,
                    text = rawText,
                    textColor = prefix.lastRgbColor(),
                    fillColor = prefix.lastFilledRectColor()
                )
            }
        }
        return cells
    }

    private fun inflateOrNull(bytes: ByteArray): ByteArray? {
        return try {
            InflaterInputStream(ByteArrayInputStream(bytes)).use { input ->
                val output = ByteArrayOutputStream()
                input.copyTo(output)
                output.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun groupRows(cells: List<PdfCell>): List<List<PdfCell>> {
        return cells
            .groupBy { it.pageIndex }
            .toSortedMap()
            .values
            .flatMap { pageCells ->
                pageCells
                    .sortedWith(compareByDescending<PdfCell> { it.y }.thenBy { it.x })
                    .fold(mutableListOf<MutableList<PdfCell>>()) { grouped, cell ->
                        val existing = grouped.firstOrNull { abs(it.first().y - cell.y) < 0.75f }
                        if (existing == null) grouped += mutableListOf(cell) else existing += cell
                        grouped
                    }
                    .map { row -> row.sortedBy { it.x } }
            }
    }

    private fun List<PdfCell>.columnsByPremiumDueLayout(): Map<DueColumn, PdfCell> {
        return associateBy { cell ->
            when {
                cell.x < 36f -> DueColumn.SERIAL
                cell.x < 82f -> DueColumn.POLICY_NUMBER
                cell.x < 195f -> DueColumn.NAME
                cell.x < 240f -> DueColumn.DOC
                cell.x < 277f -> DueColumn.PLAN_TERM
                cell.x < 300f -> DueColumn.MODE
                cell.x < 337f -> DueColumn.FUP
                cell.x < 354f -> DueColumn.FLAG
                cell.x < 405f -> DueColumn.INSTALLMENT_PREMIUM
                cell.x < 433f -> DueColumn.DUE_COUNT
                cell.x < 479f -> DueColumn.GST
                cell.x < 532f -> DueColumn.TOTAL_PREMIUM
                else -> DueColumn.ESTIMATED_COMMISSION
            }
        }
    }

    private fun List<PdfCell>.columnsByCommissionLayout(): Map<CommissionColumn, PdfCell> {
        return associateBy { cell ->
            when {
                cell.x < 36f -> CommissionColumn.SERIAL
                cell.x < 180f -> CommissionColumn.NAME
                cell.x < 233f -> CommissionColumn.POLICY_NUMBER
                cell.x < 270f -> CommissionColumn.PLAN_TERM
                cell.x < 317f -> CommissionColumn.DUE_DATE
                cell.x < 371f -> CommissionColumn.RISK_DATE
                cell.x < 408f -> CommissionColumn.CBO
                cell.x < 480f -> CommissionColumn.ADJUSTMENT_DATE
                cell.x < 545f -> CommissionColumn.PREMIUM
                else -> CommissionColumn.COMMISSION
            }
        }
    }

    private fun String.lastRgbColor(): PdfRgbColor? {
        val match = colorRegex.findAll(this).lastOrNull() ?: return null
        return match.toRgbColor()
    }

    private fun String.lastFilledRectColor(): PdfRgbColor? {
        val match = filledRectRegex.findAll(this).lastOrNull() ?: return null
        return match.toRgbColor()
    }

    private fun MatchResult.toRgbColor(): PdfRgbColor? {
        return PdfRgbColor(
            red = groupValues.getOrNull(1)?.toFloatOrNull() ?: return null,
            green = groupValues.getOrNull(2)?.toFloatOrNull() ?: return null,
            blue = groupValues.getOrNull(3)?.toFloatOrNull() ?: return null
        )
    }

    private fun findAfter(cells: List<PdfCell>, label: String): String {
        return cells.firstOrNull { it.text.startsWith(label, ignoreCase = true) }
            ?.text
            ?.substringAfter(label)
            ?.trim()
            .orEmpty()
    }

    private fun String?.toAmount(): Double {
        return this
            .orEmpty()
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun String.onlyDigits(): String = filter { it.isDigit() }

    private fun String.squashSpaces(): String = trim().replace(Regex("\\s+"), " ")

    private fun String.toDateSortKey(): String {
        val parts = split("/")
        return if (parts.size == 3) "${parts[2]}${parts[1]}${parts[0]}" else this
    }

    private fun String.decodePdfLiteralString(): String {
        val builder = StringBuilder()
        var index = 0
        while (index < length) {
            val char = this[index]
            if (char == '\\' && index + 1 < length) {
                val next = this[index + 1]
                builder.append(
                    when (next) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'b' -> '\b'
                        'f' -> '\u000c'
                        '(', ')', '\\' -> next
                        else -> next
                    }
                )
                index += 2
            } else {
                builder.append(char)
                index++
            }
        }
        return builder.toString()
    }

    private fun PdfRgbColor.isLapsedRed(): Boolean {
        return red >= 0.65f && green <= 0.45f && blue <= 0.45f
    }

    private data class PdfCell(
        val pageIndex: Int,
        val x: Float,
        val y: Float,
        val text: String,
        val textColor: PdfRgbColor?,
        val fillColor: PdfRgbColor?
    )

    private enum class DueColumn {
        SERIAL,
        POLICY_NUMBER,
        NAME,
        DOC,
        PLAN_TERM,
        MODE,
        FUP,
        FLAG,
        INSTALLMENT_PREMIUM,
        DUE_COUNT,
        GST,
        TOTAL_PREMIUM,
        ESTIMATED_COMMISSION
    }

    private enum class CommissionColumn {
        SERIAL,
        NAME,
        POLICY_NUMBER,
        PLAN_TERM,
        DUE_DATE,
        RISK_DATE,
        CBO,
        ADJUSTMENT_DATE,
        PREMIUM,
        COMMISSION
    }
}
