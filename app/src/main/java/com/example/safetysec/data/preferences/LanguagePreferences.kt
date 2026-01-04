package com.example.safetysec.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

/**
 * Language codes supported by the app
 */
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    NORWEGIAN("no", "Norsk (Bokmål)"),
    PORTUGUESE("pt", "Português");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * Manages language preferences for the app
 */
class LanguagePreferences(private val context: Context) {

    private val dataStore = context.languageDataStore

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
    }

    /**
     * Get the current language as a Flow
     */
    val currentLanguage: Flow<AppLanguage> = dataStore.data
        .map { preferences ->
            val languageCode = preferences[LANGUAGE_KEY] ?: "en"
            AppLanguage.fromCode(languageCode)
        }

    /**
     * Set the app language
     */
    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }

    /**
     * Get the current language code synchronously
     */
    suspend fun getCurrentLanguageCode(): String {
        var code = "en"
        dataStore.data.collect { preferences ->
            code = preferences[LANGUAGE_KEY] ?: "en"
        }
        return code
    }
}