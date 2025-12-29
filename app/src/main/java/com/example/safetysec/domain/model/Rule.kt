package com.example.safetysec.domain.model

import java.util.Date

/**
 * Rule Domain Model
 *
 * Represents a safety monitoring rule in the SafetYSec system
 */
data class Rule(
    val id: String = "",
    val monitorId: String = "",
    val monitorName: String = "",
    val protectedId: String = "",
    val protectedName: String = "",
    val type: RuleType = RuleType.FALL_DETECTION,
    val parameters: RuleParameters = RuleParameters(),
    val status: RuleStatus = RuleStatus.PENDING,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val authorizedAt: Date? = null
) {
    /**
     * Check if rule is active and authorized
     */
    fun isActive(): Boolean = status == RuleStatus.AUTHORIZED

    /**
     * Get user-friendly description of the rule
     */
    fun getDescription(): String {
        return when (type) {
            RuleType.FALL_DETECTION -> "Detects when user falls to the ground"
            RuleType.ACCIDENT_DETECTION -> "Detects sudden deceleration (accidents)"
            RuleType.GEOFENCING -> {
                val areas = parameters.geofenceAreas.size
                "Monitors $areas defined area${if (areas != 1) "s" else ""}"
            }
            RuleType.SPEED_CONTROL -> "Alerts when speed exceeds ${parameters.maxSpeed} km/h"
            RuleType.PROLONGED_INACTIVITY -> "Alerts after ${parameters.inactivityDuration} minutes of inactivity"
            RuleType.PANIC_BUTTON -> "Allows manual emergency alert activation"
        }
    }
}

/**
 * Rule Type Enumeration
 */
enum class RuleType {
    FALL_DETECTION,
    ACCIDENT_DETECTION,
    GEOFENCING,
    SPEED_CONTROL,
    PROLONGED_INACTIVITY,
    PANIC_BUTTON;

    fun toDisplayString(): String = when (this) {
        FALL_DETECTION -> "Fall Detection"
        ACCIDENT_DETECTION -> "Accident Detection"
        GEOFENCING -> "Geofencing"
        SPEED_CONTROL -> "Speed Control"
        PROLONGED_INACTIVITY -> "Prolonged Inactivity"
        PANIC_BUTTON -> "Panic Button"
    }

    fun requiresParameters(): Boolean = when (this) {
        GEOFENCING, SPEED_CONTROL, PROLONGED_INACTIVITY -> true
        else -> false
    }
}

/**
 * Rule Status
 */
enum class RuleStatus {
    PENDING,     // Waiting for Protected user authorization
    AUTHORIZED,  // Authorized and active
    REJECTED,    // Rejected by Protected user
    REVOKED,     // Authorization revoked by Protected user
    CANCELLED;   // Cancelled by Monitor

    fun toDisplayString(): String = when (this) {
        PENDING -> "Pending Authorization"
        AUTHORIZED -> "Authorized"
        REJECTED -> "Rejected"
        REVOKED -> "Revoked"
        CANCELLED -> "Cancelled"
    }
}

/**
 * Rule Parameters
 *
 * Contains specific parameters for each rule type
 */
data class RuleParameters(
    // Geofencing parameters
    val geofenceAreas: List<GeofenceArea> = emptyList(),

    // Speed control parameter
    val maxSpeed: Int = 120, // km/h

    // Prolonged inactivity parameter
    val inactivityDuration: Int = 30 // minutes
)

/**
 * Geofence Area
 */
data class GeofenceArea(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 100.0 // meters
) {
    fun getFormattedLocation(): String {
        return String.format("%.4f°, %.4f°", latitude, longitude)
    }
}