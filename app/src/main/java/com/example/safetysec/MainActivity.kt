package com.example.safetysec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.example.safetysec.data.preferences.ThemeMode
import com.example.safetysec.data.preferences.ThemePreferences
import com.example.safetysec.domain.model.AlertType
import com.example.safetysec.presentation.components.AlertCancellationDialog
import com.example.safetysec.presentation.navigation.AppNavHost
import com.example.safetysec.presentation.theme.SafetYSecTheme
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.receiver.AlertCancellationReceiver

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            android.util.Log.d("MainActivity", "Permission ${it.key} granted: ${it.value}")
        }
    }

    // Alert cancellation state
    private val showCancelDialog = mutableStateOf(false)
    private val alertToCancel = mutableStateOf<Pair<String, AlertType>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle cancellation intent if present
        intent?.let { handleAlertIntent(it) }

        // Request all permissions on startup
        requestAllPermissions()

        // Initialize theme preferences
        val themePreferences = ThemePreferences(this)

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main navigation
                        val navController = rememberNavController()
                        AppNavHost(navController = navController)

                        // Alert cancellation dialog overlay
                        AlertCancellationOverlay()
                    }
                }
            }
        }
    }

    @Composable
    private fun AlertCancellationOverlay() {
        val authViewModel: AuthViewModel = hiltViewModel()
        val authState by authViewModel.authState.collectAsState()
        val showDialog by showCancelDialog
        val alertData by alertToCancel

        // Show cancellation dialog if needed
        if (showDialog && alertData != null) {
            val (alertId, alertType) = alertData!!
            val userPin = authState.user?.alertCancellationCode ?: "0000"

            AlertCancellationDialog(
                alertId = alertId,
                alertType = alertType,
                userCancellationCode = userPin,
                onDismiss = {
                    showCancelDialog.value = false
                    alertToCancel.value = null
                },
                onCancel = { enteredPin ->
                    // Handle cancellation
                    handleAlertCancellation(alertId, enteredPin)
                    showCancelDialog.value = false
                    alertToCancel.value = null
                }
            )
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Add background location for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // Check which permissions are not granted
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Important: Update the intent
        handleAlertIntent(intent)
    }

    private fun handleAlertIntent(intent: Intent) {
        // Check if this is a cancellation dialog request
        val showCancellation = intent.getBooleanExtra("SHOW_CANCEL_DIALOG", false)

        if (showCancellation) {
            val alertId = intent.getStringExtra("ALERT_ID")
            val alertTypeString = intent.getStringExtra("ALERT_TYPE")

            if (alertId != null && alertTypeString != null) {
                try {
                    val alertType = AlertType.valueOf(alertTypeString)

                    // Update state to show dialog
                    alertToCancel.value = alertId to alertType
                    showCancelDialog.value = true

                    android.util.Log.d("MainActivity", "Showing cancellation dialog for alert: $alertId")
                } catch (e: IllegalArgumentException) {
                    android.util.Log.e("MainActivity", "Invalid alert type: $alertTypeString", e)
                }
            }
        }

        // Handle navigation to alerts screen
        val openAlerts = intent.getBooleanExtra("OPEN_ALERTS", false)
        if (openAlerts) {
            // Navigation will be handled by the NavHost when it reads the ALERT_ID
            android.util.Log.d("MainActivity", "Navigate to alerts screen requested")
        }
    }

    /**
     * Cancel alert directly in Firestore (no broadcast needed)
     * SIMPLIFIED: Avoids Android 14+ broadcast permission issues
     */
    private fun handleAlertCancellation(alertId: String, pin: String) {
        try {
            android.util.Log.d("MainActivity", "Cancelling alert: $alertId with PIN")

            // Get Firestore instance
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

            // Get current user ID
            val userId = auth.currentUser?.uid
            if (userId == null) {
                android.util.Log.e("MainActivity", "User not authenticated")
                android.widget.Toast.makeText(this, "Not authenticated", android.widget.Toast.LENGTH_LONG).show()
                return
            }

            // Get alert document
            firestore.collection("alerts").document(alertId).get()
                .addOnSuccessListener { alertDoc ->
                    if (!alertDoc.exists()) {
                        android.util.Log.e("MainActivity", "Alert not found: $alertId")
                        android.widget.Toast.makeText(this, "Alert not found", android.widget.Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Check timing window (10 seconds)
                    val timestamp = alertDoc.getTimestamp("timestamp")?.toDate()
                    val now = java.util.Date()
                    val timeDiff = now.time - (timestamp?.time ?: 0)

                    if (timeDiff > 10000) {
                        android.util.Log.e("MainActivity", "Cancellation window expired")
                        android.widget.Toast.makeText(this, "Cancellation window expired", android.widget.Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Get user's saved PIN
                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val savedPin = userDoc.getString("alertCancellationCode") ?: "0000"

                            // Verify PIN
                            if (pin != savedPin) {
                                android.util.Log.e("MainActivity", "Invalid PIN")
                                android.widget.Toast.makeText(this, "Incorrect PIN", android.widget.Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // PIN is correct - cancel the alert
                            firestore.collection("alerts").document(alertId)
                                .update(
                                    mapOf(
                                        "cancelled" to true,
                                        "cancelledAt" to com.google.firebase.Timestamp.now()
                                    )
                                )
                                .addOnSuccessListener {
                                    android.util.Log.d("MainActivity", "Alert cancelled successfully")
                                    android.widget.Toast.makeText(this, "Alert cancelled successfully", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("MainActivity", "Failed to cancel alert", e)
                                    android.widget.Toast.makeText(this, "Failed to cancel: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("MainActivity", "Failed to get user PIN", e)
                            android.widget.Toast.makeText(this, "Failed to verify PIN", android.widget.Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MainActivity", "Failed to get alert", e)
                    android.widget.Toast.makeText(this, "Failed to load alert", android.widget.Toast.LENGTH_LONG).show()
                }

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Exception during cancellation", e)
            android.widget.Toast.makeText(this, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}