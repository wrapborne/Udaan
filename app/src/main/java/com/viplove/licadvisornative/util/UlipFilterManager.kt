package com.viplove.licadvisornative.util

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages saving and loading the user's ULIP NAV filter preferences.
 */
object UlipFilterManager {

    private const val PREFERENCES_FILE_NAME = "ulip_filter_prefs"
    private const val KEY_SELECTED_PLANS = "key_selected_plans"

    private fun getSharedPreferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    /**
     * Saves the set of selected plan names to SharedPreferences.
     */
    suspend fun saveFilters(context: Context, selectedPlans: Set<String>) {
        withContext(Dispatchers.IO) {
            val sharedPrefs = getSharedPreferences(context)
            sharedPrefs.edit {
                putStringSet(KEY_SELECTED_PLANS, selectedPlans)
            }
        }
    }

    /**
     * Loads the set of selected plan names from SharedPreferences.
     * Returns null if no filters have been saved yet.
     */
    suspend fun loadFilters(context: Context): Set<String>? {
        return withContext(Dispatchers.IO) {
            val sharedPrefs = getSharedPreferences(context)
            // Retrieve the set. If it doesn't exist, return null.
            sharedPrefs.getStringSet(KEY_SELECTED_PLANS, null)
        }
    }
}