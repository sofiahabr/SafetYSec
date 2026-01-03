package com.example.safetysec.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.safetysec.service.MonitoringService

/**
 * Boot Receiver
 *
 * Restarts monitoring service when device boots up
 * if monitoring was active before shutdown
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                Log.d(TAG, "Boot completed - checking if monitoring should restart")

                // Check if monitoring was previously active
                val wasMonitoringActive = checkMonitoringState(it)

                if (wasMonitoringActive) {
                    // CRITICAL: Check permissions before starting service
                    if (hasRequiredPermissions(it)) {
                        Log.d(TAG, "Permissions granted - restarting monitoring service")
                        MonitoringService.startMonitoring(it)
                    } else {
                        Log.w(TAG, "Cannot restart monitoring: required permissions not granted")
                        Log.w(TAG, "User must open app and grant permissions to re-enable monitoring")

                        // Clear monitoring state since we can't auto-restart
                        clearMonitoringState(it)
                    }
                } else {
                    Log.d(TAG, "Monitoring was not active before reboot - not starting service")
                }
            }
        }
    }

    /**
     * Check if all required permissions are granted
     * CRITICAL: Must check permissions before starting MonitoringService
     */
    private fun hasRequiredPermissions(context: Context): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // Android 13+ requires notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Android 14+ requires foreground service location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        // Check all permissions
        return requiredPermissions.all { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.d(TAG, "Permission not granted: $permission")
            }

            granted
        }
    }

    /**
     * Check if monitoring was active before reboot
     * Uses SharedPreferences to track monitoring state
     */
    private fun checkMonitoringState(context: Context): Boolean {
        val prefs = context.getSharedPreferences("monitoring_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("was_monitoring_active", false)
    }

    /**
     * Clear monitoring state
     * Called when we can't auto-restart due to missing permissions
     */
    private fun clearMonitoringState(context: Context) {
        val prefs = context.getSharedPreferences("monitoring_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("was_monitoring_active", false)
            .apply()
        Log.d(TAG, "Cleared monitoring state - user must manually re-enable in app")
    }
}