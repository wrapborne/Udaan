package com.viplove.licadvisornative.model

enum class LicPremiumPdfType {
    PREMIUM_DUE_LIST,
    COMMISSION_BILL,
    UNKNOWN
}

enum class PremiumYearType {
    FIRST_YEAR,
    RENEWAL,
    UNKNOWN
}

enum class CommissionRowStatus {
    PAID,
    COOLING_OFF_REVERSAL
}

data class PdfRgbColor(
    val red: Float,
    val green: Float,
    val blue: Float
)

data class PremiumDuePdfResult(
    val branchCode: String = "",
    val agentName: String = "",
    val agentCode: String = "",
    val reportMonth: String = "",
    val rows: List<PremiumDuePdfRow> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class PremiumDuePdfRow(
    val serialNumber: Int,
    val policyNumber: String,
    val policyHolderName: String,
    val dateOfCommencement: String,
    val planTerm: String,
    val mode: String,
    val fupMonth: String,
    val premiumYearType: PremiumYearType,
    val isLapsed: Boolean,
    val installmentPremium: Double,
    val dueCount: Int,
    val gst: Double,
    val totalPremium: Double,
    val estimatedCommission: Double,
    val rowFillColors: List<PdfRgbColor> = emptyList()
) {
    val dueKey: String
        get() = "$policyNumber|$fupMonth"
}

data class CommissionBillPdfResult(
    val branchCode: String = "",
    val agentName: String = "",
    val agentCode: String = "",
    val reportMonth: String = "",
    val batch: String = "",
    val processedDate: String = "",
    val rows: List<CommissionBillPdfRow> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class CommissionBillPdfRow(
    val serialNumber: Int,
    val policyHolderName: String,
    val policyNumber: String,
    val planTerm: String,
    val dueDate: String,
    val riskDate: String,
    val cbo: String,
    val adjustmentDate: String,
    val premium: Double,
    val commission: Double,
    val status: CommissionRowStatus
) {
    val paymentKey: String
        get() = "$policyNumber|$dueDate"
}

sealed class LicPremiumPdfParseResult {
    data class PremiumDueList(val result: PremiumDuePdfResult) : LicPremiumPdfParseResult()
    data class CommissionBill(val result: CommissionBillPdfResult) : LicPremiumPdfParseResult()
    data class Error(val message: String) : LicPremiumPdfParseResult()
}
