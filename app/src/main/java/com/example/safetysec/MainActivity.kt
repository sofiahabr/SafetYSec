package com.example.safetysec

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.example.safetysec.data.preferences.ThemeMode
import com.example.safetysec.data.preferences.ThemePreferences
import com.example.safetysec.presentation.navigation.AppNavHost
import com.example.safetysec.presentation.theme.SafetYSecTheme


/**
 * Main Activity for SafetYSec application
 *
 * This is the single activity that hosts all Compose screens.
 * Uses Hilt for dependency injection.
 * Supports dynamic theme switching (Light/Dark/System).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent?.let { handleAlertIntent(it) }

        // Initialize theme preferences
        val themePreferences = ThemePreferences(this)

        setContent {
            // Collect the current theme preference
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            // Determine if dark theme should be used
            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SafetYSecTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAlertIntent(intent)
    }

    private fun handleAlertIntent(intent: Intent) {
        val showCancellation = intent.getBooleanExtra("SHOW_CANCELLATION_DIALOG", false)
        val alertId = intent.getStringExtra("ALERT_ID")
        val openAlerts = intent.getBooleanExtra("OPEN_ALERTS", false)

        // Handle navigation based on intent extras
        // TODO: Implement navigation logic when needed
        // For example:
        // if (showCancellation && alertId != null) {
        //     // Show cancellation dialog
        // } else if (openAlerts) {
        //     // Navigate to alerts screen
        // }
    }
}