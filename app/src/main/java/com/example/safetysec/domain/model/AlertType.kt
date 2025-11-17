package com.example.safetysec.domain.model

enum class AlertType {
    FALL_DETECTED,
    SPEED_ALERT,
    GEOFENCE_BREACH,
    ACCIDENT_DETECTED,
    PROLONGED_INACTIVITY,
    PANIC_BUTTON;

    fun toDisplayString(): String = when (this) {
        FALL_DETECTED -> "Fall Detected"
        SPEED_ALERT -> "Speed Alert"
        GEOFENCE_BREACH -> "Geofence Breach"
        ACCIDENT_DETECTED -> "Accident Detected"
        PROLONGED_INACTIVITY -> "Prolonged Inactivity"
        PANIC_BUTTON -> "Panic Button"
    }

    fun toFirebaseString(): String = this.name
}
