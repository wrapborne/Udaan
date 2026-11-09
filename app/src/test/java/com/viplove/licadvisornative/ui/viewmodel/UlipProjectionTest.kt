package com.viplove.licadvisornative.ui.viewmodel

import com.viplove.licadvisornative.model.UlipProjectionResult
import org.junit.Assert.assertEquals
import org.junit.Test

class UlipProjectionTest {

    private fun year(n: Int, closing: Double, yield: Double) = UlipProjectionResult(
        year = n, age = 30 + n, annualPremium = 0.0, premiumAllocationCharge = 0.0, mortalityCharge = 0.0,
        adminCharge = 0.0, guaranteedAddition = 0.0, openingFundValue = 0.0, netInvestment = 0.0,
        fundGrowth = 0.0, fundManagementCharge = 0.0, closingFundValue = closing, netYield = yield
    )

    @Test
    fun finalValuesComeFromTheLastYear() {
        val projection = UlipViewModel.Projection(listOf(year(1, 100.0, 1.0), year(2, 250.0, 6.5)))
        assertEquals(250.0, projection.finalMaturityValue, 0.0)
        assertEquals(6.5, projection.finalNetYield, 0.0)
    }

    /** A blank term used to produce an empty list and crash the screen while formatting null. */
    @Test(expected = IllegalArgumentException::class)
    fun emptyProjectionCannotBeCreated() {
        UlipViewModel.Projection(emptyList())
    }
}
