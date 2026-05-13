// File: app/src/main/java/com/viplove/licadvisornative/domain/FilterPoliciesUseCase.kt
package com.viplove.licadvisornative.domain

import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.User
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

/**
 * A Use Case class dedicated to handling the business logic of filtering policies.
 * This centralizes the filtering rules, making them reusable and easier to test.
 */
class FilterPoliciesUseCase {

    /**
     * Data class to hold all possible filter parameters, making the input explicit.
     */
    data class Params(
        val allPolicies: List<Policy>,
        val searchQuery: String,
        val selectedPlan: String,
        val selectedMode: String,
        val startDate: Long?,
        val endDate: Long?,
        val selectedFinancialYear: String,
        val selectedAppraisalYear: String,
        val showOnlyLate: Boolean = false,
        val user: User? // The user (agent or admin) for appraisal year context
    )

    /**
     * Executes the filtering logic.
     * @param params The filtering parameters.
     * @return A new list of policies that match the filter criteria.
     */
    fun execute(params: Params): List<Policy> {
        val userStartDate = params.user?.startDate

        return params.allPolicies.filter { policy ->
            // Match against search query (policy number, plan, or name)
            val searchMatch = policy.policyNumber.contains(params.searchQuery, ignoreCase = true) ||
                    policy.plan.contains(params.searchQuery, ignoreCase = true) ||
                    policy.shortName.contains(params.searchQuery, ignoreCase = true)

            // Match against selected plan
            val planMatch = params.selectedPlan == "All" || policy.plan == params.selectedPlan

            // Match against selected mode (including special case for ANANDA)
            val modeMatch = when (params.selectedMode) {
                "All" -> true
                "ANANDA" -> policy.isAnanda
                else -> policy.mode == params.selectedMode
            }

            // Determine the final date range based on user selections
            var finalStart: Long? = params.startDate
            var finalEnd: Long? = params.endDate?.plus(86400000) // Include the full end day

            // Override date range if an appraisal year is selected
            if (params.selectedAppraisalYear != "None") {
                val appraisalBounds = parseAppraisalYearString(params.selectedAppraisalYear, userStartDate)
                finalStart = appraisalBounds.first
                finalEnd = appraisalBounds.second
            }

            // Further refine date range if a financial year is also selected
            if (params.selectedFinancialYear != "None") {
                val financialBounds = parseFinancialYearString(params.selectedFinancialYear)
                if (finalStart != null && finalEnd != null && financialBounds.first != null && financialBounds.second != null) {
                    // Find the intersection of the two date ranges
                    finalStart = max(finalStart, financialBounds.first!!)
                    finalEnd = min(finalEnd, financialBounds.second!!)
                } else {
                    finalStart = financialBounds.first
                    finalEnd = financialBounds.second
                }
            }

            // Check if the policy's date of commencement falls within the final date range
            val policyDocValue = policy.doc
            val startDateMatch = finalStart == null || policyDocValue >= finalStart
            val endDateMatch = finalEnd == null || policyDocValue < finalEnd

            // Match against late policy filter
            val lateMatch = if (params.showOnlyLate) {
                com.viplove.licadvisornative.util.PolicyFilter.isPolicyLate(policy)
            } else {
                true
            }

            // A policy is included if it matches all criteria
            searchMatch && planMatch && modeMatch && startDateMatch && endDateMatch && lateMatch
        }
    }

    // --- Private Helper Functions for Date Parsing ---

    private fun parseFinancialYearString(yearString: String): Pair<Long?, Long?> {
        return try {
            val years = yearString.removePrefix("Financial Year ").split(" - ")
            val startYear = years[0].toInt()
            val endYear = years[1].toInt()
            val cal = Calendar.getInstance()
            cal.set(startYear, Calendar.APRIL, 1, 0, 0, 0)
            val start = cal.timeInMillis
            cal.set(endYear, Calendar.MARCH, 31, 23, 59, 59)
            val end = cal.timeInMillis
            Pair(start, end)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private fun parseAppraisalYearString(yearString: String, userStartDate: Long?): Pair<Long?, Long?> {
        return try {
            val years = yearString.removePrefix("Appraisal ").split(" - ")
            val startYear = years[0].toInt()
            val baseStartDate = userStartDate ?: return Pair(null, null)
            val startCal = Calendar.getInstance().apply { timeInMillis = baseStartDate; set(Calendar.YEAR, startYear) }
            val endCal = (startCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
            Pair(startCal.timeInMillis, endCal.timeInMillis)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
}
