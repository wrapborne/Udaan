package com.viplove.licadvisornative.ui.viewmodel

import com.viplove.licadvisornative.model.CommissionBillPdfResult
import com.viplove.licadvisornative.model.LicPremiumPdfParseResult
import com.viplove.licadvisornative.model.PremiumDuePdfResult
import com.viplove.licadvisornative.model.PremiumPdfImportApplyResult
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.Applied
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.ApplyFailed
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.Applying
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.Idle
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.ParseFailed
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.Parsing
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportPhase.Reviewing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumPdfImportPhaseTest {

    private val dueList = LicPremiumPdfParseResult.PremiumDueList(PremiumDuePdfResult(agentCode = "0189927C"))
    private val commissionBill = LicPremiumPdfParseResult.CommissionBill(CommissionBillPdfResult(agentCode = "0189927C"))
    private val applyResult = PremiumPdfImportApplyResult(importedRows = 3)

    @Test
    fun happyPath_parsesReviewsAndApplies() {
        val parsing = Idle.startParsing()
        assertEquals(Parsing, parsing)
        val reviewing = parsing!!.onParsed(dueList)
        assertEquals(Reviewing(dueList), reviewing)
        val applying = reviewing!!.startApplying()
        assertEquals(Applying(dueList), applying)
        assertEquals(Applied(dueList, applyResult), applying!!.onApplySucceeded(applyResult))
    }

    @Test
    fun parseError_becomesParseFailed_andCannotApply() {
        val failed = Parsing.onParsed(LicPremiumPdfParseResult.Error("Not a LIC PDF"))
        assertEquals(ParseFailed("Not a LIC PDF"), failed)
        assertNull(failed!!.startApplying())
    }

    @Test
    fun applyFailure_keepsParsedPdf_andAllowsRetry() {
        val failed = Applying(commissionBill).onApplyFailed("offline")
        assertEquals(ApplyFailed(commissionBill, "offline"), failed)
        assertEquals(Applying(commissionBill), failed!!.startApplying())
    }

    @Test
    fun appliedImport_cannotBeAppliedAgain() {
        assertNull(Applied(dueList, applyResult).startApplying())
    }

    @Test
    fun busyPhases_rejectNewParsesClearsAndApplies() {
        listOf(Parsing, Applying(dueList)).forEach { busy ->
            assertTrue(busy.isBusy)
            assertNull(busy.startParsing())
            assertNull(busy.clear())
            assertNull(busy.startApplying())
        }
    }

    @Test
    fun settledPhases_canStartOverOrClear() {
        listOf(Idle, ParseFailed("x"), Reviewing(dueList), ApplyFailed(dueList, "x"), Applied(dueList, applyResult)).forEach { settled ->
            assertFalse(settled.isBusy)
            assertEquals(Parsing, settled.startParsing())
            assertEquals(Idle, settled.clear())
        }
    }

    @Test
    fun completionEvents_areIgnoredOutsideTheirBusyPhase() {
        assertNull(Idle.onParsed(dueList))
        assertNull(Reviewing(dueList).onApplySucceeded(applyResult))
        assertNull(Reviewing(dueList).onApplyFailed("late"))
    }

    @Test
    fun uiState_derivesLegacyFieldsFromPhase() {
        val failed = PremiumPdfImportViewModel.UiState(ApplyFailed(dueList, "offline"))
        assertEquals(dueList, failed.parseResult)
        assertEquals("offline", failed.error)
        assertTrue(failed.canApply)
        assertFalse(failed.isApplying)

        val applied = PremiumPdfImportViewModel.UiState(Applied(dueList, applyResult))
        assertEquals(applyResult, applied.applyResult)
        assertFalse(applied.canApply)
        assertNull(applied.error)
    }
}
