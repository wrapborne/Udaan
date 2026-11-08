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

    /**
     * Screen state. [phase] is the single source of truth; the other properties are derived
     * from it so the screen can't observe impossible combinations (e.g. parsing while applying).
     */
    data class UiState(
        val phase: PremiumPdfImportPhase = PremiumPdfImportPhase.Idle
    ) {
        val isParsing: Boolean get() = phase is PremiumPdfImportPhase.Parsing
        val isApplying: Boolean get() = phase is PremiumPdfImportPhase.Applying
        val canApply: Boolean get() = phase.startApplying() != null
        val parseResult: LicPremiumPdfParseResult?
            get() = (phase as? PremiumPdfImportPhase.HasParsed)?.parsed
        val applyResult: PremiumPdfImportApplyResult?
            get() = (phase as? PremiumPdfImportPhase.Applied)?.result
        val error: String?
            get() = when (phase) {
                is PremiumPdfImportPhase.ParseFailed -> phase.message
                is PremiumPdfImportPhase.ApplyFailed -> phase.message
                else -> null
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api

    fun parsePdf(uri: Uri, contentResolver: ContentResolver) {
        if (!transition { it.startParsing() }) return
        viewModelScope.launch {
            val result = LicPremiumPdfParser.parse(uri, contentResolver)
            transition { it.onParsed(result) }
        }
    }

    fun applyCurrentImport() {
        var applying: PremiumPdfImportPhase.Applying? = null
        if (!transition { phase -> phase.startApplying().also { applying = it } }) return
        val parsed = applying?.parsed ?: return
        viewModelScope.launch {
            try {
                val response = when (parsed) {
                    is LicPremiumPdfParseResult.PremiumDueList -> api.applyPremiumDueImport(parsed.result)
                    is LicPremiumPdfParseResult.CommissionBill -> api.applyCommissionBillImport(parsed.result)
                    // Unreachable: HasParsed phases never hold a parse error.
                    is LicPremiumPdfParseResult.Error -> {
                        transition { it.onApplyFailed(parsed.message) }
                        return@launch
                    }
                }

                val body = response.body()
                if (response.isSuccessful && body != null) {
                    transition { it.onApplySucceeded(body) }
                } else {
                    val message = response.errorBody()?.string()?.extractMessage() ?: "Could not apply import."
                    transition { it.onApplyFailed(message) }
                }
            } catch (e: Exception) {
                transition { it.onApplyFailed(e.localizedMessage ?: "Import failed.") }
            }
        }
    }

    fun clear() {
        transition { it.clear() }
    }

    /** Atomically applies [step] to the current phase; returns false if the transition isn't allowed. */
    private inline fun transition(step: (PremiumPdfImportPhase) -> PremiumPdfImportPhase?): Boolean {
        while (true) {
            val current = _uiState.value
            val next = step(current.phase) ?: return false
            if (_uiState.compareAndSet(current, current.copy(phase = next))) return true
        }
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
