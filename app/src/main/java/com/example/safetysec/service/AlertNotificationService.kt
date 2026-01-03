package com.example.safetysec.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.safetysec.MainActivity
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alert Notification Service
 *
 * Handles push notifications for alerts sent to monitors
 * Uses Firebase Cloud Messaging (FCM)
 */
@Singleton
class AlertNotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) {
    companion object {
        private const val TAG = "AlertNotificationService"
        private const val ALERT_CHANNEL_ID = "safety_alerts"
        private const val ALERT_CHANNEL_NAME = "Safety Alerts"
    }

    /**
     * Send push notifications to all monitors
     * @param alert Alert event to notify about
     * @param context Application context for showing local notification
     */
    suspend fun notifyMonitors(alert: AlertEvent, context: Context) {
        try {
            // Get FCM tokens for all monitors
            val monitorTokens = getMonitorFCMTokens(alert.monitorIds)

            // Send notification to each monitor
            monitorTokens.forEach { (monitorId, token) ->
                sendPushNotification(
                    token = token,
                    alert = alert,
                    context = context
                )
            }

            // Also show local notification if this device is a monitor
            if (alert.monitorIds.contains(getCurrentUserId())) {
                showLocalNotification(alert, context)
            }

            Log.d(TAG, "Notifications sent to ${monitorTokens.size} monitors")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notifications", e)
        }
    }

    /**
     * Send push notification via FCM
     */
    private suspend fun sendPushNotification(
        token: String,
        alert: AlertEvent,
        context: Context
    ) {
        try {
            // Note: Direct FCM API calls require server-side implementation
            // This is a simplified version - in production, use Cloud Functions or your backend

            val data = mapOf(
                "alertId" to alert.id,
                "type" to alert.type.name,
                "protectedUserName" to alert.protectedUserName,
                "timestamp" to alert.timestamp.toString(),
                "latitude" to alert.latitude.toString(),
                "longitude" to alert.longitude.toString(),
                "details" to (alert.details ?: "")
            )

            // Store notification in Firestore for monitoring apps to poll
            // (Alternative to direct FCM when you don't have a backend)
            storeNotificationInFirestore(alert)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending push notification", e)
        }
    }

    /**
     * Store notification in Firestore as fallback mechanism
     */
    private suspend fun storeNotificationInFirestore(alert: AlertEvent) {
        try {
            val notificationData = hashMapOf(
                "alertId" to alert.id,
                "type" to alert.type.name,
                "protectedUserId" to alert.protectedUserId,
                "protectedUserName" to alert.protectedUserName,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "monitorIds" to alert.monitorIds,
                "read" to false
            )

            firestore.collection("notifications")
                .add(notificationData)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error storing notification in Firestore", e)
        }
    }

    /**
     * Show local notification
     */
    private fun showLocalNotification(alert: AlertEvent, context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Safety detection alerts"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for notification tap
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ALERT_ID", alert.id)
            putExtra("OPEN_ALERTS", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 ${alert.type.toDisplayString()}")
            .setContentText("${alert.protectedUserName} needs attention")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${alert.protectedUserName} - ${alert.type.toDisplayString()}\n${alert.details ?: ""}\nLocation: ${alert.getFormattedLocation()}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .build()

        notificationManager.notify(alert.id.hashCode(), notification)
    }

    /**
     * Get FCM tokens for monitors from Firestore
     */
    private suspend fun getMonitorFCMTokens(monitorIds: List<String>): Map<String, String> {
        val tokens = mutableMapOf<String, String>()

        try {
            for (monitorId in monitorIds) {
                val doc = firestore.collection("users")
                    .document(monitorId)
                    .get()
                    .await()

                val token = doc.getString("fcmToken")
                if (!token.isNullOrBlank()) {
                    tokens[monitorId] = token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM tokens", e)
        }

        return tokens
    }

    /**
     * Get current user ID (simplified - should come from auth service)
     */
    private fun getCurrentUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    /**
     * Register FCM token for current user
     */
    suspend fun registerFCMToken(userId: String) {
        try {
            val token = messaging.token.await()

            // Store token in Firestore
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()

            Log.d(TAG, "FCM token registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering FCM token", e)
        }
    }

    /**
     * Clear FCM token on logout
     */
    suspend fun clearFCMToken(userId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", null)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FCM token", e)
        }
    }
}