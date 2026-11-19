package com.viplove.licadvisornative.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar

class RevivalCalculatorTest {
    private val rates = RevivalRates.parse(File("src/main/assets/revival_rates.csv").readText())

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month - 1, day, 10, 0)
    }.timeInMillis

    @Test
    fun yearlyExampleFromSpec() {
        // Last paid 1 Aug 2025 -> due 1 Aug 2026, today 19 Nov 2026: 110 days, 4 months -> 1.03167.
        val status = RevivalCalculator.status(rates, 25000.0, "Yearly", date(2025, 8, 1), date(2026, 11, 19)) as RevivalStatus.Late
        assertEquals(110, status.daysOverdue)
        assertEquals(1, status.instalmentsDue)
        assertEquals(25000L, status.premiumTotal)
        assertEquals(792L, status.lateFee)
        assertEquals(25792L, status.payable)
    }

    @Test
    fun monthlyWithSeveralUnpaidInstalments() {
        // Due 1 Aug, Sep, Oct, Nov 2026 unpaid: Mly[4] 4.04683, then 18 days (1 month) -> Broken 1.00792.
        val status = RevivalCalculator.status(rates, 2404.0, "MLY", date(2026, 7, 1), date(2026, 11, 19)) as RevivalStatus.Late
        assertEquals(4, status.instalmentsDue)
        assertEquals(9616L, status.premiumTotal)
        assertEquals(9806L, status.payable)
        assertEquals(190L, status.lateFee)
    }

    @Test
    fun yearlyTwoInstalmentsUsesModeColumnThenBrokenMonths() {
        // Due 1 Aug 2025 and 1 Aug 2026; today 19 Nov 2026 -> Yly[2] 2.09726 x Broken[4] 1.03167.
        val status = RevivalCalculator.status(rates, 10000.0, "YLY", date(2024, 8, 1), date(2026, 11, 19)) as RevivalStatus.Late
        assertEquals(2, status.instalmentsDue)
        assertEquals(Math.round(10000 * 2.09726 * 1.03167), status.payable)
        assertEquals(status.payable - 20000, status.lateFee)
    }

    @Test
    fun noLateFeeUpTo45Days() {
        val due = date(2026, 10, 1) // last paid 1 Oct 2025, yearly
        assertTrue(RevivalCalculator.status(rates, 5000.0, "Yearly", date(2025, 10, 1), date(2026, 11, 15)) is RevivalStatus.Normal) // 45 days
        assertTrue(RevivalCalculator.status(rates, 5000.0, "Yearly", date(2025, 10, 1), date(2026, 11, 16)) is RevivalStatus.Late) // 46 days
        assertEquals(46, RevivalCalculator.daysBetween(due, date(2026, 11, 16)))
    }

    @Test
    fun revivalPeriodOverAfterFiveYears() {
        val status = RevivalCalculator.status(rates, 5000.0, "Yearly", date(2020, 6, 1), date(2026, 11, 19))
        assertTrue(status is RevivalStatus.RevivalPeriodOver)
    }

    @Test
    fun unknownLastPaidOrModeShowsNormalCard() {
        assertTrue(RevivalCalculator.status(rates, 5000.0, "Yearly", null, date(2026, 11, 19)) is RevivalStatus.Normal)
        assertTrue(RevivalCalculator.status(rates, 5000.0, "Single", date(2024, 1, 1), date(2026, 11, 19)) is RevivalStatus.Normal)
    }

    // Real LIC revival quotations (19 Nov 2026), revival campaign rebate left out.
    @Test
    fun matchesLicQuoteBackdatedYearlyMegha() {
        // DOC 28-07-2024, recorded last paid 02-09-2024; LIC: FUP 2025-07-28, 2 YLY, late fee 1898.
        val status = RevivalCalculator.status(rates, 11595.0, "YLY", date(2024, 9, 2), date(2026, 11, 19), doc = date(2024, 7, 28)) as RevivalStatus.Late
        assertEquals(date(2025, 7, 28), status.firstUnpaidDue)
        assertEquals(2, status.instalmentsDue)
        assertEquals(23190L, status.premiumTotal)
        assertEquals(1898L, status.lateFee)
        assertEquals(479, status.daysOverdue)
    }

    @Test
    fun matchesLicQuoteBackdatedYearlyRishipal() {
        // DOC 28-05-2024; LIC: FUP 2025-05-28, 2 YLY of 12855, late fee 2531.
        val status = RevivalCalculator.status(rates, 12855.0, "YLY", date(2024, 9, 2), date(2026, 11, 19), doc = date(2024, 5, 28)) as RevivalStatus.Late
        assertEquals(25710L, status.premiumTotal)
        assertEquals(2531L, status.lateFee)
    }

    @Test
    fun matchesLicQuoteQuarterlyNargish() {
        // DOC 20-01-2026, last paid 20-01-2026; LIC: FUP 2026-04-20, 3 QLY of 2595, late fee 247.
        val status = RevivalCalculator.status(rates, 2595.0, "QLY", date(2026, 1, 20), date(2026, 11, 19), doc = date(2026, 1, 20)) as RevivalStatus.Late
        assertEquals(3, status.instalmentsDue)
        assertEquals(7785L, status.premiumTotal)
        assertEquals(247L, status.lateFee)
    }

    @Test
    fun monthEndDocStaysOnScheduleDay() {
        // DOC 31-01-2025 monthly, last paid 31-01-2025 -> first unpaid 28-02-2025, then 31-03-2025.
        assertEquals(date(2025, 2, 28), RevivalCalculator.firstUnpaidDue(date(2025, 1, 31), "MLY", date(2025, 1, 31)))
        assertEquals(date(2025, 3, 31), RevivalCalculator.firstUnpaidDue(date(2025, 2, 28), "MLY", date(2025, 1, 31)))
    }

    @Test
    fun ulipPoliciesNeverShowALateFee() {
        val status = RevivalCalculator.status(rates, 25000.0, "Yearly", date(2025, 8, 1), date(2026, 11, 19), isUlip = true)
        assertTrue(status is RevivalStatus.Normal)
    }

    @Test
    fun ratesFileHasEveryColumn() {
        assertEquals(1.00792, rates.factor("Broken", 1)!!, 1e-9)
        assertEquals(1.09726, rates.factor("Broken", 12)!!, 1e-9)
        assertEquals(7.66236, rates.factor("Yly", 6)!!, 1e-9)
        assertEquals(95.97836, rates.factor("Mly", 72)!!, 1e-9)
    }
}
