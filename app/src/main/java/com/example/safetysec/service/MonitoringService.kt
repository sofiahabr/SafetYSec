package com.example.safetysec.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.safetysec.MainActivity
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.DetectionResult
import com.example.safetysec.domain.model.SensorData
import com.example.safetysec.domain.repository.MonitoringRepository
import com.example.safetysec.receiver.AlertCancellationReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Monitoring Foreground Service
 *
 * Runs continuously to monitor sensors and evaluate safety rules
 */
@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject
    lateinit var monitoringRepository: MonitoringRepository

    @Inject
    lateinit var sensorDataCollector: SensorDataCollector

    @Inject
    lateinit var locationTracker: LocationTracker

    @Inject
    lateinit var alertNotificationService: AlertNotificationService

    @Inject
    lateinit var videoRecordingService: VideoRecordingService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var monitoringJob: Job? = null

    private var lastAlertTriggerTime = 0L
    private val alertCooldownMs = 60000L // 1 minute cooldown between alerts

    override fun onCreate() {
        super.onCreate()

        // Initialize sensor collector and location tracker
        sensorDataCollector.initialize(this)
        locationTracker.initialize(this)

        initializeVideoRecording()

        createNotificationChannel()
    }

    /**
     * Initialize video recording service with camera
     */
    private fun initializeVideoRecording() {
        if (!videoRecordingService.hasCameraPermission(this)) {
            Log.w(TAG, "Camera permission not granted - video recording disabled")
            return
        }

        serviceScope.launch {
            try {
                val result = videoRecordingService.initializeCamera(
                    context = this@MonitoringService,
                    lifecycleOwner = ProcessLifecycleOwner.get()
                )

                result.onSuccess {
                    Log.d(TAG, "Video recording initialized successfully")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to initialize video recording", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception initializing video recording", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_MONITORING -> {
                // Checking permissions before starting
                if (!hasRequiredPermissions()) {
                    Log.e(TAG, "Cannot start monitoring: missing required permissions")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_TRIGGER_PANIC -> {
                Log.d(TAG, "Panic button pressed - triggering alert with video")
                handlePanicButton()
            }
        }

        return START_STICKY
    }

    /**
     * Check if all required permissions are granted
     * Required for Android 14+ (API 34+) to start foreground service
     */
    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Android 14+ requires FOREGROUND_SERVICE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handle panic button press
     * This method ensures panic button goes through the same flow as automatic alerts
     * which includes video recording, cancellation window, and FCM notifications
     */
    private fun handlePanicButton() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Processing panic button alert")

                // Collect current sensor data
                val accelerometerData = sensorDataCollector.getAccelerometerData()
                val gyroscopeData = sensorDataCollector.getGyroscopeData()
                val activityData = sensorDataCollector.getActivityData()
                val locationData = locationTracker.getCurrentLocation()

                val sensorData = SensorData(
                    timestamp = System.currentTimeMillis(),
                    accelerometerX = accelerometerData.x,
                    accelerometerY = accelerometerData.y,
                    accelerometerZ = accelerometerData.z,
                    gyroscopeX = gyroscopeData.x,
                    gyroscopeY = gyroscopeData.y,
                    gyroscopeZ = gyroscopeData.z,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    speed = locationData.speed,
                    accuracy = locationData.accuracy,
                    activityType = activityData.type,
                    activityConfidence = activityData.confidence
                )

                // Get current monitoring state to find panic button rule
                val monitoringState = monitoringRepository.getMonitoringStateFlow().first()
                val panicRule = monitoringState.activeRules.find {
                    it.type == com.example.safetysec.domain.model.RuleType.PANIC_BUTTON
                }

                if (panicRule == null) {
                    Log.e(TAG, "No panic button rule found - cannot trigger alert")
                    return@launch
                }

                // Create detection result for panic button
                val detectionResult = DetectionResult(
                    ruleId = panicRule.id,
                    ruleType = com.example.safetysec.domain.model.RuleType.PANIC_BUTTON,
                    isTriggered = true,
                    confidence = 1.0f,
                    details = "Panic button pressed by user",
                    sensorData = sensorData
                )

                // Call triggerAlert which includes video recording!
                triggerAlert(detectionResult, monitoringState, sensorData)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle panic button", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        videoRecordingService.release()
        sensorDataCollector.release()
        locationTracker.release()
        serviceScope.cancel()
    }

    private fun startMonitoring() {
        try {
            // Save monitoring state for boot receiver
            saveMonitoringState(true)

            // Create foreground notification
            val notification = createNotification(
                "Monitoring Active",
                "SafetYSec is protecting you"
            )

            // Start foreground with proper type for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ (API 34+): Use ServiceInfo constant for location
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-13: Use standard startForeground
                startForeground(NOTIFICATION_ID, notification)
            } else {
                // Android 9 and below
                startForeground(NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "Foreground service started successfully")

            // Start monitoring loop
            monitoringJob = serviceScope.launch {
                // Start monitoring in repository
                monitoringRepository.startMonitoring()

                // Start collecting sensor data
                sensorDataCollector.startCollecting()
                locationTracker.startTracking()

                // Main monitoring loop
                while (isActive) {
                    try {
                        performMonitoringCycle()
                        delay(MONITORING_INTERVAL_MS)
                    } catch (e: Exception) {
                        // Log error but continue monitoring
                        Log.e(TAG, "Error in monitoring cycle", e)
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service", e)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting monitoring", e)
            stopSelf()
        }
    }

    private fun stopMonitoring() {
        // Save monitoring state
        saveMonitoringState(false)

        monitoringJob?.cancel()
        monitoringJob = null

        sensorDataCollector.stopCollecting()
        locationTracker.stopTracking()

        serviceScope.launch {
            monitoringRepository.stopMonitoring()
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Save monitoring state to SharedPreferences
     * Used by BootReceiver to restart monitoring after reboot
     */
    private fun saveMonitoringState(isActive: Boolean) {
        val prefs = getSharedPreferences("monitoring_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("was_monitoring_active", isActive).apply()
    }

    /**
     * Perform one monitoring cycle:
     * 1. Collect sensor data
     * 2. Get location
     * 3. Update monitoring state
     * 4. Evaluate rules
     * 5. Trigger alerts if needed
     */
    private suspend fun performMonitoringCycle() {
        // Collect current sensor data
        val accelerometerData = sensorDataCollector.getAccelerometerData()
        val gyroscopeData = sensorDataCollector.getGyroscopeData()
        val activityData = sensorDataCollector.getActivityData()
        val locationData = locationTracker.getCurrentLocation()

        // Combine into SensorData object
        val sensorData = SensorData(
            timestamp = System.currentTimeMillis(),
            accelerometerX = accelerometerData.x,
            accelerometerY = accelerometerData.y,
            accelerometerZ = accelerometerData.z,
            gyroscopeX = gyroscopeData.x,
            gyroscopeY = gyroscopeData.y,
            gyroscopeZ = gyroscopeData.z,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            speed = locationData.speed,
            accuracy = locationData.accuracy,
            activityType = activityData.type,
            activityConfidence = activityData.confidence
        )

        // Update last activity timestamp if user is active
        if (sensorData.activityType != com.example.safetysec.domain.model.ActivityType.STILL) {
            monitoringRepository.updateLastActivityTimestamp(System.currentTimeMillis())
        }

        // Update monitoring repository with new sensor data
        monitoringRepository.updateSensorData(sensorData)

        // Get current monitoring state
        val monitoringState = monitoringRepository.getMonitoringStateFlow().first()

        // Only evaluate rules if in active time window
        if (monitoringState.canEvaluateRules()) {
            // Evaluate all detection algorithms
            val detectionResults = monitoringRepository.evaluateDetections(
                sensorData,
                monitoringState.activeRules
            )

            // Check if any detection should trigger an alert
            detectionResults.forEach { result ->
                if (result.shouldTriggerAlert() && canTriggerAlert()) {
                    triggerAlert(result, monitoringState, sensorData)
                }
            }

            // Update notification with current status
            updateNotification(monitoringState, detectionResults)
        }
    }

    /**
     * Check if enough time has passed since last alert (cooldown)
     */
    private fun canTriggerAlert(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastAlertTriggerTime) >= alertCooldownMs
    }

    /**
     * Trigger an alert with cancellation window, video recording, and notifications
     */
    private suspend fun triggerAlert(
        detectionResult: DetectionResult,
        monitoringState: com.example.safetysec.domain.model.MonitoringState,
        sensorData: SensorData
    ) {
        lastAlertTriggerTime = System.currentTimeMillis()

        val rule = monitoringState.activeRules.find { it.id == detectionResult.ruleId }

        if (rule != null) {
            val alertResult = monitoringRepository.createAlert(
                detectionResult,
                rule,
                sensorData
            )

            alertResult.onSuccess { alertEvent ->
                showCancellationNotification(alertEvent)

                // Start video recording immediately
                val videoJob = if (::videoRecordingService.isInitialized &&
                    videoRecordingService.hasCameraPermission(this@MonitoringService)) {

                    serviceScope.launch {
                        try {
                            Log.d(TAG, "Starting video recording for alert: ${alertEvent.id}")

                            // Record and upload video (30 seconds)
                            val videoResult = videoRecordingService.recordAndUploadVideo(
                                context = this@MonitoringService,
                                alertId = alertEvent.id
                            )

                            videoResult.onSuccess { downloadUrl ->
                                // Check if alert was cancelled before updating with video
                                val alert = monitoringRepository.getAlertById(alertEvent.id)
                                if (alert?.isCancelled == true) {
                                    Log.d(TAG, "Alert was cancelled - not updating with video URL")
                                    // Optionally delete the video from storage here
                                } else {
                                    Log.d(TAG, "Video uploaded successfully: $downloadUrl")
                                    // Update alert with video URL
                                    monitoringRepository.updateAlertWithVideo(
                                        alertId = alertEvent.id,
                                        videoUrl = downloadUrl
                                    )
                                }
                            }.onFailure { error ->
                                Log.e(TAG, "Video recording/upload failed: ${error.message}", error)
                            }
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Video recording cancelled for alert: ${alertEvent.id}")
                            throw e  // Re-throw to properly cancel coroutine
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception during video recording", e)
                        }
                    }
                } else {
                    Log.w(TAG, "Camera permission not granted or service not initialized")
                    null
                }

                // 10-second cancellation window with grace period
                serviceScope.launch {
                    var cancelled = false

                    // Check every second for 10 seconds
                    repeat(10) { second ->
                        delay(1000)
                        Log.d(TAG, "Cancellation check ${second + 1}/10 for alert: ${alertEvent.id}")

                        val alert = monitoringRepository.getAlertById(alertEvent.id)
                        if (alert?.isCancelled == true) {
                            cancelled = true
                            Log.d(TAG, "✓ Alert ${alertEvent.id} was cancelled at ${second + 1} seconds")

                            // Cancel video recording job
                            videoJob?.cancel()
                            Log.d(TAG, "Video recording job cancelled for alert: ${alertEvent.id}")

                            // Cancel notification to protected user
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(CANCELLATION_NOTIFICATION_ID)
                            return@launch
                        }
                    }

                    // Add 2-second grace period to ensure any pending cancellations complete
                    if (!cancelled) {
                        Log.d(TAG, "Waiting 2 seconds grace period for alert: ${alertEvent.id}")
                        delay(2000)

                        // Final check
                        val alert = monitoringRepository.getAlertById(alertEvent.id)
                        if (alert?.isCancelled == true) {
                            cancelled = true
                            Log.d(TAG, "✓ Alert ${alertEvent.id} was cancelled during grace period")

                            // Cancel video recording job
                            videoJob?.cancel()
                            Log.d(TAG, "Video recording job cancelled for alert: ${alertEvent.id}")

                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(CANCELLATION_NOTIFICATION_ID)
                            return@launch
                        }
                    }

                    if (!cancelled) {
                        Log.d(TAG, "⚠ Alert ${alertEvent.id} was NOT cancelled - sending to monitors")
                        alertNotificationService.notifyMonitors(alertEvent, this@MonitoringService)
                    } else {
                        Log.d(TAG, "✓ Alert ${alertEvent.id} successfully cancelled - NOT notifying monitors")
                    }
                }
            }
        }
    }

    /**
     * Show cancellation notification to protected user
     */
    private fun showCancellationNotification(alertEvent: AlertEvent) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open MainActivity for PIN entry
        val cancelIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_CANCEL_DIALOG", true)
            putExtra("ALERT_ID", alertEvent.id)
            putExtra("ALERT_TYPE", alertEvent.type.name)
        }

        val cancelPendingIntent = PendingIntent.getActivity(
            this,
            alertEvent.id.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification with full screen intent for locked screens
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("🚨 ${alertEvent.type.name}")
            .setContentText("Alert will be sent in 10 seconds. Tap to cancel.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(cancelPendingIntent)
            .setFullScreenIntent(cancelPendingIntent, true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(CANCELLATION_NOTIFICATION_ID, notification)

        // Auto-dismiss notification after 10 seconds
        serviceScope.launch {
            delay(10000)
            notificationManager.cancel(CANCELLATION_NOTIFICATION_ID)
        }
    }

    /**
     * Create pending intent for cancelling alert
     */
    private fun createCancelAlertPendingIntent(alertId: String): PendingIntent {
        val intent = Intent(this, AlertCancellationReceiver::class.java).apply {
            action = AlertCancellationReceiver.ACTION_CANCEL_ALERT  //
            putExtra(AlertCancellationReceiver.EXTRA_ALERT_ID, alertId)
        }

        return PendingIntent.getBroadcast(
            this,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Update the foreground notification with current status
     */
    private fun updateNotification(
        monitoringState: com.example.safetysec.domain.model.MonitoringState,
        detectionResults: List<DetectionResult>
    ) {
        val activeDetections = detectionResults.count { it.isTriggered }

        val title = if (activeDetections > 0) {
            "$activeDetections Detection(s)"
        } else {
            "Monitoring Active"
        }

        val message = monitoringState.getStatusMessage()

        val notification = createNotification(title, message)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show alert notification
     */
    private fun showAlertNotification(title: String, message: String, alertId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ALERT_ID", alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(title: String, message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Monitoring channel
            val monitoringChannel = NotificationChannel(
                CHANNEL_ID,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows monitoring status"
                setShowBadge(false)
            }

            // Alert channel
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Safety detection alerts"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(monitoringChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        private const val TAG = "MonitoringService"
        const val ACTION_START_MONITORING = "com.example.safetysec.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.safetysec.STOP_MONITORING"
        const val ACTION_TRIGGER_PANIC = "com.example.safetysec.TRIGGER_PANIC"

        private const val NOTIFICATION_ID = 1001
        private const val CANCELLATION_NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "monitoring_service"
        private const val ALERT_CHANNEL_ID = "safety_alerts"

        private const val MONITORING_INTERVAL_MS = 2000L // 2 seconds

        /**
         * Helper function to start monitoring service
         */
        fun startMonitoring(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start monitoring service", e)
            }
        }

        /**
         * Helper function to stop monitoring service
         */
        fun stopMonitoring(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        /**
         * Trigger panic button with video recording
         * This ensures the panic button goes through MonitoringService
         * so video recording is triggered properly
         */
        fun triggerPanic(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_TRIGGER_PANIC
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Panic button trigger sent to MonitoringService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger panic button", e)
            }
        }
    }
}