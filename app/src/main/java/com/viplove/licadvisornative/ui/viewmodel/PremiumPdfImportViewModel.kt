package com.viplove.licadvisornative.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.CommissionBillPdfResult
import com.viplove.licadvisornative.model.LicPremiumPdfParseResult
import com.viplove.licadvisornative.model.PremiumDuePdfResult
import com.viplove.licadvisornative.model.PremiumPdfImportApplyResult
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.util.LicPremiumPdfParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PremiumPdfImportViewModel : ViewModel() {

    data class UiState(
        val isParsing: Boolean = false,
        val isApplying: Boolean = false,
        val parseResult: LicPremiumPdfParseResult? = null,
        val applyResult: PremiumPdfImportApplyResult? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api

    fun parsePdf(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isParsing = true, applyResult = null, error = null)
            }
            val result = LicPremiumPdfParser.parse(uri, contentResolver)
            _uiState.update {
                it.copy(
                    isParsing = false,
                    parseResult = result,
                    error = (result as? LicPremiumPdfParseResult.Error)?.message
                )
            }
        }
    }

    fun applyCurrentImport() {
        val currentResult = _uiState.value.parseResult ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, error = null, applyResult = null) }
            try {
                val response = when (currentResult) {
                    is LicPremiumPdfParseResult.PremiumDueList -> {
                        api.applyPremiumDueImport(currentResult.result)
                    }
                    is LicPremiumPdfParseResult.CommissionBill -> {
                        api.applyCommissionBillImport(currentResult.result)
                    }
                    is LicPremiumPdfParseResult.Error -> {
                        _uiState.update { it.copy(isApplying = false, error = currentResult.message) }
                        return@launch
                    }
                }

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(isApplying = false, applyResult = response.body())
                    }
                } else {
                    _uiState.update {
                        it.copy(isApplying = false, error = response.errorBody()?.string()?.extractMessage() ?: "Could not apply import.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isApplying = false, error = e.localizedMessage ?: "Import failed.")
                }
            }
        }
    }

    fun clear() {
        _uiState.value = UiState()
    }

    val premiumDueResult: PremiumDuePdfResult?
        get() = (_uiState.value.parseResult as? LicPremiumPdfParseResult.PremiumDueList)?.result

    val commissionBillResult: CommissionBillPdfResult?
        get() = (_uiState.value.parseResult as? LicPremiumPdfParseResult.CommissionBill)?.result

    private fun String.extractMessage(): String? {
        return Regex(""""message"\s*:\s*"([^"]+)"""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }
}
