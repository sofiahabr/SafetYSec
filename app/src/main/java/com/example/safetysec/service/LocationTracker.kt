package com.example.safetysec.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location Tracker
 *
 * Manages location updates for monitoring
 */
@Singleton
class LocationTracker @Inject constructor() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private var currentLocationData = LocationData()

    private var isTracking = false

    /**
     * Initialize location client
     */
    fun initialize(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Start tracking location
     */
    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL_MS)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationData(location)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        isTracking = true
    }

    /**
     * Stop tracking location
     */
    fun stopTracking() {
        if (!isTracking) return

        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }

        isTracking = false
    }

    /**
     * Release resources
     */
    fun release() {
        stopTracking()
        fusedLocationClient = null
        locationCallback = null
    }

    /**
     * Update location data from Location object
     */
    private fun updateLocationData(location: Location) {
        currentLocationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            speed = if (location.hasSpeed()) location.speed else null,
            accuracy = if (location.hasAccuracy()) location.accuracy else null,
            timestamp = location.time
        )
    }

    /**
     * Get current location data
     */
    fun getCurrentLocation(): LocationData {
        return currentLocationData
    }

    /**
     * Get last known location (one-time)
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationData? {
        return try {
            val location = fusedLocationClient?.lastLocation?.result
            location?.let {
                LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    speed = if (it.hasSpeed()) it.speed else null,
                    accuracy = if (it.hasAccuracy()) it.accuracy else null,
                    timestamp = it.time
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val LOCATION_UPDATE_INTERVAL_MS = 5000L // 5 seconds
        private const val LOCATION_FASTEST_INTERVAL_MS = 2000L // 2 seconds
    }
}

/**
 * Location data class
 */
data class LocationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speed: Float? = null, // m/s
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }
}