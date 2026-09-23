package com.viplove.licadvisornative.util

/**
 * Single source of truth for which LIC plan numbers are ULIP plans.
 * Used by FirebaseApi to derive `isUlip` when the Firestore document
 * doesn't explicitly carry the flag (e.g. legacy uploads).
 *
 * Keep in sync with the Superadmin "Manage ULIP plan numbers" defaults
 * (UlipPlanViewModel.defaultPlanNumbers).
 */
object UlipPlanDetector {

    val DEFAULT_ULIP_PLAN_NUMBERS: Set<String> = setOf(
        "735", // New Endowment Plus
        "749", // Nivesh Plus
        "752", // SIIP
        "867", // New Pension Plus
        "873", // Index Plus
        "886"  // (per project requirement)
    )

    fun isUlipPlan(planNumber: String?): Boolean {
        if (planNumber.isNullOrBlank()) return false
        return planNumber.trim() in DEFAULT_ULIP_PLAN_NUMBERS
    }
}
