package com.viplove.licadvisornative.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viplove.licadvisornative.model.*
import com.viplove.licadvisornative.util.NavScraper
import com.viplove.licadvisornative.util.NavStorage
import com.viplove.licadvisornative.util.RemoteConfigManager
import com.viplove.licadvisornative.util.UlipFilterManager
import com.viplove.licadvisornative.util.UlipPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val TAG = "UlipViewModel"

class UlipViewModel : ViewModel() {

    // This map will hold all pre-calculated CAGR options, loaded from cache
    // The key is the SFIN (e.g., "ULIF005...")
    private var cagrOptionsCache: Map<String, List<Pair<String, Double>>> = emptyMap()

    // This is the default list of options if calculation fails
    private val defaultCagrOptions = listOf(
        "4% (User Fed)" to 4.0,
        "8% (User Fed)" to 8.0,
        "10% (User Fed)" to 10.0,
        "12% (User Fed)" to 12.0
    )

    data class UlipUiState(
        val searchQuery: String = "",
        val isFetchingAllNavs: Boolean = true, // Used for the spinner
        val navListError: String? = null,
        val allNavs: List<NavData> = emptyList(), // Stores the fetched NAVs

        // Filter states for the "NAV" tab
        val selectedPlans: Set<String> = emptySet(),
        val filtersInitialized: Boolean = false,
        val cacheInitialized: Boolean = false, // Tracks if cache is loaded

        // Calculator states
        val availablePlans: List<UlipPlanRules> = UlipPlanRepository.getAvailablePlans(),
        val selectedPlan: UlipPlanRules? = UlipPlanRepository.getAvailablePlans().firstOrNull(),
        val projectionAge: String = "30",
        val projectionPremium: String = "2500",
        val projectionMode: String = "MLY",
        val projectionTerm: String = "25",
        val selectedFundName: String = UlipPlanRepository.getAvailablePlans().firstOrNull()?.fundOptions?.first()?.name ?: "",

        val projectionRate: Double = 8.0,
        val selectedCagrLabel: String = "8% (User Fed)",
        val cagrOptions: List<Pair<String, Double>> = listOf("8% (User Fed)" to 8.0), // Default

        val saMultiplier: Int = 10,
        val sumAssured: Double = 0.0,
        val projectionResult: List<UlipProjectionResult>? = null,
        val finalMaturityValue: Double? = null,
        val finalNetYield: Double? = null,
        val isModeSelectionEnabled: Boolean = true
    )

    private val _uiState = MutableStateFlow(UlipUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Set initial state based on the default plan
        val initialPlan = UlipPlanRepository.getAvailablePlans().firstOrNull()
        if (initialPlan != null) {
            _uiState.update {
                it.copy(
                    isModeSelectionEnabled = initialPlan.premiumType == "Regular",
                    projectionMode = if (initialPlan.premiumType == "Regular") "MLY" else "Single"
                )
            }
        }
        updateSumAssured()
    }

    /**
     * This is the new entry point, called by both NAV and Calculator tabs.
     * It loads data from cache OR fetches new data, then pre-calculates all CAGRs.
     */
    fun initializeCache(context: Context) {
        if (_uiState.value.cacheInitialized) return // Already done

        viewModelScope.launch {
            if (NavStorage.isCacheValid(context)) {
                // 1. Load from Cache
                Log.d(TAG, "Cache is valid, loading from NavStorage...")
                val navs = NavStorage.loadNavs(context)
                val cagrs = NavStorage.loadCagrOptions(context)

                cagrOptionsCache = cagrs // Load pre-calculated CAGRs
                _uiState.update {
                    it.copy(
                        allNavs = navs,
                        cacheInitialized = true,
                        isFetchingAllNavs = false // Done fetching
                    )
                }
                initializeFilters(context, navs)
                updateCagrDropdown(autoSelect = true)

            } else {
                // 2. Fetch from Network
                Log.d(TAG, "Cache is stale, fetching from network...")
                _uiState.update { it.copy(isFetchingAllNavs = true, navListError = null) }

                val publicUrl = RemoteConfigManager.ulipPublicUrl
                // --- YEH RAHA FIX #1 ---
                // `ulipNavUrls` (plural) ko call kiya, jo ab List<String> return karta hai
                val dataUrls = RemoteConfigManager.ulipNavUrls

                // --- YEH RAHA FIX #2 ---
                // `.isBlank()` ko `.isEmpty()` se replace kiya
                if (publicUrl.isBlank() || dataUrls.isEmpty()) {
                    Log.e(TAG, "Config URLs are missing. PublicURL blank: ${publicUrl.isBlank()}, DataURLs empty: ${dataUrls.isEmpty()}")
                    _uiState.update { it.copy(isFetchingAllNavs = false, navListError = "Config URLs are missing.") }
                    return@launch
                }

                // --- YEH RAHA FIX #3 ---
                // Naya scraper function `fetchAllNavData` (plural) call kiya
                when (val result = NavScraper.fetchAllNavData(publicUrl, dataUrls)) {
                    is NavScraper.ScrapeResult.Success -> {
                        val navs = result.data
                        Log.d(TAG, "Successfully fetched ${navs.size} NAVs. Pre-calculating CAGRs...")
                        // Pre-calculate all CAGRs
                        cagrOptionsCache = preCalculateAllCagrs(navs)
                        // Save to cache
                        NavStorage.saveData(context, navs, cagrOptionsCache)

                        _uiState.update {
                            it.copy(
                                allNavs = navs,
                                cacheInitialized = true,
                                isFetchingAllNavs = false,
                                navListError = null
                            )
                        }
                        initializeFilters(context, navs) // Initialize filters
                        updateCagrDropdown(autoSelect = true) // Update dropdown
                    }
                    is NavScraper.ScrapeResult.Error -> {
                        _uiState.update { it.copy(isFetchingAllNavs = false, navListError = result.message) }
                        _uiState.update { it.copy(cacheInitialized = true) }
                        updateCagrDropdown(autoSelect = true) // Will load defaults
                    }
                }
            }
        }
    }

    /**
     * Initializes the "NAV" tab's filters.
     */
    private fun initializeFilters(context: Context, navs: List<NavData>) {
        if (_uiState.value.filtersInitialized) return

        viewModelScope.launch {
            val savedFilters = UlipFilterManager.loadFilters(context)
            val allPlanNames = navs.map { it.planName }.toSet()

            val finalSelectedPlans = if (savedFilters == null) {
                UlipFilterManager.saveFilters(context, allPlanNames)
                allPlanNames
            } else {
                savedFilters.intersect(allPlanNames)
            }

            _uiState.update { it.copy(
                selectedPlans = finalSelectedPlans,
                filtersInitialized = true
            ) }
        }
    }

    fun onSelectedPlansChanged(context: Context, newPlans: Set<String>) {
        _uiState.update { it.copy(selectedPlans = newPlans) }
        viewModelScope.launch {
            UlipFilterManager.saveFilters(context, newPlans)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectedPlanChanged(plan: UlipPlanRules) {
        val isRegularPremium = plan.premiumType == "Regular"
        _uiState.update {
            it.copy(
                selectedPlan = plan,
                selectedFundName = plan.fundOptions.first().name, // Default to first fund
                isModeSelectionEnabled = isRegularPremium,
                projectionMode = if (isRegularPremium) "MLY" else "Single"
            )
        }
        updateSumAssured()
        updateCagrDropdown(autoSelect = true)
    }

    fun onSelectedFundChanged(fundName: String) {
        _uiState.update { it.copy(selectedFundName = fundName) }
        updateCagrDropdown(autoSelect = true)
    }

    /**
     * Pre-calculates CAGR options for ALL funds found in the NAV data.
     */
    private fun preCalculateAllCagrs(navs: List<NavData>): Map<String, List<Pair<String, Double>>> {
        val cagrMap = mutableMapOf<String, List<Pair<String, Double>>>()
        val allSfins = UlipPlanRepository.getAvailablePlans().flatMap { it.fundOptions }.map { it.sfin }.toSet()

        for (sfin in allSfins) {
            val navData = navs.find { it.sfin == sfin }
            val options = mutableListOf<Pair<String, Double>>()

            val historicalCagr = calculateHistoricalCagr(navData)
            if (historicalCagr != null) {
                val formattedCagr = String.format("%.2f", historicalCagr)
                options.add("Historical ($formattedCagr%)" to historicalCagr)

                val forecastValue = historicalCagr + 1.5
                val formattedForecast = String.format("%.2f", forecastValue)
                options.add("Forecast ($formattedForecast%)" to forecastValue)
            }

            options.addAll(defaultCagrOptions) // Add the user-fed options
            cagrMap[sfin] = options
        }
        Log.d(TAG, "Pre-calculated CAGRs for ${cagrMap.size} funds.")
        return cagrMap
    }

    /**
     * Calculates CAGR for a *single* NavData item.
     */
    private fun calculateHistoricalCagr(navData: NavData?): Double? {
        if (navData == null) {
            Log.w(TAG, "calculateHistoricalCagr: NavData is null")
            return null
        }
        try {
            val launchDateStr = navData.launchDate
            val currentNav = navData.nav
            if (launchDateStr.isBlank() || currentNav <= 0) {
                Log.w(TAG, "calculateHistoricalCagr: Missing launchDate or NAV for ${navData.sfin}")
                return null
            }

            // Handle two possible date formats: "dd/MM/yyyy" (old) and "yyyy-MM-dd" (new)
            val sdf = if (launchDateStr.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            }

            val launchDate = sdf.parse(launchDateStr) ?: return null
            if (launchDate.time > System.currentTimeMillis()) return null // Date in future

            val years = (System.currentTimeMillis() - launchDate.time) / (1000.0 * 60 * 60 * 24 * 365.25)
            if (years < (1.0/12.0)) return null // Not meaningful if < 1 month

            // All ULIPs launch with NAV 10
            val cagr = (((currentNav / 10.0).pow(1.0 / years)) - 1) * 100

            Log.d(TAG, "CAGR for ${navData.sfin}: $cagr% (NAV: $currentNav, Years: $years)")
            return if (cagr > -50.0 && cagr < 100.0) cagr else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate CAGR for ${navData.sfin}", e)
            return null
        }
    }

    /**
     * Updates the UI's CAGR dropdown based on the currently selected fund.
     */
    private fun updateCagrDropdown(autoSelect: Boolean = false) {
        val state = _uiState.value
        val selectedSfin = state.selectedPlan?.fundOptions
            ?.find { it.name == state.selectedFundName }?.sfin
            ?: ""

        val options = cagrOptionsCache[selectedSfin]

        if (options.isNullOrEmpty()) {
            Log.w(TAG, "No CAGR options found in cache for SFIN: $selectedSfin. Using default.")
            _uiState.update {
                it.copy(
                    cagrOptions = defaultCagrOptions,
                    selectedCagrLabel = defaultCagrOptions.first().first,
                    projectionRate = defaultCagrOptions.first().second
                )
            }
            return
        }

        val defaultOption = options.first()

        _uiState.update {
            it.copy(
                cagrOptions = options,
                selectedCagrLabel = if (autoSelect || it.selectedCagrLabel !in options.map { o -> o.first }) defaultOption.first else it.selectedCagrLabel,
                projectionRate = if (autoSelect || it.selectedCagrLabel !in options.map { o -> o.first }) defaultOption.second else it.projectionRate
            )
        }
    }

    fun onProjectionRateChanged(label: String, rate: Double) {
        _uiState.update { it.copy(selectedCagrLabel = label, projectionRate = rate) }
    }

    fun onProjectionAgeChanged(age: String) {
        _uiState.update { it.copy(projectionAge = age) }
        updateSumAssured()
    }

    fun onProjectionPremiumChanged(premium: String) {
        _uiState.update { it.copy(projectionPremium = premium) }
        updateSumAssured()
    }

    fun onProjectionModeChanged(mode: String) {
        if (_uiState.value.isModeSelectionEnabled) {
            _uiState.update { it.copy(projectionMode = mode) }
            updateSumAssured()
        }
    }

    fun onProjectionTermChanged(term: String) { _uiState.update { it.copy(projectionTerm = term) } }

    fun onSaMultiplierChanged(multiplier: Int) {
        _uiState.update { it.copy(saMultiplier = multiplier) }
        updateSumAssured()
    }

    private fun updateSumAssured() {
        val state = _uiState.value
        val premium = state.projectionPremium.toDoubleOrNull() ?: 0.0

        val basePremium = if (state.selectedPlan?.premiumType == "Single") {
            premium
        } else {
            premium * when (state.projectionMode) {
                "YLY" -> 1; "HLY" -> 2; "QLY" -> 4; "MLY" -> 12
                else -> 1
            }
        }

        val age = state.projectionAge.toIntOrNull() ?: 0
        val currentMultiplier = if (age > 50 && state.saMultiplier == 10) 7 else state.saMultiplier

        val newSumAssured = if (state.selectedPlan?.planNumber == "867") 0.0 else basePremium * currentMultiplier

        _uiState.update { it.copy(sumAssured = newSumAssured, saMultiplier = currentMultiplier) }
    }


    fun calculateMaturityProjection() {
        val state = _uiState.value
        val rules = state.selectedPlan ?: return
        val age = state.projectionAge.toIntOrNull() ?: 0
        val sumAssured = state.sumAssured
        val premium = state.projectionPremium.toDoubleOrNull() ?: 0.0
        val term = state.projectionTerm.toIntOrNull() ?: 0
        val cagr = state.projectionRate / 100.0
        val fund = rules.fundOptions.find { it.name == state.selectedFundName } ?: return

        if (premium <= 0 || term <= 0 || age <= 0) {
            if(rules.planNumber != "867" && sumAssured <= 0) return
        }

        val singlePremium = if (rules.premiumType == "Single") premium else 0.0
        val annualPremium = if (rules.premiumType == "Regular") {
            premium * when (state.projectionMode) {
                "YLY" -> 1; "HLY" -> 2; "QLY" -> 4; "MLY" -> 12
                else -> 1
            }
        } else 0.0

        val results = mutableListOf<UlipProjectionResult>()
        var openingFundValue = 0.0
        var totalPremiumsPaid = 0.0

        for (year in 1..term) {
            val currentYearPremium = if (rules.premiumType == "Single") {
                if (year == 1) singlePremium else 0.0
            } else {
                annualPremium
            }
            totalPremiumsPaid += currentYearPremium

            val allocChargeRule = rules.premiumAllocationCharges
                .firstOrNull { year >= it.fromYear && year <= it.toYear && it.premiumType == rules.premiumType }
            val allocationChargePercent = allocChargeRule?.percentage ?: 0.0
            val allocationCharge = currentYearPremium * (allocationChargePercent / 100.0)
            val netPremium = currentYearPremium - allocationCharge

            val sumAtRisk = max(0.0, sumAssured - openingFundValue)
            val currentAge = age + year - 1
            val mortalityRate = rules.mortalityChargeRates
                .minByOrNull { kotlin.math.abs(it.age - currentAge) }
                ?.rate ?: 0.0
            val mortalityCharge = (sumAtRisk / 1000) * mortalityRate

            val adminChargeRule = rules.policyAdministrationCharges
                .firstOrNull { year >= it.startsFromYear && year <= it.toYear && it.premiumType == rules.premiumType }

            val adminCharge = if (adminChargeRule != null) {
                val premiumBase = if(rules.premiumType == "Single") singlePremium else annualPremium
                val baseChargePercent = (premiumBase * (adminChargeRule.initialChargeAsPercentageOfPremium / 100.0))

                val baseCharge = if (baseChargePercent > 0) {
                    baseChargePercent / 12.0
                } else {
                    adminChargeRule.initialMonthlyCharge
                }

                val monthlyCharge = min(
                    baseCharge.takeIf { it > 0 } ?: Double.MAX_VALUE,
                    adminChargeRule.initialMonthlyCharge.takeIf { it > 0 } ?: Double.MAX_VALUE
                )

                monthlyCharge * 12 * (1 + (adminChargeRule.escalationRate / 100.0)).pow(year - adminChargeRule.startsFromYear)
            } else 0.0

            val gaRule = rules.guaranteedAdditions
                .find { year >= it.fromYear && year <= it.toYear }
            val guaranteedAdditionPercent = gaRule?.percentage ?: 0.0

            val premiumBaseForGA = if (gaRule?.of == "SinglePremium") singlePremium else annualPremium
            val guaranteedAddition = premiumBaseForGA * (guaranteedAdditionPercent / 100.0)

            val fundBeforeGrowth = openingFundValue + netPremium + guaranteedAddition
            val fundGrowth = fundBeforeGrowth * cagr

            val fundBeforeFMC = fundBeforeGrowth + fundGrowth - mortalityCharge - adminCharge
            val fmc = fundBeforeFMC * (fund.fundManagementCharge / 100.0)
            val closingFundValue = fundBeforeFMC - fmc

            val netYield = if (totalPremiumsPaid > 0) ((closingFundValue / totalPremiumsPaid).pow(1.0 / year) - 1) * 100 else 0.0

            results.add(
                UlipProjectionResult(
                    year = year, age = currentAge, annualPremium = currentYearPremium,
                    premiumAllocationCharge = allocationCharge, mortalityCharge = mortalityCharge,
                    adminCharge = adminCharge, guaranteedAddition = guaranteedAddition,
                    openingFundValue = openingFundValue, netInvestment = netPremium,
                    fundGrowth = fundGrowth, fundManagementCharge = fmc,
                    closingFundValue = closingFundValue, netYield = netYield
                )
            )
            openingFundValue = closingFundValue
        }

        _uiState.update { it.copy(
            projectionResult = results,
            finalMaturityValue = results.lastOrNull()?.closingFundValue,
            finalNetYield = results.lastOrNull()?.netYield
        ) }
    }
}