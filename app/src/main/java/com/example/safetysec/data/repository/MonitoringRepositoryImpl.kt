package com.example.safetysec.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.safetysec.domain.model.*
import com.example.safetysec.domain.repository.MonitoringRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Monitoring Repository Implementation
 *
 * Implements monitoring logic and detection algorithms
 */
@Singleton
class MonitoringRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : MonitoringRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    private val _monitoringState = MutableStateFlow(MonitoringState())

    private var lastActivityTimestamp: Long = System.currentTimeMillis()

    // Sensor data history for pattern detection
    private val sensorDataHistory = mutableListOf<SensorData>()
    private val maxHistorySize = 50

    // Previous speed for accident detection
    private var previousSpeed: Float? = null
    private var previousSpeedTimestamp: Long = 0L

    override fun getMonitoringStateFlow(): Flow<MonitoringState> {
        return _monitoringState.asStateFlow()
    }

    override suspend fun startMonitoring(): Result<Boolean> {
        return try {
            val activeRules = getActiveRules(currentUserId)
            val activeTimeWindows = getActiveTimeWindows(currentUserId)

            _monitoringState.value = _monitoringState.value.copy(
                isRunning = true,
                protectedUserId = currentUserId,
                activeRules = activeRules,
                activeTimeWindows = activeTimeWindows,
                lastUpdateTime = Date()
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopMonitoring(): Result<Boolean> {
        return try {
            _monitoringState.value = MonitoringState(
                isRunning = false,
                protectedUserId = currentUserId
            )

            sensorDataHistory.clear()
            previousSpeed = null

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSensorData(sensorData: SensorData) {
        // Add to history
        sensorDataHistory.add(sensorData)
        if (sensorDataHistory.size > maxHistorySize) {
            sensorDataHistory.removeAt(0)
        }

        // Update monitoring state
        val isInWindow = isInActiveTimeWindow(_monitoringState.value.activeTimeWindows)

        _monitoringState.value = _monitoringState.value.copy(
            currentSensorData = sensorData,
            isInActiveTimeWindow = isInWindow,
            lastUpdateTime = Date(),
            lastLocationUpdate = if (sensorData.hasLocation()) Date() else _monitoringState.value.lastLocationUpdate,
            lastActivityUpdate = if (sensorData.activityType != ActivityType.UNKNOWN) Date() else _monitoringState.value.lastActivityUpdate
        )
    }

    override suspend fun getActiveRules(protectedUserId: String): List<Rule> {
        return try {
            val querySnapshot = firestore
                .collection("rules")
                .whereEqualTo("protectedId", protectedUserId)
                .whereEqualTo("status", "AUTHORIZED")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    Rule(
                        id = doc.id,
                        monitorId = doc.getString("monitorId") ?: "",
                        monitorName = doc.getString("monitorName") ?: "",
                        protectedId = doc.getString("protectedId") ?: "",
                        protectedName = doc.getString("protectedName") ?: "",
                        type = RuleType.valueOf(doc.getString("type") ?: "PANIC_BUTTON"),
                        parameters = parseRuleParameters(doc.get("parameters") as? Map<String, Any>),
                        status = RuleStatus.AUTHORIZED,
                        createdAt = doc.getDate("createdAt") ?: Date(),
                        updatedAt = doc.getDate("updatedAt") ?: Date(),
                        authorizedAt = doc.getDate("authorizedAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getActiveTimeWindows(protectedUserId: String): List<TimeWindow> {
        return try {
            val querySnapshot = firestore
                .collection("timeWindows")
                .whereEqualTo("protectedId", protectedUserId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    val daysOfWeekList = (doc.get("daysOfWeek") as? List<String>)?.mapNotNull {
                        try {
                            DayOfWeek.valueOf(it)
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    TimeWindow(
                        id = doc.id,
                        protectedId = doc.getString("protectedId") ?: "",
                        monitorId = doc.getString("monitorId") ?: "",
                        monitorName = doc.getString("monitorName") ?: "",
                        daysOfWeek = daysOfWeekList,
                        startTime = doc.getString("startTime") ?: "00:00",
                        endTime = doc.getString("endTime") ?: "23:59",
                        isActive = doc.getBoolean("isActive") ?: false,
                        createdAt = doc.getDate("createdAt") ?: Date(),
                        updatedAt = doc.getDate("updatedAt") ?: Date()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun isInActiveTimeWindow(timeWindows: List<TimeWindow>): Boolean {
        if (timeWindows.isEmpty()) return true // If no time windows, always active
        return timeWindows.any { it.isCurrentlyActive() }
    }

    override suspend fun evaluateDetections(
        sensorData: SensorData,
        rules: List<Rule>
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        rules.forEach { rule ->
            val result = when (rule.type) {
                RuleType.FALL_DETECTION -> detectFall(sensorData, rule)
                RuleType.ACCIDENT_DETECTION -> detectAccident(sensorData, rule)
                RuleType.GEOFENCING -> detectGeofenceViolation(sensorData, rule)
                RuleType.SPEED_CONTROL -> detectSpeedViolation(sensorData, rule)
                RuleType.PROLONGED_INACTIVITY -> detectProlongedInactivity(sensorData, rule)
                RuleType.PANIC_BUTTON -> null // Handled separately by user action
            }

            result?.let { results.add(it) }
        }

        _monitoringState.value = _monitoringState.value.copy(
            detectionResults = results
        )

        return results
    }

    /**
     * Fall Detection Algorithm
     * Detects sudden acceleration changes and orientation changes
     */
    private fun detectFall(sensorData: SensorData, rule: Rule): DetectionResult {
        val totalAcceleration = sensorData.getTotalAcceleration()
        val totalRotation = sensorData.getTotalRotation()

        // Fall characteristics:
        // 1. High acceleration (> 2.5g = 24.5 m/s²)
        // 2. Followed by low acceleration (free fall)
        // 3. High rotation (orientation change)

        val highAcceleration = totalAcceleration > 24.5f
        val highRotation = totalRotation > 3.0f // rad/s

        // Check history for free fall pattern
        val hasFreefall = if (sensorDataHistory.size >= 5) {
            val recent = sensorDataHistory.takeLast(5)
            recent.any { it.getTotalAcceleration() < 5.0f }
        } else false

        val isTriggered = highAcceleration && (highRotation || hasFreefall)
        val confidence = when {
            highAcceleration && highRotation && hasFreefall -> 0.95f
            highAcceleration && highRotation -> 0.8f
            highAcceleration && hasFreefall -> 0.75f
            highAcceleration -> 0.6f
            else -> 0.0f
        }

        return DetectionResult(
            ruleId = rule.id,
            ruleType = RuleType.FALL_DETECTION,
            isTriggered = isTriggered,
            confidence = confidence,
            details = "Acceleration: ${String.format("%.2f", totalAcceleration)} m/s², Rotation: ${String.format("%.2f", totalRotation)} rad/s",
            sensorData = sensorData
        )
    }

    /**
     * Accident Detection Algorithm
     * Detects sudden deceleration (> 30 km/h loss in < 1 second)
     */
    private fun detectAccident(sensorData: SensorData, rule: Rule): DetectionResult {
        val currentSpeed = sensorData.getSpeedKmh()
        val currentTime = sensorData.timestamp

        val isTriggered = if (currentSpeed != null && previousSpeed != null) {
            val timeDelta = (currentTime - previousSpeedTimestamp) / 1000.0f // seconds

            if (timeDelta > 0 && timeDelta < 2.0f) {
                val speedDelta = previousSpeed!! - currentSpeed
                val deceleration = speedDelta / timeDelta

                // Sudden deceleration: > 30 km/h in 1 second
                speedDelta > 30f && timeDelta < 1.5f
            } else false
        } else false

        // Also check accelerometer for impact
        val totalAcceleration = sensorData.getTotalAcceleration()
        val hasImpact = totalAcceleration > 30f // High impact force

        val confidence = when {
            isTriggered && hasImpact -> 0.9f
            isTriggered -> 0.75f
            hasImpact -> 0.6f
            else -> 0.0f
        }

        // Update previous speed
        if (currentSpeed != null) {
            previousSpeed = currentSpeed
            previousSpeedTimestamp = currentTime
        }

        return DetectionResult(
            ruleId = rule.id,
            ruleType = RuleType.ACCIDENT_DETECTION,
            isTriggered = isTriggered || hasImpact,
            confidence = confidence,
            details = "Speed: ${currentSpeed?.let { String.format("%.1f", it) } ?: "N/A"} km/h, Impact: ${String.format("%.2f", totalAcceleration)} m/s²",
            sensorData = sensorData
        )
    }

    /**
     * Geofencing Detection Algorithm
     * Checks if user is outside defined areas
     */
    private fun detectGeofenceViolation(sensorData: SensorData, rule: Rule): DetectionResult? {
        if (!sensorData.hasLocation()) return null

        val areas = rule.parameters.geofenceAreas
        if (areas.isEmpty()) return null

        val userLat = sensorData.latitude!!
        val userLon = sensorData.longitude!!

        // Check if user is inside ANY of the defined areas
        val isInsideAnyArea = areas.any { area ->
            val distance = calculateDistance(
                userLat, userLon,
                area.latitude, area.longitude
            )
            distance <= area.radius
        }

        val isTriggered = !isInsideAnyArea

        // Find closest area
        val closestArea = areas.minByOrNull { area ->
            calculateDistance(userLat, userLon, area.latitude, area.longitude)
        }

        val closestDistance = closestArea?.let { area ->
            calculateDistance(userLat, userLon, area.latitude, area.longitude)
        } ?: 0.0

        return DetectionResult(
            ruleId = rule.id,
            ruleType = RuleType.GEOFENCING,
            isTriggered = isTriggered,
            confidence = if (isTriggered) 0.95f else 0.0f,
            details = "Outside all ${areas.size} area(s). Closest: ${closestArea?.name ?: "N/A"} (${String.format("%.0f", closestDistance)}m away)",
            sensorData = sensorData
        )
    }

    /**
     * Speed Control Detection Algorithm
     * Detects when speed exceeds maximum limit
     */
    private fun detectSpeedViolation(sensorData: SensorData, rule: Rule): DetectionResult? {
        val currentSpeed = sensorData.getSpeedKmh() ?: return null
        val maxSpeed = rule.parameters.maxSpeed

        val isTriggered = currentSpeed > maxSpeed

        // Calculate confidence based on how much over the limit
        val overspeed = currentSpeed - maxSpeed
        val confidence = when {
            overspeed > 20 -> 0.95f
            overspeed > 10 -> 0.85f
            overspeed > 5 -> 0.75f
            overspeed > 0 -> 0.65f
            else -> 0.0f
        }

        return DetectionResult(
            ruleId = rule.id,
            ruleType = RuleType.SPEED_CONTROL,
            isTriggered = isTriggered,
            confidence = confidence,
            details = "Current: ${String.format("%.1f", currentSpeed)} km/h, Max: $maxSpeed km/h",
            sensorData = sensorData
        )
    }

    /**
     * Prolonged Inactivity Detection Algorithm
     * Detects when user has been inactive for too long
     */
    private fun detectProlongedInactivity(sensorData: SensorData, rule: Rule): DetectionResult {
        val maxInactivityMinutes = rule.parameters.inactivityDuration
        val maxInactivityMs = maxInactivityMinutes * 60 * 1000L

        // Check if current activity is STILL
        val isStill = sensorData.activityType == ActivityType.STILL

        val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTimestamp
        val isTriggered = isStill && timeSinceLastActivity > maxInactivityMs

        val minutesInactive = (timeSinceLastActivity / 60000f)
        val confidence = when {
            minutesInactive > maxInactivityMinutes * 1.5 -> 0.95f
            minutesInactive > maxInactivityMinutes * 1.2 -> 0.85f
            minutesInactive > maxInactivityMinutes -> 0.75f
            else -> 0.0f
        }

        return DetectionResult(
            ruleId = rule.id,
            ruleType = RuleType.PROLONGED_INACTIVITY,
            isTriggered = isTriggered,
            confidence = confidence,
            details = "Inactive for ${String.format("%.1f", minutesInactive)} minutes (max: $maxInactivityMinutes)",
            sensorData = sensorData
        )
    }

    override suspend fun createAlert(
        detectionResult: DetectionResult,
        rule: Rule,
        sensorData: SensorData
    ): Result<AlertEvent> {
        return try {
            val alertData = hashMapOf(
                "type" to detectionResult.ruleType.name,
                "protectedUserId" to rule.protectedId,
                "protectedUserName" to rule.protectedName,
                "monitorIds" to listOf(rule.monitorId),
                "ruleId" to rule.id,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "latitude" to (sensorData.latitude ?: 0.0),
                "longitude" to (sensorData.longitude ?: 0.0),
                "details" to detectionResult.details,
                "confidence" to detectionResult.confidence,
                "cancelled" to false,
                "viewed" to false,
                "createdAt" to Date()
            )

            val docRef = firestore.collection("alerts").add(alertData).await()

            val alertEvent = AlertEvent(
                id = docRef.id,
                type = AlertType.valueOf(detectionResult.ruleType.name),
                protectedUserId = rule.protectedId,
                protectedUserName = rule.protectedName,
                timestamp = java.time.LocalDateTime.now(),
                latitude = sensorData.latitude ?: 0.0,
                longitude = sensorData.longitude ?: 0.0,
                details = detectionResult.details,
                videoUrl = null,
                monitorIds = listOf(rule.monitorId)
            )

            Result.success(alertEvent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastActivityTimestamp(): Long {
        return lastActivityTimestamp
    }

    override suspend fun updateLastActivityTimestamp(timestamp: Long) {
        lastActivityTimestamp = timestamp
    }

    /**
     * Calculate distance between two coordinates (Haversine formula)
     * Returns distance in meters
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Parse rule parameters from Firestore map
     */
    private fun parseRuleParameters(params: Map<String, Any>?): RuleParameters {
        if (params == null) return RuleParameters()

        val geofenceAreas = (params["geofenceAreas"] as? List<Map<String, Any>>)?.mapNotNull { areaMap ->
            try {
                GeofenceArea(
                    id = areaMap["id"] as? String ?: "",
                    name = areaMap["name"] as? String ?: "",
                    latitude = (areaMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (areaMap["longitude"] as? Number)?.toDouble() ?: 0.0,
                    radius = (areaMap["radius"] as? Number)?.toDouble() ?: 100.0
                )
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()

        return RuleParameters(
            geofenceAreas = geofenceAreas,
            maxSpeed = (params["maxSpeed"] as? Number)?.toInt() ?: 120,
            inactivityDuration = (params["inactivityDuration"] as? Number)?.toInt() ?: 30
        )
    }

    /**
     * Cancel an alert within the 10-second cancellation window
     */
    override suspend fun cancelAlert(alertId: String): Result<Boolean> {
        return try {
            val alertRef = firestore.collection("alerts").document(alertId)
            val alertDoc = alertRef.get().await()

            if (!alertDoc.exists()) {
                return Result.failure(Exception("Alert not found"))
            }

            // Check if alert can still be cancelled
            val timestamp = alertDoc.getTimestamp("timestamp")?.toDate()
            val now = Date()
            val timeDiff = now.time - (timestamp?.time ?: 0)

            if (timeDiff > 10000) { // 10 seconds
                return Result.failure(Exception("Cancellation window expired"))
            }

            // Update alert as cancelled
            alertRef.update(
                mapOf(
                    "cancelled" to true,
                    "cancelledAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update alert with video URL after recording and upload
     */
    override suspend fun updateAlertWithVideo(alertId: String, videoUrl: String): Result<Boolean> {
        return try {
            firestore.collection("alerts")
                .document(alertId)
                .update("videoUrl", videoUrl)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add to MonitoringRepositoryImpl.kt

    /**
     * Get alert by ID
     */
    override suspend fun getAlertById(alertId: String): AlertEvent? {
        return try {
            val doc = firestore
                .collection("alerts")
                .document(alertId)
                .get()
                .await()

            if (!doc.exists()) return null

            AlertEvent(
                id = doc.id,
                type = AlertType.valueOf(doc.getString("type") ?: "PANIC_BUTTON"),
                protectedUserId = doc.getString("protectedUserId") ?: return null,
                protectedUserName = doc.getString("protectedUserName") ?: "Unknown User",
                timestamp = doc.getTimestamp("timestamp")?.toLocalDateTime()
                    ?: java.time.LocalDateTime.now(),
                latitude = doc.getDouble("latitude") ?: 0.0,
                longitude = doc.getDouble("longitude") ?: 0.0,
                details = doc.getString("details"),
                videoUrl = doc.getString("videoUrl"),
                monitorIds = doc.get("monitorIds") as? List<String> ?: emptyList(),
                isCancelled = doc.getBoolean("cancelled") ?: false,
                cancelledAt = doc.getTimestamp("cancelledAt")?.toLocalDateTime()
            )
        } catch (e: Exception) {
            null
        }
    }
}

// Extension function to convert Firestore Timestamp to LocalDateTime
@RequiresApi(Build.VERSION_CODES.O)
private fun com.google.firebase.Timestamp.toLocalDateTime(): java.time.LocalDateTime {
    return java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochSecond(seconds, nanoseconds.toLong()),
        java.time.ZoneId.systemDefault()
    )
}

