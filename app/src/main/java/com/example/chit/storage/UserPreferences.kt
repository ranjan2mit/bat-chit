package com.example.chit.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "user_prefs")

    companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_PHONE = stringPreferencesKey("phone")
        val KEY_NAME = stringPreferencesKey("name")
        val KEY_PROFILE_IMG_URI = stringPreferencesKey("profile_img_url")
        val KEY_OTP_VERIFIED = booleanPreferencesKey("otp_verified")
        val KEY_PROFILE_SETUP = booleanPreferencesKey("profile_setup")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    val phoneFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PHONE]
    }

    val otpVerifiedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OTP_VERIFIED] ?: false
    }
    val nameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_NAME]
    }

    val profileImageUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROFILE_IMG_URI]
    }

    val profileSetupFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROFILE_SETUP] ?: false
    }
    suspend fun saveUserData(token: String, phone: String, isOtpVerified: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_PHONE] = phone
            prefs[KEY_OTP_VERIFIED] = isOtpVerified
        }
    }

    suspend fun clearUserData() {
        context.dataStore.edit { it.clear() }
    }
}
