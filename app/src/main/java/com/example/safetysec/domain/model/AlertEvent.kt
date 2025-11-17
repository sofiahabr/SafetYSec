package com.example.safetysec.domain.model

import java.time.LocalDateTime


data class AlertEvent(
    val id: String,
    val type: AlertType,

    val protectedUserId: String,
    val protectedUserName: String,

    val timestamp: LocalDateTime,
    val latitude: Double,
    val longitude: Double,
    val details: String? = null,
    val videoUrl: String? = null,

    val monitorIds: List<String> = emptyList()
) {
    /**
     * Format coordinates for display
     */
    fun getFormattedLocation(): String {
        return if (latitude != 0.0 && longitude != 0.0) {
            String.format("%.4f°N, %.5f°W", latitude, longitude)
        } else {
            "Location unavailable"
        }
    }

    /**
     * Get human-readable alert type
     */
    fun getAlertTypeDisplay(): String = type.toDisplayString()
}