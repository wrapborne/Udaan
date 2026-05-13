// File: app/src/main/java/com/viplove/licadvisornative/util/UlipPlanRepository.kt
package com.viplove.licadvisornative.util

import com.viplove.licadvisornative.model.*

/**
 * This object acts as a central repository for all available ULIP plan rules.
 * All data is sourced from the official plan brochures.
 */
object UlipPlanRepository {

    // --- RULES FOR 873 (INDEX PLUS) ---
    private val indexPlusRules = UlipPlanRules(
        planName = "Index Plus",
        planNumber = "873",
        premiumType = "Regular",
        premiumAllocationCharges = listOf(
            // Source: Plans_873_873Brochure.pdf, Page 10 (Offline sale)
            PremiumAllocationCharge(fromYear = 1, toYear = 1, percentage = 8.0, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 2, toYear = 5, percentage = 5.5, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 6, toYear = Int.MAX_VALUE, percentage = 4.0, premiumType = "Regular")
        ),
        mortalityChargeRates = listOf(
            // Source: Plans_873_873Brochure.pdf, Page 11 (Sample rates)
            MortalityChargeRate(age = 25, rate = 1.26),
            MortalityChargeRate(age = 35, rate = 1.62),
            MortalityChargeRate(age = 45, rate = 3.48),
            MortalityChargeRate(age = 50, rate = 5.99),
            MortalityChargeRate(age = 60, rate = 15.07)
        ),
        fundOptions = listOf(
            // Source: Plans_873_873Brochure.pdf, Page 8
            FundOption(name = "Flexi Growth Fund", sfin = "ULIF00510/11/23", fundManagementCharge = 1.35),
            FundOption(name = "Flexi Smart Growth Fund", sfin = "ULIF00610/11/23", fundManagementCharge = 1.35)
        ),
        policyAdministrationCharges = listOf(
            PolicyAdministrationChargeRules(
                // Source: Plans_873_873Brochure.pdf, Page 11
                premiumType = "Regular",
                startsFromYear = 6,
                toYear = Int.MAX_VALUE, // Doesn't end
                initialMonthlyCharge = 125.0,
                initialChargeAsPercentageOfPremium = 3.25, // 3.25% of Annualized Premium
                escalationRate = 5.0 // escalating at 5% p.a. from 7th year
            )
        ),
        guaranteedAdditions = listOf(
            // Source: Plans_873_873Brochure.pdf, Page 5 (Using AP >= 48k column, as per illustration)
            GuaranteedAddition(fromYear = 6, toYear = 6, percentage = 5.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 10, toYear = 10, percentage = 10.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 15, toYear = 15, percentage = 20.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 20, toYear = 20, percentage = 25.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 25, toYear = 25, percentage = 30.0, of = "AnnualPremium")
        )
    )

    // --- RULES FOR 735 (NEW ENDOWMENT PLUS) ---
    private val endowmentPlusRules = UlipPlanRules(
        planName = "New Endowment Plus",
        planNumber = "735",
        premiumType = "Regular",
        premiumAllocationCharges = listOf(
            // Source: Plans_735_735Brochure.pdf, Page 8
            PremiumAllocationCharge(fromYear = 1, toYear = 1, percentage = 7.5, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 2, toYear = 5, percentage = 5.0, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 6, toYear = Int.MAX_VALUE, percentage = 3.0, premiumType = "Regular")
        ),
        mortalityChargeRates = listOf(
            // Source: Plans_735_735Brochure.pdf, Page 8 (Sample rates)
            MortalityChargeRate(age = 25, rate = 1.26),
            MortalityChargeRate(age = 35, rate = 1.62),
            MortalityChargeRate(age = 45, rate = 3.48),
            MortalityChargeRate(age = 50, rate = 5.99)
        ),
        fundOptions = listOf(
            // Source: Plans_735_735Brochure.pdf, Page 5 & 9
            FundOption(name = "Bond Fund", sfin = "ULIF001201114LICNED+BND512", fundManagementCharge = 0.75),
            FundOption(name = "Secured Fund", sfin = "ULIF002201114LICNED+SEC512", fundManagementCharge = 0.75),
            FundOption(name = "Balanced Fund", sfin = "ULIF003201114LICNED+BAL512", fundManagementCharge = 0.75),
            FundOption(name = "Growth Fund", sfin = "ULIF004201114LICNED+GRW512", fundManagementCharge = 0.75)
        ),
        policyAdministrationCharges = listOf(
            PolicyAdministrationChargeRules(
                // Source: Plans_735_735Brochure.pdf, Page 9 (Using Yr 6+ logic for simplicity)
                premiumType = "Regular",
                startsFromYear = 6,
                toYear = Int.MAX_VALUE,
                initialMonthlyCharge = 150.0,
                initialChargeAsPercentageOfPremium = 0.0, // Not based on premium from 6th year
                escalationRate = 5.0 // escalating at 5% p.a. from 7th year
            )
        ),
        // Source: Plans_735_735Brochure.pdf - No Guaranteed Additions mentioned.
        guaranteedAdditions = emptyList()
    )

    // --- RULES FOR 752 (SIIP) ---
    private val siipRules = UlipPlanRules(
        planName = "SIIP",
        planNumber = "752",
        premiumType = "Regular",
        premiumAllocationCharges = listOf(
            // Source: Plans_752_752Brochure.pdf, Page 9 (Offline sale)
            PremiumAllocationCharge(fromYear = 1, toYear = 1, percentage = 8.0, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 2, toYear = 5, percentage = 5.5, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 6, toYear = Int.MAX_VALUE, percentage = 3.0, premiumType = "Regular")
        ),
        mortalityChargeRates = listOf(
            // Source: Plans_752_752Brochure.pdf, Page 10 (Sample rates)
            MortalityChargeRate(age = 25, rate = 1.26),
            MortalityChargeRate(age = 35, rate = 1.62),
            MortalityChargeRate(age = 45, rate = 3.48),
            MortalityChargeRate(age = 50, rate = 5.99),
            MortalityChargeRate(age = 60, rate = 15.07)
        ),
        fundOptions = listOf(
            // Source: Plans_752_752Brochure.pdf, Page 7 & 11
            FundOption(name = "Bond Fund", sfin = "ULIF001241218LICULIP+BND512", fundManagementCharge = 1.35),
            FundOption(name = "Secured Fund", sfin = "ULIF002241218LICULIP+SEC512", fundManagementCharge = 1.35),
            FundOption(name = "Balanced Fund", sfin = "ULIF003241218LICULIP+BAL512", fundManagementCharge = 1.35),
            FundOption(name = "Growth Fund", sfin = "ULIF004241218LICULIP+GRW512", fundManagementCharge = 1.35)
        ),
        policyAdministrationCharges = listOf(
            PolicyAdministrationChargeRules(
                // Source: Plans_752_752Brochure.pdf, Page 10
                premiumType = "Regular",
                startsFromYear = 6,
                toYear = Int.MAX_VALUE,
                initialMonthlyCharge = 150.0,
                initialChargeAsPercentageOfPremium = 0.0, // Not based on premium
                escalationRate = 5.0 // escalating at 5% p.a. from 7th year
            )
        ),
        guaranteedAdditions = listOf(
            // Source: Plans_752_752Brochure.pdf, Page 3
            GuaranteedAddition(fromYear = 6, toYear = 6, percentage = 5.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 10, toYear = 10, percentage = 10.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 15, toYear = 15, percentage = 15.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 20, toYear = 20, percentage = 20.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 25, toYear = 25, percentage = 25.0, of = "AnnualPremium")
        )
    )

    // --- RULES FOR 867 (NEW PENSION PLUS - REGULAR) ---
    private val newPensionPlusRegularRules = UlipPlanRules(
        planName = "New Pension Plus (Regular)",
        planNumber = "867",
        premiumType = "Regular",
        premiumAllocationCharges = listOf(
            // Source: Plans_867_867Brochure.pdf, Page 16 (Offline, Regular, AP >= 50k)
            PremiumAllocationCharge(fromYear = 1, toYear = 1, percentage = 7.0, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 2, toYear = 5, percentage = 4.0, premiumType = "Regular"),
            PremiumAllocationCharge(fromYear = 6, toYear = Int.MAX_VALUE, percentage = 3.0, premiumType = "Regular")
        ),
        mortalityChargeRates = emptyList(), // Source: Plans_867_867Brochure.pdf, Page 17 (Mortality Charge: Nil)
        fundOptions = listOf(
            // Source: Plans_867_867Brochure.pdf, Page 11-12 & 18
            FundOption(name = "Pension Bond Fund", sfin = "ULIF001010222LICPENFBND512", fundManagementCharge = 1.35),
            FundOption(name = "Pension Secured Fund", sfin = "ULIF002010222LICPENFSEC512", fundManagementCharge = 1.35),
            FundOption(name = "Pension Balanced Fund", sfin = "ULIF003010222LICPENFBAL512", fundManagementCharge = 1.35),
            FundOption(name = "Pension Growth Fund", sfin = "ULIF004010222LICPENFGRW512", fundManagementCharge = 1.35)
        ),
        policyAdministrationCharges = listOf(
            PolicyAdministrationChargeRules(
                // Source: Plans_867_867Brochure.pdf, Page 17 (Regular Premium, AP >= 50k)
                premiumType = "Regular",
                startsFromYear = 1,
                toYear = 5, // Charge is NIL from 6th year onwards
                initialMonthlyCharge = 57.0, // Using max from Year 1
                initialChargeAsPercentageOfPremium = 0.190, // Using Year 1 (calc based on Inst_Prem*k, simplified to AP)
                escalationRate = 0.0 // Logic is de-escalating, not escalating. Using simplified flat rate.
            )
        ),
        guaranteedAdditions = listOf(
            // Source: Plans_867_867Brochure.pdf, Page 5-6 (Regular Premium)
            GuaranteedAddition(fromYear = 6, toYear = 6, percentage = 5.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 10, toYear = 10, percentage = 10.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 11, toYear = 15, percentage = 4.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 16, toYear = 20, percentage = 5.5, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 21, toYear = 25, percentage = 7.0, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 26, toYear = 30, percentage = 8.75, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 31, toYear = 35, percentage = 10.75, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 36, toYear = 40, percentage = 13.00, of = "AnnualPremium"),
            GuaranteedAddition(fromYear = 41, toYear = 42, percentage = 15.50, of = "AnnualPremium")
        )
    )

    // --- RULES FOR 867 (NEW PENSION PLUS - SINGLE) ---
    private val newPensionPlusSingleRules = UlipPlanRules(
        planName = "New Pension Plus (Single)",
        planNumber = "867",
        premiumType = "Single",
        premiumAllocationCharges = listOf(
            // Source: Plans_867_867Brochure.pdf, Page 16 (Offline, Single)
            PremiumAllocationCharge(fromYear = 1, toYear = 1, percentage = 3.30, premiumType = "Single")
        ),
        mortalityChargeRates = emptyList(), // Source: Plans_867_867Brochure.pdf, Page 17 (Mortality Charge: Nil)
        fundOptions = listOf(
            // Source: Plans_867_867Brochure.pdf, Page 11-12 & 18
            FundOption(name = "Pension Bond Fund", sfin = "ULIF001010222LICPENFBND512", fundManagementCharge = 1.35),
            FundOption(name = "Pension Secured Fund", sfin = "ULIF002010222LICPENFSEC512", fundManagementCharge = 1.35),
            FundOption(name = "Pension Balanced Fund", sfin = "ULIF003010222LICPENFBAL512", fundManagementCharge = 1.35),
            FundOption(name = "Pension Growth Fund", sfin = "ULIF004010222LICPENFGRW512", fundManagementCharge = 1.35)
        ),
        policyAdministrationCharges = listOf(
            PolicyAdministrationChargeRules(
                // Source: Plans_867_867Brochure.pdf, Page 17 (Single Premium)
                premiumType = "Single",
                startsFromYear = 1,
                toYear = 5, // Charge is NIL from 6th year onwards
                initialMonthlyCharge = 80.0, // Year 1 rate
                initialChargeAsPercentageOfPremium = 0.0,
                escalationRate = 0.0 // De-escalates, simplified to flat 80/mo
            )
        ),
        guaranteedAdditions = listOf(
            // Source: Plans_867_867Brochure.pdf, Page 5-6 (Single Premium)
            GuaranteedAddition(fromYear = 6, toYear = 6, percentage = 4.0, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 10, toYear = 10, percentage = 5.0, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 11, toYear = 15, percentage = 1.25, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 16, toYear = 20, percentage = 1.50, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 21, toYear = 25, percentage = 2.00, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 26, toYear = 30, percentage = 2.50, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 31, toYear = 35, percentage = 3.00, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 36, toYear = 40, percentage = 3.75, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 41, toYear = 42, percentage = 4.50, of = "SinglePremium")
        )
    )

    // --- RULES FOR 749 (NIVESH PLUS) ---
    private val niveshPlusRules = UlipPlanRules(
        planName = "Nivesh Plus",
        planNumber = "749",
        premiumType = "Single",
        premiumAllocationCharges = listOf(
            // Source: Plans_749_749Brochure.pdf, Page 7 (Offline sale)
            PremiumAllocationCharge(fromYear = 1, toYear = 1, percentage = 3.30, premiumType = "Single")
        ),
        mortalityChargeRates = listOf(
            // Source: Plans_749_749Brochure.pdf, Page 7 (Sample rates)
            MortalityChargeRate(age = 25, rate = 1.26),
            MortalityChargeRate(age = 35, rate = 1.62),
            MortalityChargeRate(age = 45, rate = 3.48),
            MortalityChargeRate(age = 50, rate = 5.99)
        ),
        fundOptions = listOf(
            // Source: Plans_749_749Brochure.pdf, Page 5 & 8
            FundOption(name = "Bond Fund", sfin = "ULIF001241218LICULIP+BND512", fundManagementCharge = 1.35),
            FundOption(name = "Secured Fund", sfin = "ULIF002241218LICULIP+SEC512", fundManagementCharge = 1.35),
            FundOption(name = "Balanced Fund", sfin = "ULIF003241218LICULIP+BAL512", fundManagementCharge = 1.35),
            FundOption(name = "Growth Fund", sfin = "ULIF004241218LICULIP+GRW512", fundManagementCharge = 1.35)
        ),
        policyAdministrationCharges = listOf(
            PolicyAdministrationChargeRules(
                // Source: Plans_749_749Brochure.pdf, Page 7-8 - No Policy Admin Charge listed.
                premiumType = "Single",
                startsFromYear = 1,
                toYear = 0, // Set toYear < startsFromYear to effectively disable it
                initialMonthlyCharge = 0.0,
                initialChargeAsPercentageOfPremium = 0.0,
                escalationRate = 0.0
            )
        ),
        guaranteedAdditions = listOf(
            // Source: Plans_749_749Brochure.pdf, Page 3
            GuaranteedAddition(fromYear = 6, toYear = 6, percentage = 3.0, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 10, toYear = 10, percentage = 4.0, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 15, toYear = 15, percentage = 5.0, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 20, toYear = 20, percentage = 6.0, of = "SinglePremium"),
            GuaranteedAddition(fromYear = 25, toYear = 25, percentage = 7.0, of = "SinglePremium")
        )
    )

    // The main map of all available plans.
    // We use planName as the key to support two rule sets for Plan 867.
    private val plans: Map<String, UlipPlanRules> = mapOf(
        "Index Plus" to indexPlusRules,
        "New Endowment Plus" to endowmentPlusRules,
        "SIIP" to siipRules,
        "New Pension Plus (Regular)" to newPensionPlusRegularRules,
        "New Pension Plus (Single)" to newPensionPlusSingleRules,
        "Nivesh Plus" to niveshPlusRules
    )

    fun getAvailablePlans(): List<UlipPlanRules> {
        return plans.values.toList().sortedBy { it.planName } // Sort them alphabetically
    }
}