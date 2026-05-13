// File: app/src/main/java/com/viplove/licadvisornative/model/ChartData.kt
package com.viplove.licadvisornative.model
import com.viplove.licadvisornative.model.PlanData
/**
 * Data class to represent a single age-based limit for Sum Assured.
 * Used for parsing the JSON from Firebase Remote Config.
 */
data class AgeLimit(
    val age_range: List<Int> = emptyList(),
    val max_suc: Int = 0
)

/**
 * Data class that represents the entire structure of the non-medical rules JSON
 * fetched from Firebase Remote Config.
 */
data class ChartData(
    val grp_1_plans: List<String> = emptyList(),
    val grp_2_plans: List<String> = emptyList(),
    val limits: Map<String, Map<String, List<AgeLimit>>> = emptyMap(),
    val plan_details_list: List<PlanData> = emptyList()
)
