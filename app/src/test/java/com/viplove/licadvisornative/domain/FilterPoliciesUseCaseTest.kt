package com.viplove.licadvisornative.domain

import com.viplove.licadvisornative.model.Policy
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterPoliciesUseCaseTest {

    private val day = 86_400_000L
    private val policies = listOf(
        Policy(policyId = "a", policyNumber = "111", plan = "914", mode = "YLY", shortName = "Asha", doc = 10 * day),
        Policy(policyId = "b", policyNumber = "222", plan = "936", mode = "MLY", shortName = "Bala", doc = 20 * day, isAnanda = true),
        Policy(policyId = "c", policyNumber = "333", plan = "914", mode = "MLY", shortName = "Chitra", doc = 30 * day)
    )

    private fun filter(
        query: String = "",
        plan: String = "All",
        mode: String = "All",
        start: Long? = null,
        end: Long? = null
    ) = FilterPoliciesUseCase().execute(
        FilterPoliciesUseCase.Params(
            allPolicies = policies, searchQuery = query, selectedPlan = plan, selectedMode = mode,
            startDate = start, endDate = end, selectedFinancialYear = "None", selectedAppraisalYear = "None",
            user = null
        )
    ).map { it.policyId }

    @Test
    fun noFiltersReturnsEverything() = assertEquals(listOf("a", "b", "c"), filter())

    @Test
    fun searchMatchesNameNumberOrPlanIgnoringCase() {
        assertEquals(listOf("b"), filter(query = "bala"))
        assertEquals(listOf("c"), filter(query = "333"))
        assertEquals(listOf("a", "c"), filter(query = "914"))
    }

    @Test
    fun planAndModeFilters() {
        assertEquals(listOf("a", "c"), filter(plan = "914"))
        assertEquals(listOf("b", "c"), filter(mode = "MLY"))
        assertEquals(listOf("b"), filter(mode = "ANANDA"))
    }

    @Test
    fun dateRangeIncludesTheWholeEndDay() {
        assertEquals(listOf("a", "b"), filter(start = 10 * day, end = 20 * day))
        assertEquals(listOf("b"), filter(start = 11 * day, end = 29 * day))
    }
}
