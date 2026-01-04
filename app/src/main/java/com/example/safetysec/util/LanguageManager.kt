package com.example.safetysec.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.safetysec.data.preferences.AppLanguage
import java.util.Locale

/**
 * Manages app language/locale changes across different Android versions
 */
object LanguageManager {

    /**
     * Set app language - handles different Android versions appropriately
     *
     * @param context Application context
     * @param language The language to set
     * @param activity Optional activity to recreate (for Android 12 and below)
     */
    fun setAppLanguage(context: Context, language: AppLanguage, activity: Activity? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Use system locale API
            setLanguageApi33(context, language)
        } else {
            // Android 12 and below - Use AppCompat
            setLanguageCompat(language, activity)
        }
    }

    /**
     * Android 13+ (API 33+) - Use per-app language preferences
     */
    private fun setLanguageApi33(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = LocaleList.forLanguageTags(language.code)
        }
    }

    /**
     * Android 12 and below - Use AppCompat locale
     */
    private fun setLanguageCompat(language: AppLanguage, activity: Activity?) {
        val locale = Locale(language.code)
        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)

        // Recreate activity to apply changes
        activity?.recreate()
    }

    /**
     * Get current app locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = localeManager.applicationLocales
            if (!locales.isEmpty) {
                locales[0] ?: Locale.getDefault()
            } else {
                Locale.getDefault()
            }
        } else {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (!locales.isEmpty) {
                locales[0] ?: Locale.getDefault()
            } else {
                Locale.getDefault()
            }
        }
    }

    /**
     * Get AppLanguage from current locale
     */
    fun getCurrentAppLanguage(context: Context): AppLanguage {
        val locale = getCurrentLocale(context)
        return when (locale.language) {
            "no" -> AppLanguage.NORWEGIAN
            "pt" -> AppLanguage.PORTUGUESE
            else -> AppLanguage.ENGLISH
        }
    }

    /**
     * Create a context with the specified locale (for updating configuration)
     * Use this if you need to update resources manually
     */
    fun createLocalizedContext(context: Context, language: AppLanguage): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}