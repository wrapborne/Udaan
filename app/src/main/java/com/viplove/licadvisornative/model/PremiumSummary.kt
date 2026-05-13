package com.viplove.licadvisornative.model

data class PremiumSummary(
    var summaryId: String = "",
    var reportMonth: String = "",
    var fpSchPrem: Double = 0.0,
    var fySchPrem: Double = 0.0,
    var adminId: String = "",
    var agencyCode: String = ""
) {
    val totalScheduledPremium: Double
        get() = fpSchPrem + fySchPrem
}
