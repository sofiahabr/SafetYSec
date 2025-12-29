package com.example.safetysec.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.safetysec.service.MonitoringService

/**
 * Boot Receiver
 *
 * Restarts monitoring service when device boots up
 * if monitoring was active before shutdown
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                // Check if monitoring was previously active
                val wasMonitoringActive = checkMonitoringState(it)

                if (wasMonitoringActive) {
                    // Restart monitoring service
                    MonitoringService.startMonitoring(it)
                }
            }
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
}