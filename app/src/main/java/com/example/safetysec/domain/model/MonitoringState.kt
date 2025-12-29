package com.example.safetysec.domain.model

import java.util.Date

/**
 * Monitoring State Domain Model
 *
 * Represents the current state of the monitoring service
 */
data class MonitoringState(
    val isRunning: Boolean = false,
    val protectedUserId: String = "",
    val protectedUserName: String = "",
    val activeRules: List<Rule> = emptyList(),
    val activeTimeWindows: List<TimeWindow> = emptyList(),
    val currentSensorData: SensorData? = null,
    val lastUpdateTime: Date = Date(),
    val detectionResults: List<DetectionResult> = emptyList(),
    val isInActiveTimeWindow: Boolean = false,
    val lastLocationUpdate: Date? = null,
    val lastActivityUpdate: Date? = null
) {
    /**
     * Check if monitoring is active and rules can be evaluated
     */
    fun canEvaluateRules(): Boolean {
        return isRunning && activeRules.isNotEmpty() && isInActiveTimeWindow
    }

    /**
     * Get count of active authorized rules
     */
    fun getActiveRuleCount(): Int {
        return activeRules.count { it.isActive() }
    }

    /**
     * Get formatted status message
     */
    fun getStatusMessage(): String {
        return when {
            !isRunning -> "Monitoring is stopped"
            !isInActiveTimeWindow -> "Outside active time window"
            activeRules.isEmpty() -> "No active rules"
            else -> "Monitoring ${getActiveRuleCount()} rule(s)"
        }
    }
}

/**
 * Detection Result
 *
 * Represents the result of a rule detection
 */
data class DetectionResult(
    val ruleId: String,
    val ruleType: RuleType,
    val isTriggered: Boolean,
    val confidence: Float = 0f, // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = "",
    val sensorData: SensorData? = null
) {
    /**
     * Check if this detection should trigger an alert
     */
    fun shouldTriggerAlert(): Boolean {
        return isTriggered && confidence >= 0.7f
    }
}