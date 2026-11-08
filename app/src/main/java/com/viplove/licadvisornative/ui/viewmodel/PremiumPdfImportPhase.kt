package com.viplove.licadvisornative.ui.viewmodel

import com.viplove.licadvisornative.model.LicPremiumPdfParseResult
import com.viplove.licadvisornative.model.PremiumPdfImportApplyResult

/**
 * The PDF import flow as an explicit state machine. See STATE_MACHINES.md.
 *
 * ```
 * Idle ──select──▶ Parsing ──ok──▶ Reviewing ──apply──▶ Applying ──ok──▶ Applied
 *                     │                ▲                    │
 *                     └─fail─▶ ParseFailed   ApplyFailed ◀──fail
 *                                            (apply = retry)
 * ```
 *
 * Every settled phase can start a new parse or be cleared back to [Idle]; the busy phases
 * ([Parsing], [Applying]) accept only their completion events. Transitions that are not
 * allowed return null, so callers can't double-apply an import or start a parse mid-write.
 */
sealed interface PremiumPdfImportPhase {

    /** True while a parse or a Firestore write is in flight. */
    val isBusy: Boolean get() = this is Parsing || this is Applying

    data object Idle : PremiumPdfImportPhase
    data object Parsing : PremiumPdfImportPhase
    data class ParseFailed(val message: String) : PremiumPdfImportPhase

    /** Phases that hold a successfully parsed PDF (never [LicPremiumPdfParseResult.Error]). */
    sealed interface HasParsed : PremiumPdfImportPhase {
        val parsed: LicPremiumPdfParseResult
    }

    data class Reviewing(override val parsed: LicPremiumPdfParseResult) : HasParsed
    data class Applying(override val parsed: LicPremiumPdfParseResult) : HasParsed
    data class ApplyFailed(override val parsed: LicPremiumPdfParseResult, val message: String) : HasParsed
    data class Applied(override val parsed: LicPremiumPdfParseResult, val result: PremiumPdfImportApplyResult) : HasParsed

    fun startParsing(): PremiumPdfImportPhase? = if (isBusy) null else Parsing

    fun onParsed(result: LicPremiumPdfParseResult): PremiumPdfImportPhase? {
        if (this !is Parsing) return null
        return when (result) {
            is LicPremiumPdfParseResult.Error -> ParseFailed(result.message)
            else -> Reviewing(result)
        }
    }

    /** Allowed from [Reviewing], and from [ApplyFailed] as a retry. Not after [Applied]. */
    fun startApplying(): Applying? = when (this) {
        is Reviewing -> Applying(parsed)
        is ApplyFailed -> Applying(parsed)
        else -> null
    }

    fun onApplySucceeded(result: PremiumPdfImportApplyResult): PremiumPdfImportPhase? =
        (this as? Applying)?.let { Applied(it.parsed, result) }

    fun onApplyFailed(message: String): PremiumPdfImportPhase? =
        (this as? Applying)?.let { ApplyFailed(it.parsed, message) }

    fun clear(): PremiumPdfImportPhase? = if (isBusy) null else Idle
}
