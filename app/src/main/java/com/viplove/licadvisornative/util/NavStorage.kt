package com.viplove.licadvisornative.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viplove.licadvisornative.model.NavData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages the daily caching of NAV data and calculated CAGRs using SharedPreferences.
 * This implements the "fetch once a day" logic.
 */
object NavStorage {

    private const val PREFS_NAME = "NavCache"
    private const val KEY_LAST_FETCH_DATE = "lastFetchDate"
    private const val KEY_NAV_DATA_JSON = "navDataJson"
    private const val KEY_CAGR_OPTIONS_JSON = "cagrOptionsJson"

    private val gson = Gson()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the last fetch was today.
     */
    fun isCacheValid(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastFetchDate = prefs.getString(KEY_LAST_FETCH_DATE, null)
        val todayDate = dateFormatter.format(Date())

        if (lastFetchDate == todayDate) {
            Log.d("NavStorage", "NAV cache is valid (fetched today: $todayDate)")
            return true
        }
        Log.d("NavStorage", "NAV cache is stale (last fetch: $lastFetchDate, today: $todayDate)")
        return false
    }

    /**
     * Saves the fetched NAVs, calculated CAGRs, and today's date.
     */
    suspend fun saveData(
        context: Context,
        navs: List<NavData>,
        cagrs: Map<String, List<Pair<String, Double>>> // Key is SFIN
    ) {
        withContext(Dispatchers.IO) {
            try {
                val navsJson = gson.toJson(navs)
                val cagrsJson = gson.toJson(cagrs)
                val todayDate = dateFormatter.format(Date())

                getPrefs(context).edit()
                    .putString(KEY_LAST_FETCH_DATE, todayDate)
                    .putString(KEY_NAV_DATA_JSON, navsJson)
                    .putString(KEY_CAGR_OPTIONS_JSON, cagrsJson)
                    .apply()
                Log.d("NavStorage", "Successfully saved NAVs and CAGRs to cache.")
            } catch (e: Exception) {
                Log.e("NavStorage", "Failed to save NAV/CAGR cache", e)
            }
        }
    }

    /**
     * Loads the cached NAV list.
     */
    suspend fun loadNavs(context: Context): List<NavData> {
        return withContext(Dispatchers.IO) {
            try {
                val json = getPrefs(context).getString(KEY_NAV_DATA_JSON, null)
                if (json != null) {
                    val type = object : TypeToken<List<NavData>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("NavStorage", "Failed to load NAVs from cache", e)
                emptyList()
            }
        }
    }

    /**
     * Loads the cached CAGR options map. The key is the SFIN of the fund.
     */
    suspend fun loadCagrOptions(context: Context): Map<String, List<Pair<String, Double>>> {
        return withContext(Dispatchers.IO) {
            try {
                val json = getPrefs(context).getString(KEY_CAGR_OPTIONS_JSON, null)
                if (json != null) {
                    val type = object : TypeToken<Map<String, List<Pair<String, Double>>>>() {}.type
                    gson.fromJson(json, type) ?: emptyMap()
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e("NavStorage", "Failed to load CAGRs from cache", e)
                emptyMap()
            }
        }
    }
}