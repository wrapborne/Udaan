package com.viplove.licadvisornative.model

data class PremiumPdfImportApplyResult(
    val importedRows: Int = 0,
    val updatedPolicies: Int = 0,
    val clearedDueItems: Int = 0,
    val reconciledDueItems: Int = 0,
    val reversalRows: Int = 0,
    val warnings: List<String> = emptyList()
)
