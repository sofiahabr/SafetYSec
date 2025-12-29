package com.example.safetysec.domain.model

/**
 * Sensor Data Domain Model
 *
 * Represents sensor readings collected during monitoring
 */
data class SensorData(
    val timestamp: Long = System.currentTimeMillis(),

    // Accelerometer data (m/s²)
    val accelerometerX: Float = 0f,
    val accelerometerY: Float = 0f,
    val accelerometerZ: Float = 0f,

    // Gyroscope data (rad/s)
    val gyroscopeX: Float = 0f,
    val gyroscopeY: Float = 0f,
    val gyroscopeZ: Float = 0f,

    // Location data
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speed: Float? = null, // m/s
    val accuracy: Float? = null,

    // Activity recognition
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val activityConfidence: Int = 0
) {
    /**
     * Calculate total acceleration magnitude
     */
    fun getTotalAcceleration(): Float {
        return kotlin.math.sqrt(
            accelerometerX * accelerometerX +
                    accelerometerY * accelerometerY +
                    accelerometerZ * accelerometerZ
        )
    }

    /**
     * Calculate total rotation magnitude
     */
    fun getTotalRotation(): Float {
        return kotlin.math.sqrt(
            gyroscopeX * gyroscopeX +
                    gyroscopeY * gyroscopeY +
                    gyroscopeZ * gyroscopeZ
        )
    }

    /**
     * Get speed in km/h
     */
    fun getSpeedKmh(): Float? {
        return speed?.let { it * 3.6f } // Convert m/s to km/h
    }

    /**
     * Check if location data is available
     */
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }
}

/**
 * Activity Type Enumeration
 */
enum class ActivityType {
    STILL,
    ON_FOOT,
    WALKING,
    RUNNING,
    ON_BICYCLE,
    IN_VEHICLE,
    TILTING,
    UNKNOWN;

    fun toDisplayString(): String = when (this) {
        STILL -> "Still"
        ON_FOOT -> "On Foot"
        WALKING -> "Walking"
        RUNNING -> "Running"
        ON_BICYCLE -> "On Bicycle"
        IN_VEHICLE -> "In Vehicle"
        TILTING -> "Tilting"
        UNKNOWN -> "Unknown"
    }
}