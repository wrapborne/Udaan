// File: app/src/main/java/com/viplove/licadvisornative/model/UlipPlanModels.kt
package com.viplove.licadvisornative.model

/**
 * This file contains the data structures for defining the rules of a dynamic ULIP plan.
 * This allows new plans to be added in the future by simply creating a new UlipPlanRules object.
 */

// Represents a single rate for Premium Allocation Charge
data class PremiumAllocationCharge(
    val fromYear: Int,
    val toYear: Int,
    val percentage: Double, // e.g., 8.0 for 8%
    // NEW: Specifies if the charge is for 'Regular' or 'Single' premium type
    val premiumType: String = "Regular"
)

// Represents a single rate for Mortality Charges (cost per 1000 Sum at Risk)
data class MortalityChargeRate(
    val age: Int,
    val rate: Double
)

// Represents a single Guaranteed Addition
data class GuaranteedAddition(
    // MODIFIED: Changed from 'atYear' to a range to support plans like New Pension Plus
    val fromYear: Int,
    val toYear: Int,
    val percentage: Double, // Renamed from percentageOfAnnualPremium
    // NEW: Specifies if the GA is a % of 'AnnualPremium' or 'SinglePremium'
    val of: String = "AnnualPremium"
)

// Represents a single fund option for the plan
data class FundOption(
    val name: String,
    val sfin: String, // Segregated Fund Identification Number
    val fundManagementCharge: Double // e.g., 1.35 for 1.35%
)

/**
 * The main data class that holds the entire rule set for a specific ULIP plan.
 */
data class UlipPlanRules(
    val planName: String,
    val planNumber: String,
    // NEW: Differentiates between regular and single premium plans
    val premiumType: String = "Regular", // "Regular" or "Single"
    val premiumAllocationCharges: List<PremiumAllocationCharge>,
    val mortalityChargeRates: List<MortalityChargeRate>, // Per 1000 Sum at Risk
    val fundOptions: List<FundOption>,
    // MODIFIED: Changed to a list to support different rules for Regular/Single
    val policyAdministrationCharges: List<PolicyAdministrationChargeRules>,
    val guaranteedAdditions: List<GuaranteedAddition>
)

data class PolicyAdministrationChargeRules(
    // NEW: Added premiumType to link the charge
    val premiumType: String = "Regular",
    val startsFromYear: Int,
    // NEW: Added toYear to support charges that end (like Plan 867)
    val toYear: Int = Int.MAX_VALUE,
    val initialMonthlyCharge: Double,
    val initialChargeAsPercentageOfPremium: Double, // As % of *Annual* premium
    val escalationRate: Double // e.g., 5.0 for 5%
)

/**
 * Represents the detailed year-by-year result of a ULIP projection.
 */
data class UlipProjectionResult(
    val year: Int,
    val age: Int,
    val annualPremium: Double,
    val premiumAllocationCharge: Double,
    val mortalityCharge: Double,
    val adminCharge: Double,
    val guaranteedAddition: Double,
    val openingFundValue: Double,
    val netInvestment: Double,
    val fundGrowth: Double,
    val fundManagementCharge: Double,
    val closingFundValue: Double,
    val netYield: Double
)