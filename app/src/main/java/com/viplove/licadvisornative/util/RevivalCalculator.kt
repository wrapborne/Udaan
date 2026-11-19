package com.viplove.licadvisornative.util

import android.content.Context
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * LIC revival interest ("late fee") for an overdue premium, worked out for a given day.
 *
 * Factors come from assets/revival_rates.csv (taken from the Smart "All In One Calculator" app),
 * which has LIC's revival interest built in. Replace that file when LIC changes the rate.
 *   Inst            number of unpaid instalments (or months, for Broken)
 *   Yly/Hly/Qly/Mly amount owed for N unpaid instalments, in premiums, at the latest due date
 *   Broken          interest factor for a part period, by whole months (1-12)
 *
 * Rules agreed for Udaan:
 *   - the fee is counted from the first unpaid due date on the DOC schedule; the card shows it once that is more than
 *     45 days ago;
 *   - several unpaid instalments use the mode column, then interest runs on from the latest due
 *     date to today (Broken factor); a single instalment is just the Broken factor;
 *   - part months count as whole months (110 days = 4 months);
 *   - past 5 years from the first unpaid premium the policy can no longer be revived;
 *   - no GST is shown;
 *   - ULIP policies never show a late fee.
 */
class RevivalRates(private val rows: Map<Int, Map<String, Double>>) {
    fun factor(column: String, n: Int): Double? = rows[n]?.get(column)

    companion object {
        const val ASSET_NAME = "revival_rates.csv"

        fun parse(csv: String): RevivalRates {
            val lines = csv.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            if (lines.isEmpty()) return RevivalRates(emptyMap())
            val header = lines.first().split(",").map { it.trim() }
            val rows = mutableMapOf<Int, Map<String, Double>>()
            for (line in lines.drop(1)) {
                val cells = line.split(",")
                val n = cells.getOrNull(0)?.trim()?.toIntOrNull() ?: continue
                val values = mutableMapOf<String, Double>()
                header.forEachIndexed { index, name ->
                    if (index == 0) return@forEachIndexed
                    cells.getOrNull(index)?.trim()?.toDoubleOrNull()?.let { values[name] = it }
                }
                rows[n] = values
            }
            return RevivalRates(rows)
        }

        @Volatile private var cached: RevivalRates? = null

        fun fromAssets(context: Context): RevivalRates =
            cached ?: synchronized(this) {
                cached ?: parse(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }).also { cached = it }
            }
    }
}

sealed class RevivalStatus {
    /** Not overdue, or overdue 45 days or less: show the card as usual. */
    object Normal : RevivalStatus()

    /** Overdue more than 45 days: premium, late fee and the total payable today. */
    data class Late(
        val daysOverdue: Int,
        val firstUnpaidDue: Long,
        val instalmentsDue: Int,
        val instalmentPremium: Double,
        val premiumTotal: Long,
        val lateFee: Long,
        val payable: Long,
    ) : RevivalStatus()

    /** More than 5 years since the first unpaid premium: revival is no longer possible. */
    data class RevivalPeriodOver(val daysOverdue: Int, val firstUnpaidDue: Long) : RevivalStatus()
}

object RevivalCalculator {
    const val LATE_AFTER_DAYS = 45
    const val REVIVAL_YEARS = 5
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun modeMonths(mode: String): Int? = when (mode.uppercase().replace(" ", "").replace("-", "")) {
        "YLY", "YEARLY" -> 12
        "HLY", "HALFYEARLY" -> 6
        "QLY", "QTLY", "QUARTERLY" -> 3
        "MLY", "MNLY", "MONTHLY" -> 1
        else -> null
    }

    private fun column(months: Int) = when (months) {
        12 -> "Yly"
        6 -> "Hly"
        3 -> "Qly"
        else -> "Mly"
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Whole calendar days from `from` to `to` (both taken at the start of their day). */
    fun daysBetween(from: Long, to: Long): Int {
        val a = startOfDay(from)
        val b = startOfDay(to)
        // Adding the zone offsets keeps a daylight-saving change from losing a day.
        val tz = TimeZone.getDefault()
        return (((b + tz.getOffset(b)) - (a + tz.getOffset(a))) / DAY_MS).toInt()
    }

    /** Due date number k of the schedule that starts at `start` (k = 0 is `start` itself). */
    private fun scheduleDate(start: Long, k: Int, months: Int): Long =
        Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, k * months) }.timeInMillis

    /**
     * First unpaid premium (LIC's FUP). Premiums fall due on the DOC schedule, so the recorded last-paid
     * date is snapped back to the schedule date on or before it: for a backdated policy the first
     * premium can show a later date (e.g. DOC 28-07-2024, "last paid" 02-09-2024 = the 28-07-2024
     * premium). The next schedule date is the first unpaid one. Without a DOC, last paid + one period
     * is used. Null when it cannot be known.
     */
    fun firstUnpaidDue(lastPremiumPaidDate: Long?, mode: String, doc: Long = 0L): Long? {
        val months = modeMonths(mode) ?: return null
        if (lastPremiumPaidDate == null || lastPremiumPaidDate <= 0L) return null
        if (doc <= 0L) return scheduleDate(lastPremiumPaidDate, 1, months)
        val paidDay = startOfDay(lastPremiumPaidDate)
        var k = 0
        while (startOfDay(scheduleDate(doc, k + 1, months)) <= paidDay) k += 1
        return scheduleDate(doc, k + 1, months)
    }

    fun status(
        rates: RevivalRates,
        premium: Double,
        mode: String,
        lastPremiumPaidDate: Long?,
        today: Long = System.currentTimeMillis(),
        isUlip: Boolean = false,
        doc: Long = 0L,
    ): RevivalStatus {
        // ULIP policies have no late fee.
        if (isUlip) return RevivalStatus.Normal
        val months = modeMonths(mode) ?: return RevivalStatus.Normal
        if (premium <= 0.0) return RevivalStatus.Normal
        val firstDue = firstUnpaidDue(lastPremiumPaidDate, mode, doc) ?: return RevivalStatus.Normal
        val daysOverdue = daysBetween(firstDue, today)
        if (daysOverdue <= LATE_AFTER_DAYS) return RevivalStatus.Normal

        val revivalEnd = Calendar.getInstance().apply { timeInMillis = firstDue; add(Calendar.YEAR, REVIVAL_YEARS) }.timeInMillis
        if (startOfDay(today) > startOfDay(revivalEnd)) return RevivalStatus.RevivalPeriodOver(daysOverdue, firstDue)

        // Unpaid instalments: every due date from the first unpaid one up to today.
        // Each due date is counted from the first one (not added step by step), so a 31st stays on the 31st.
        var instalments = 0
        var latestDue = firstDue
        while (startOfDay(scheduleDate(firstDue, instalments, months)) <= startOfDay(today)) {
            latestDue = scheduleDate(firstDue, instalments, months)
            instalments += 1
        }

        val base = if (instalments <= 1) 1.0 else rates.factor(column(months), instalments) ?: return RevivalStatus.Normal
        val monthsSinceLatest = ceil(daysBetween(latestDue, today) / 30.0).toInt().coerceIn(0, 12)
        val broken = if (monthsSinceLatest == 0) 1.0 else rates.factor("Broken", monthsSinceLatest) ?: return RevivalStatus.Normal

        val premiumTotal = (premium * instalments).roundToLong()
        val payable = (premium * base * broken).roundToLong()
        return RevivalStatus.Late(
            daysOverdue = daysOverdue,
            firstUnpaidDue = firstDue,
            instalmentsDue = instalments,
            instalmentPremium = premium,
            premiumTotal = premiumTotal,
            lateFee = (payable - premiumTotal).coerceAtLeast(0),
            payable = payable,
        )
    }
}
