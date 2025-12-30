package com.example.safetysec.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Alert Cancellation Receiver
 *
 * Handles alert cancellation broadcasts with PIN verification
 * Note: Does not use Hilt injection due to BroadcastReceiver limitations
 */
class AlertCancellationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlertCancellationReceiver"
        const val ACTION_CANCEL_ALERT = "com.example.safetysec.CANCEL_ALERT"
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_CANCELLATION_CODE = "cancellation_code"

        /**
         * Create an intent to cancel an alert
         */
        fun createCancelIntent(context: Context, alertId: String, code: String): Intent {
            return Intent(context, AlertCancellationReceiver::class.java).apply {
                action = ACTION_CANCEL_ALERT
                putExtra(EXTRA_ALERT_ID, alertId)
                putExtra(EXTRA_CANCELLATION_CODE, code)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_ALERT) {
            return
        }

        val alertId = intent.getStringExtra(EXTRA_ALERT_ID)
        val code = intent.getStringExtra(EXTRA_CANCELLATION_CODE)

        if (alertId == null || code == null) {
            Log.e(TAG, "Missing alert ID or cancellation code")
            return
        }

        Log.d(TAG, "Attempting to cancel alert: $alertId")

        // Use coroutine scope for async operation
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // Get alert document
                val alertRef = firestore.collection("alerts").document(alertId)
                val alertDoc = alertRef.get().await()

                if (!alertDoc.exists()) {
                    Log.e(TAG, "Alert not found: $alertId")
                    showError(context, "Alert not found")
                    return@launch
                }

                val protectedUserId = alertDoc.getString("protectedUserId")
                if (protectedUserId == null) {
                    Log.e(TAG, "Protected user ID not found")
                    showError(context, "User not found")
                    return@launch
                }

                // Get user's cancellation code
                val userDoc = firestore.collection("users").document(protectedUserId).get().await()
                val userCancellationCode = userDoc.getString("alertCancellationCode") ?: "0000"

                // Verify PIN
                if (code != userCancellationCode) {
                    Log.e(TAG, "Invalid cancellation code")
                    showError(context, "Invalid PIN code")
                    return@launch
                }

                // Check if alert can still be cancelled (10-second window)
                val timestamp = alertDoc.getTimestamp("timestamp")?.toDate()
                val now = java.util.Date()
                val timeDiff = now.time - (timestamp?.time ?: 0)

                if (timeDiff > 10000) { // 10 seconds
                    Log.e(TAG, "Cancellation window expired")
                    showError(context, "Cancellation window expired")
                    return@launch
                }

                // Update alert as cancelled
                alertRef.update(
                    mapOf(
                        "cancelled" to true,
                        "cancelledAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()

                Log.d(TAG, "Alert cancelled successfully: $alertId")
                showSuccess(context, "Alert cancelled successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Exception while cancelling alert", e)
                showError(context, e.message ?: "Unknown error")
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Show success message
     */
    private fun showSuccess(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show error message
     */
    private fun showError(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Failed to cancel: $message", Toast.LENGTH_SHORT).show()
        }
    }
}