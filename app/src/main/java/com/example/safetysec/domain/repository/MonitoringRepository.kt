package com.example.safetysec.domain.repository

import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.DetectionResult
import com.example.safetysec.domain.model.MonitoringState
import com.example.safetysec.domain.model.Rule
import com.example.safetysec.domain.model.SensorData
import com.example.safetysec.domain.model.TimeWindow
import kotlinx.coroutines.flow.Flow

/**
 * Monitoring Repository Interface
 *
 * Handles monitoring operations, sensor data, and alert creation
 */
interface MonitoringRepository {

    /**
     * Get current monitoring state as a flow
     */
    fun getMonitoringStateFlow(): Flow<MonitoringState>

    /**
     * Start monitoring service
     */
    suspend fun startMonitoring(): Result<Boolean>

    /**
     * Stop monitoring service
     */
    suspend fun stopMonitoring(): Result<Boolean>

    /**
     * Update current sensor data
     */
    suspend fun updateSensorData(sensorData: SensorData)

    /**
     * Get active rules for current protected user
     */
    suspend fun getActiveRules(protectedUserId: String): List<Rule>

    /**
     * Get active time windows for current protected user
     */
    suspend fun getActiveTimeWindows(protectedUserId: String): List<TimeWindow>

    /**
     * Check if current time is within active time windows
     */
    suspend fun isInActiveTimeWindow(timeWindows: List<TimeWindow>): Boolean

    /**
     * Evaluate detection algorithms against current sensor data
     */
    suspend fun evaluateDetections(
        sensorData: SensorData,
        rules: List<Rule>
    ): List<DetectionResult>

    /**
     * Create alert from detection result
     */
    suspend fun createAlert(
        detectionResult: DetectionResult,
        rule: Rule,
        sensorData: SensorData
    ): Result<AlertEvent>

    /**
     * Cancel an alert within the cancellation window
     */
    suspend fun cancelAlert(alertId: String, code: String): Result<Boolean>

    /**
     * Update alert with video URL after recording
     */
    suspend fun updateAlertWithVideo(alertId: String, videoUrl: String): Result<Boolean>

    /**
     * Get last inactivity time (for prolonged inactivity detection)
     */
    suspend fun getLastActivityTimestamp(): Long

    /**
     * Update last activity timestamp
     */
    suspend fun updateLastActivityTimestamp(timestamp: Long)

    /**
     * Refresh statistics (rules and time windows) for current user
     */
    suspend fun refreshStatistics()

    // Add to MonitoringRepository.kt interface
    suspend fun getAlertById(alertId: String): AlertEvent?
}