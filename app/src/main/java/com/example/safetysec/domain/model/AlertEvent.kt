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
    val videoRecordingPath: String? = null,

    val monitorIds: List<String> = emptyList(),

    // Cancellation fields
    val isCancelled: Boolean = false,
    val cancelledAt: LocalDateTime? = null,
    val cancellationDeadline: LocalDateTime? = null,

    // Status fields
    val isViewed: Boolean = false,
    val viewedAt: LocalDateTime? = null
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

    /**
     * Check if alert can still be cancelled
     */
    fun canBeCancelled(): Boolean {
        if (isCancelled) return false
        val deadline = cancellationDeadline ?: return false
        return LocalDateTime.now().isBefore(deadline)
    }

    /**
     * Get remaining cancellation time in seconds
     */
    fun getRemainingCancellationSeconds(): Int {
        if (!canBeCancelled()) return 0
        val deadline = cancellationDeadline ?: return 0
        val now = LocalDateTime.now()
        return if (now.isBefore(deadline)) {
            java.time.Duration.between(now, deadline).seconds.toInt()
        } else {
            0
        }
    }
}