// File: app/src/main/java/com/viplove/licadvisornative/util/NonMedicalCheckerLogic.kt
package com.viplove.licadvisornative.util

import com.viplove.licadvisornative.model.ChartData

data class CheckerInput(
    val age: Int,
    val income: Int,
    val qualification: String,
    val profession: String,
    val isResidentIndian: Boolean,
    val isMinor: Boolean,
    val isStudent: Boolean,
    val planNumber: String
)

object NonMedicalCheckerLogic {

    fun getAvailablePlans(): List<String> {
        // FIX: Access the .value of the StateFlow instead of calling the old function
        val chartData = RemoteConfigManager.chartDataState.value ?: return emptyList()
        return (chartData.grp_1_plans + chartData.grp_2_plans).distinct().sorted()
    }

    fun determineCategory(data: CheckerInput): String {
        val q = data.qualification.lowercase()
        val p = data.profession.lowercase()

        // Initial checks
        if (data.isMinor) return if (data.age <= 17) "N" else "Ineligible"
        if (data.isStudent) {
            val validQualifications = listOf("hsc / plus 2", "graduate", "post graduate", "professional")
            return if (q in validQualifications && data.age <= 30) "X" else "Ineligible"
        }
        if (!data.isResidentIndian || data.income <= 0) return "Ineligible"

        // PNM Logic
        val hasDiplomaOrDegree = q in listOf("graduate", "post graduate", "professional", "diploma")
        if (hasDiplomaOrDegree && data.income >= 10_00_000) {
            if (p == "employed" || p == "self employed" || p == "business") {
                return "PNM"
            }
        }

        // SNM Logic
        if (p == "employed" && data.income >= 2_50_000) return "SNM"
        if ((p == "self employed" || p == "business") && q.contains("ssc") && data.income >= 10_00_000) return "SNM"
        if (p == "professionals" && data.income >= 2_50_000) return "SNM"

        // NMG Logic (Fallback)
        if (data.income > 0) return "NMG"

        return "Ineligible"
    }

    fun getSaLimits(category: String, age: Int, planNumber: String): Int {
        // FIX: Access the .value of the StateFlow instead of calling the old function
        val chartData = RemoteConfigManager.chartDataState.value ?: return 0

        val planGroup = if (planNumber in chartData.grp_1_plans) "Grp I" else "Grp II"
        val limits = chartData.limits[category]?.get(planGroup) ?: return 0

        for (entry in limits) {
            val (ageFrom, ageTo) = entry.age_range
            if (age in ageFrom..ageTo) {
                return entry.max_suc
            }
        }
        return 0
    }
}