
// File: app/src/main/java/com/viplove/licadvisornative/util/PolicyFilter.kt
package com.viplove.licadvisornative.util

import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.ui.viewmodel.AdminViewModel
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

object PolicyFilter {

    fun getFilteredPolicies(allPolicies: List<Policy>, filters: AdminViewModel.ProposalsUiState): List<Policy> {
        return allPolicies.asSequence()
            .filter { policy -> filterBySearchQuery(policy, filters.searchQuery) }
            .filter { policy -> filterByPlan(policy, filters.selectedPlan) }
            .filter { policy -> filterByMode(policy, filters.selectedMode) }
            .filter { policy -> filterByDateRange(policy, filters.startDate, filters.endDate) }
            .filter { policy -> filterByLateStatus(policy, filters.showOnlyLate) }
            .toList()
    }

    private fun filterBySearchQuery(policy: Policy, query: String): Boolean {
        if (query.isBlank()) return true
        val queryLower = query.lowercase()
        return policy.shortName.lowercase().contains(queryLower) ||
                policy.policyNumber.contains(queryLower) ||
                policy.plan.lowercase().contains(queryLower)
    }

    private fun filterByPlan(policy: Policy, selectedPlan: String): Boolean {
        if (selectedPlan == "All") return true
        return policy.plan == selectedPlan
    }

    private fun filterByMode(policy: Policy, selectedMode: String): Boolean {
        if (selectedMode == "All") return true
        if (selectedMode == "ANANDA") return policy.isAnanda
        return policy.mode == selectedMode
    }

    /**
     * CORRECTED: This function now filters using `dateOfCompletion` (proposal registration date)
     * instead of `doc` (date of commencement).
     */
    private fun filterByDateRange(policy: Policy, startDate: Long?, endDate: Long?): Boolean {
        if (startDate == null || endDate == null) return true
        // Use the dateOfCompletion field for filtering.
        // Add a null check to safely handle policies that might not have this date.
        val proposalDate = policy.dateOfCompletion ?: return false
        return proposalDate in startDate until endDate
    }

    private fun filterByLateStatus(policy: Policy, showOnlyLate: Boolean): Boolean {
        if (!showOnlyLate) return true
        return isPolicyLate(policy)
    }

    /**
     * Check if a policy is late (due date is more than 30 days overdue but less than 1 year)
     * Policies more than 1 year overdue are considered lapsed, not late
     */
    fun isPolicyLate(policy: Policy): Boolean {
        // Determine the base date for calculating the next due date
        // Priority: 1) Last Premium Paid Date, 2) ENACH date, 3) DOC
        val baseTimestamp = if (policy.lastPremiumPaidDate != null && policy.lastPremiumPaidDate!! > 0) {
            policy.lastPremiumPaidDate!!
        } else if (policy.enachDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                dateFormat.parse(policy.enachDate)?.time ?: policy.doc
            } catch (e: Exception) {
                policy.doc
            }
        } else {
            policy.doc
        }
        
        if (baseTimestamp == 0L) return false

        val now = Calendar.getInstance()
        val nextDueCal = Calendar.getInstance().apply { timeInMillis = baseTimestamp }

        val monthInterval = when (policy.mode.uppercase(Locale.ROOT)) {
            "YLY", "YEARLY" -> 12
            "HLY", "HALFYEARLY" -> 6
            "QLY", "QUARTERLY" -> 3
            "MLY", "MONTHLY" -> 1
            else -> return false
        }

        // If we have a last premium paid date, the next due is one interval after that
        if (policy.lastPremiumPaidDate != null && policy.lastPremiumPaidDate!! > 0) {
            nextDueCal.add(Calendar.MONTH, monthInterval)
        } else {
            // Calculate the next upcoming due date from the base date
            while (nextDueCal.before(now)) {
                nextDueCal.add(Calendar.MONTH, monthInterval)
            }
        }
        
        // Check if today is more than 30 days AFTER the next due date
        // BUT less than 365 days (1 year) - policies >1 year overdue are considered lapsed
        val daysDifference = ((now.timeInMillis - nextDueCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        return daysDifference > 30 && daysDifference <= 365
    }


    fun generatePlanOptions(allPolicies: List<Policy>): List<String> {
        return listOf("All") + allPolicies.map { it.plan }.distinct().sorted()
    }

    fun generateModeOptions(allPolicies: List<Policy>): List<String> {
        val modes = allPolicies.map { it.mode }.distinct().toMutableSet()
        if (allPolicies.any { it.isAnanda }) {
            modes.add("ANANDA")
        }
        return listOf("All") + modes.sorted()
    }

    fun getAppraisalYearBounds(yearString: String, adminStartDate: Long): Pair<Long, Long>? {
        val startYear = yearString.substringAfter(" ").substringBefore(" ").toIntOrNull() ?: return null
        val baseStartCal = Calendar.getInstance().apply { timeInMillis = adminStartDate }
        val appraisalStartMonth = (baseStartCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.get(Calendar.MONTH)
        val periodStartCal = Calendar.getInstance().apply {
            clear()
            set(startYear, appraisalStartMonth, 1)
        }
        val periodEndCal = (periodStartCal.clone() as Calendar).apply {
            add(Calendar.YEAR, 1)
        }
        return Pair(periodStartCal.timeInMillis, periodEndCal.timeInMillis)
    }

    fun generateFinancialYearOptions(): List<String> {
        val years = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        var currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        if (currentMonth < Calendar.APRIL) {
            currentYear--
        }
        for (i in 0..10) {
            val startYear = currentYear - i
            val endYear = startYear + 1
            years.add("Financial Year $startYear - $endYear")
        }
        return years
    }

    fun generateAppraisalYearOptions(startDateMillis: Long?): List<String> {
        if (startDateMillis == null) return emptyList()
        val options = mutableListOf<String>()
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val firstAppraisalStartCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val today = Calendar.getInstance()
        var loopCal = firstAppraisalStartCal.clone() as Calendar
        while (loopCal.before(today) || loopCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            val startYear = loopCal.get(Calendar.YEAR)
            val endYear = startYear + 1
            options.add("Appraisal $startYear - $endYear")
            loopCal.add(Calendar.YEAR, 1)
            if (options.size > 15) break // Safety break
        }
        return options.reversed()
    }

    fun getFinancialYearBounds(yearString: String): Pair<Long, Long>? {
        val startYear = yearString.split(" ").getOrNull(2)?.toIntOrNull() ?: return null
        val endYear = startYear + 1
        val periodStartCal = Calendar.getInstance().apply {
            clear()
            set(startYear, Calendar.APRIL, 1)
        }
        val periodEndCal = Calendar.getInstance().apply {
            clear()
            set(endYear, Calendar.APRIL, 1)
        }
        return Pair(periodStartCal.timeInMillis, periodEndCal.timeInMillis)
    }

    fun getCurrentAppraisalYearString(startDateMillis: Long?): String {
        if (startDateMillis == null) return "None"
        val today = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val appraisalStartMonth = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.get(Calendar.MONTH)
        var currentAppraisalStartYear = today.get(Calendar.YEAR)
        if (today.get(Calendar.MONTH) < appraisalStartMonth) {
            currentAppraisalStartYear--
        }
        val startYear = currentAppraisalStartYear
        val endYear = startYear + 1
        return "Appraisal $startYear - $endYear"
    }

    fun getCurrentAppraisalYearBounds(startDateMillis: Long?): Pair<Long?, Long?> {
        if (startDateMillis == null) return Pair(null, null)

        val today = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }

        val appraisalStartMonth = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.get(Calendar.MONTH)

        var currentAppraisalStartYear = today.get(Calendar.YEAR)
        if (today.get(Calendar.MONTH) < appraisalStartMonth) {
            currentAppraisalStartYear--
        }

        val periodStartCal = Calendar.getInstance().apply {
            clear()
            set(currentAppraisalStartYear, appraisalStartMonth, 1)
        }

        val periodEndCal = (periodStartCal.clone() as Calendar).apply {
            add(Calendar.YEAR, 1)
        }

        return Pair(periodStartCal.timeInMillis, periodEndCal.timeInMillis)
    }
}
