package com.viplove.licadvisornative.util

import com.viplove.licadvisornative.model.PremiumSummary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A simple data structure to hold the extracted payment info before updating the database.
 * The dueDate is used to find the latest payment if multiple are listed for the same policy.
 */
data class PolicyPaymentInfo(
    val policyNumber: String,
    val dateOfCollection: Long,
    val dueDate: Long
)

object PremiumSummaryParser {

    /**
     * This is the original function. It remains unchanged and will continue to parse
     * the agent's total premium summary.
     */
    fun parse(text: String): List<PremiumSummary> {
        return if ("TOTAL FOR AGENT" in text.uppercase()) {
            parsePdfFormat(text)
        } else {
            parseTxtFormat(text)
        }
    }

    /**
     * --- NEW FUNCTION ---
     * This function is specifically designed to parse the detailed report that lists
     * individual paid policies. It correctly handles multiple payments for the same policy
     * by selecting the one with the latest due date.
     *
     * @param text The full text content from the detailed premium payment file.
     * @return A list of [PolicyPaymentInfo] objects, each containing a policy number
     * and the timestamp of its most recent premium payment.
     */
    fun parsePolicyPaymentDetails(text: String): List<PolicyPaymentInfo> {
        val allPaymentEntries = mutableListOf<PolicyPaymentInfo>()
        // Regex to find lines with a policy number, collection date, and due date.
        val lineRegex = Regex("""^(\d{9})\s+([\d-]+)\s+([\d/]+)\s+.*""")

        val collDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dueDateFormat = SimpleDateFormat("M/yyyy", Locale.getDefault())

        text.lines().forEach { line ->
            lineRegex.find(line.trim())?.let { matchResult ->
                val policyNumber = matchResult.groups[1]?.value
                val collDateStr = matchResult.groups[2]?.value
                val dueDateStr = matchResult.groups[3]?.value

                if (policyNumber != null && collDateStr != null && dueDateStr != null) {
                    try {
                        val collTimestamp = collDateFormat.parse(collDateStr)?.time ?: 0L
                        val dueTimestamp = dueDateFormat.parse(dueDateStr)?.time ?: 0L

                        if (collTimestamp > 0L && dueTimestamp > 0L) {
                            allPaymentEntries.add(
                                PolicyPaymentInfo(
                                    policyNumber = policyNumber,
                                    dateOfCollection = collTimestamp,
                                    dueDate = dueTimestamp
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // Ignore lines with unparseable dates
                    }
                }
            }
        }

        // After parsing all lines, group by policy number and find the entry
        // with the most recent due date. This handles cases where multiple
        // back-dated premiums are paid at once.
        return allPaymentEntries
            .groupBy { it.policyNumber }
            .mapNotNull { (_, entries) ->
                entries.maxByOrNull { it.dueDate }
            }
    }


    private fun parsePdfFormat(text: String): List<PremiumSummary> {
        val summaries = mutableListOf<PremiumSummary>()

        val monthRegex = Regex("""FOR THE MONTH OF (\d{2}/\d{4})""")
        val reportMonth = monthRegex.find(text)?.groups?.get(1)?.value ?: "Unknown"

        val pattern = Regex(
            """TOTAL FOR AGENT\s*:\s*(\w+).*?PREMIUM\s*:\s*([\d.]+).*?FP Sch\.Prem\s*:\s*([\d.]+).*?FY Sch\.Prem\s*:\s*([\d.]+)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val matches = pattern.findAll(text)
        for (match in matches) {
            val agencyCode = match.groups[1]?.value?.trim()?.uppercase() ?: continue
            val fpSchPremStr = match.groups[3]?.value ?: "0.0"
            val fySchPremStr = match.groups[4]?.value ?: "0.0"

            summaries.add(
                PremiumSummary(
                    reportMonth = reportMonth,
                    fpSchPrem = fpSchPremStr.toDoubleOrNull() ?: 0.0,
                    fySchPrem = fySchPremStr.toDoubleOrNull() ?: 0.0,
                    agencyCode = agencyCode
                )
            )
        }
        return summaries
    }

    private fun parseTxtFormat(text: String): List<PremiumSummary> {
        val summaries = mutableListOf<PremiumSummary>()

        val lineRegex = Regex("""(\d{6})\s+(\w+)\s+([\d.]+)\s+([\d.]+)""")

        for (line in text.lineSequence()) {
            val match = lineRegex.find(line) ?: continue

            val month = match.groups[1]?.value ?: continue  // e.g. 102024
            val agencyCode = match.groups[2]?.value?.trim()?.uppercase() ?: continue
            val eligiblePremium = match.groups[4]?.value?.toDoubleOrNull() ?: 0.0

            val formattedMonth = "${month.substring(0, 2)}/${month.substring(2)}"  // MM/YYYY

            summaries.add(
                PremiumSummary(
                    reportMonth = formattedMonth,
                    fpSchPrem = eligiblePremium,
                    fySchPrem = 0.0,
                    agencyCode = agencyCode
                )
            )
        }

        return summaries
    }
}

