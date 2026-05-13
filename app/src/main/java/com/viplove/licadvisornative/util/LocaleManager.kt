// File: app/src/main/java/com/viplove/licadvisornative/util/LocaleManager.kt
package com.viplove.licadvisornative.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {

    /**
     * Updates the application's locale and restarts the current activity to apply the change.
     * @param context The current context, which should be an Activity.
     * @param languageCode The ISO 639-1 code for the desired language (e.g., "en", "hi").
     */
    fun updateAppLocale(context: Context, languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)

        /**
         * CORRECTED: Instead of finishing and restarting the activity with an intent,
         * we now use the `recreate()` method. This is a safer and more standard way
         * to have an activity reload its configuration and resources, reducing the
         * risk of losing state.
         */
        if (context is Activity) {
            context.recreate()
        }
    }
}
