package com.webssh.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "webssh_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
        private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        private val SAVED_USERNAME_KEY = stringPreferencesKey("saved_username")
        private val SAVED_PASSWORD_KEY = stringPreferencesKey("saved_password")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    val token: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BASE_URL_KEY] ?: "http://192.168.100.20:3000"
    }

    val rememberMe: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[REMEMBER_ME_KEY] ?: false
    }

    val savedUsername: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SAVED_USERNAME_KEY] ?: ""
    }

    val savedPassword: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SAVED_PASSWORD_KEY] ?: ""
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }

    suspend fun getToken(): String? {
        return token.first()
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[BASE_URL_KEY] = url
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }

    suspend fun saveCredentials(username: String, password: String, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REMEMBER_ME_KEY] = remember
            if (remember) {
                prefs[SAVED_USERNAME_KEY] = username
                prefs[SAVED_PASSWORD_KEY] = password
            } else {
                prefs.remove(SAVED_USERNAME_KEY)
                prefs.remove(SAVED_PASSWORD_KEY)
            }
        }
    }

    suspend fun getRememberMe(): Boolean {
        return rememberMe.first()
    }

    suspend fun getSavedUsername(): String {
        return savedUsername.first()
    }

    suspend fun getSavedPassword(): String {
        return savedPassword.first()
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun getBiometricEnabled(): Boolean {
        return biometricEnabled.first()
    }
}
