package com.example.safetysec.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Alert Cancellation Receiver
 *
 * Handles alert cancellation requests from notifications
 */
class AlertCancellationReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AlertCancellationReceiver"
        const val ACTION_CANCEL_ALERT = "CANCEL_ALERT"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_CANCEL_ALERT) {
            val alertId = intent.getStringExtra("ALERT_ID")

            if (alertId != null && context != null) {
                scope.launch {
                    try {
                        val result = cancelAlert(alertId)
                        if (result) {
                            Log.d(TAG, "Alert $alertId cancelled successfully")
                            // Cancel the notification
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                                    as? android.app.NotificationManager
                            notificationManager?.cancel(alertId.hashCode())
                        } else {
                            Log.e(TAG, "Failed to cancel alert $alertId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cancelling alert", e)
                    }
                }
            }
        }
    }

    /**
     * Cancel alert directly using Firebase
     */
    private suspend fun cancelAlert(alertId: String): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val alertRef = firestore.collection("alerts").document(alertId)
            val alertDoc = alertRef.get().await()

            if (!alertDoc.exists()) {
                Log.e(TAG, "Alert not found: $alertId")
                return false
            }

            // Check if alert can still be cancelled
            val timestamp = alertDoc.getTimestamp("timestamp")?.toDate()
            val now = java.util.Date()
            val timeDiff = now.time - (timestamp?.time ?: 0)

            if (timeDiff > 10000) { // 10 seconds
                Log.w(TAG, "Cancellation window expired for alert: $alertId")
                return false
            }

            // Update alert as cancelled
            alertRef.update(
                mapOf(
                    "cancelled" to true,
                    "cancelledAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alert in Firebase", e)
            false
        }
    }
}