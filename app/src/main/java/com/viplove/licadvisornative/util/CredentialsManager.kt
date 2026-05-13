package com.viplove.licadvisornative.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.viplove.licadvisornative.ui.viewmodel.LoginViewModel

/**
 * A secure class to manage saving and loading user credentials.
 * This uses EncryptedSharedPreferences to keep data safe.
 */
object CredentialsManager {

    private const val PREFERENCES_FILE_NAME = "secure_user_prefs"
    private const val KEY_EMAIL = "key_email"
    private const val KEY_PASSWORD = "key_password"
    private const val KEY_REMEMBER_ME = "key_remember_me"

    private fun getEncryptedSharedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFERENCES_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    /**
     * Saves the user's credentials securely.
     */
    fun saveCredentials(context: Context, state: LoginViewModel.LoginCredentials) {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        with(sharedPrefs.edit()) {
            putString(KEY_EMAIL, state.email)
            putString(KEY_PASSWORD, state.password)
            putBoolean(KEY_REMEMBER_ME, state.rememberMe)
            apply()
        }
    }

    /**
     * Loads the last saved credentials.
     */
    fun loadCredentials(context: Context): LoginViewModel.LoginCredentials {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        val email = sharedPrefs.getString(KEY_EMAIL, "") ?: ""
        val password = sharedPrefs.getString(KEY_PASSWORD, "") ?: ""
        val rememberMe = sharedPrefs.getBoolean(KEY_REMEMBER_ME, false)

        // Only return email/pass if rememberMe was true
        return if (rememberMe) {
            LoginViewModel.LoginCredentials(email, password, true)
        } else {
            LoginViewModel.LoginCredentials("", "", false)
        }
    }

    /**
     * Clears all saved credentials.
     */
    fun clearCredentials(context: Context) {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        with(sharedPrefs.edit()) {
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER_ME)
            apply()
        }
    }
}