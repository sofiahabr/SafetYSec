package com.example.safetysec.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.safetysec.domain.model.ActivityType
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sensor Data Collector
 *
 * Manages sensor listeners and collects sensor data
 */
@Singleton
class SensorDataCollector @Inject constructor() : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var activityRecognitionClient: ActivityRecognitionClient? = null

    // Current sensor values
    private var accelerometerData = Vector3(0f, 0f, 0f)
    private var gyroscopeData = Vector3(0f, 0f, 0f)
    private var activityData = ActivityData(ActivityType.UNKNOWN, 0)

    private var isCollecting = false

    /**
     * Initialize sensor manager
     */
    fun initialize(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        activityRecognitionClient = ActivityRecognition.getClient(context)
    }

    /**
     * Start collecting sensor data
     */
    fun startCollecting() {
        if (isCollecting) return

        accelerometer?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        gyroscope?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        isCollecting = true
    }

    /**
     * Stop collecting sensor data
     */
    fun stopCollecting() {
        if (!isCollecting) return

        sensorManager?.unregisterListener(this)

        isCollecting = false
    }

    /**
     * Release resources
     */
    fun release() {
        stopCollecting()
        sensorManager = null
        activityRecognitionClient = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerData = Vector3(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroscopeData = Vector3(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    /**
     * Get current accelerometer data
     */
    fun getAccelerometerData(): Vector3 {
        return accelerometerData
    }

    /**
     * Get current gyroscope data
     */
    fun getGyroscopeData(): Vector3 {
        return gyroscopeData
    }

    /**
     * Get current activity data
     */
    fun getActivityData(): ActivityData {
        return activityData
    }

    /**
     * Update activity data (called from activity recognition)
     */
    fun updateActivityData(detectedActivity: DetectedActivity) {
        val activityType = when (detectedActivity.type) {
            DetectedActivity.STILL -> ActivityType.STILL
            DetectedActivity.ON_FOOT -> ActivityType.ON_FOOT
            DetectedActivity.WALKING -> ActivityType.WALKING
            DetectedActivity.RUNNING -> ActivityType.RUNNING
            DetectedActivity.ON_BICYCLE -> ActivityType.ON_BICYCLE
            DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
            DetectedActivity.TILTING -> ActivityType.TILTING
            else -> ActivityType.UNKNOWN
        }

        activityData = ActivityData(activityType, detectedActivity.confidence)
    }
}

/**
 * Vector3 data class for 3D sensor values
 */
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * Activity data class
 */
data class ActivityData(
    val type: ActivityType,
    val confidence: Int
)