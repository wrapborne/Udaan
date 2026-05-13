package com.viplove.licadvisornative.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viplove.licadvisornative.model.ChartData
import com.viplove.licadvisornative.model.PlanData
import com.viplove.licadvisornative.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    private val gson = Gson()

    // ULIP NAV URL list — can be overridden by API config
    var ulipNavUrls: List<String> = listOf(
        "https://services.licindia.in/LICEPS/portlets/visitor/CurrentNAV/CurrentNAVDay/CurrentNAVDayController.jpf",
        "https://licindia.in/getNavValue"
    )
        private set

    var ulipPublicUrl: String = "https://licindia.in/plan-nav1"
        private set

    private val defaultConfigJson = """
    {
      "grp_1_plans": [736], "grp_2_plans": [], "limits": {}, "plan_details_list": []
    }
    """.trimIndent()

    private val defaultPlansJson = """
        [
          { "name": "Jeevan Labh", "planNumber": "736", "termsAndPpts": { "16": 10, "21": 15, "25": 16 } },
          { "name": "Jeevan Anand", "planNumber": "715", "termsAndPpts": { "15": 15, "20": 20, "25": 25, "30": 30 } }
        ]
    """.trimIndent()

    private val _chartDataState = MutableStateFlow<ChartData?>(null)
    val chartDataState = _chartDataState.asStateFlow()

    init {
        loadDefaultData()
    }

    private fun loadDefaultData() {
        try {
            val rules = gson.fromJson(defaultConfigJson, ChartData::class.java)
            val plansType = object : TypeToken<List<PlanData>>() {}.type
            val plans: List<PlanData> = gson.fromJson(defaultPlansJson, plansType)
            _chartDataState.value = rules.copy(plan_details_list = plans)
            Log.d(TAG, "Successfully loaded default data.")
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Could not parse hardcoded default JSON!", e)
        }
    }

    fun initializeAndFetch() {
        // Fetch ULIP NAV URL from API config (optional, non-blocking)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.api.getConfig("ulip_nav_url")
                if (response.isSuccessful) {
                    val value = response.body()?.value?.toString() ?: ""
                    if (value.isNotBlank()) {
                        ulipNavUrls = listOf(value)
                        Log.d(TAG, "ULIP NAV URL fetched from API: $value")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch ULIP NAV URL from API, using defaults.")
            }
        }
    }
}
