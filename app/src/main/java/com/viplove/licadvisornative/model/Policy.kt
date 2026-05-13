package com.viplove.licadvisornative.model

/**
 * Main data model for a single policy.
 * This now includes all fields for both regular and ULIP policies.
 */
data class Policy(
    var policyId: String = "",
    var policyNumber: String = "",
    var plan: String = "",
    var mode: String = "",
    var doc: Long = 0L, // Date of Commencement
    var dateOfCompletion: Long = 0L, // Use Long, not Long?
    var isAnanda: Boolean = false,
    var premium: Double = 0.0,
    var agentCode: String = "",
    var adminId: String = "",
    var shortName: String = "",
    var enachDate: String = "",
    var agentName: String = "",
    var lastPremiumPaidDate: Long? = null,

    // --- THIS IS THE NEW FIELD THAT SORTS EVERYTHING ---
    var isUlip: Boolean = false,

    // Registration/Upload timestamp (when policy was added to system)
    var createdAt: Long = System.currentTimeMillis()
)