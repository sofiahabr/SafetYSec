package com.example.safetysec.domain.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Date

/**
 * Time Window Domain Model
 *
 * Represents a temporal monitoring window where rules are active
 */
data class TimeWindow(
    val id: String = "",
    val protectedId: String = "",
    val monitorId: String = "",
    val monitorName: String = "",
    val daysOfWeek: List<DayOfWeek> = emptyList(),
    val startTime: String = "00:00", // Format: HH:mm
    val endTime: String = "23:59",   // Format: HH:mm
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    /**
     * Check if current time falls within this window
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isCurrentlyActive(): Boolean {
        if (!isActive) return false

        val now = java.time.LocalDateTime.now()
        val currentDay = DayOfWeek.from(now.dayOfWeek)

        if (!daysOfWeek.contains(currentDay)) return false

        val currentTime = String.format("%02d:%02d", now.hour, now.minute)

        return currentTime >= startTime && currentTime <= endTime
    }

    /**
     * Get human-readable days string
     */
    fun getDaysString(): String {
        if (daysOfWeek.isEmpty()) return "No days selected"
        if (daysOfWeek.size == 7) return "Every day"

        return daysOfWeek.sortedBy { it.ordinal }
            .joinToString(", ") { it.toShortString() }
    }

    /**
     * Get formatted time range
     */
    fun getTimeRangeString(): String {
        return "$startTime - $endTime"
    }
}

/**
 * Day of Week Enumeration
 */
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    fun toDisplayString(): String = when (this) {
        MONDAY -> "Monday"
        TUESDAY -> "Tuesday"
        WEDNESDAY -> "Wednesday"
        THURSDAY -> "Thursday"
        FRIDAY -> "Friday"
        SATURDAY -> "Saturday"
        SUNDAY -> "Sunday"
    }

    fun toShortString(): String = when (this) {
        MONDAY -> "Mon"
        TUESDAY -> "Tue"
        WEDNESDAY -> "Wed"
        THURSDAY -> "Thu"
        FRIDAY -> "Fri"
        SATURDAY -> "Sat"
        SUNDAY -> "Sun"
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun from(dayOfWeek: java.time.DayOfWeek): DayOfWeek {
            return when (dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> MONDAY
                java.time.DayOfWeek.TUESDAY -> TUESDAY
                java.time.DayOfWeek.WEDNESDAY -> WEDNESDAY
                java.time.DayOfWeek.THURSDAY -> THURSDAY
                java.time.DayOfWeek.FRIDAY -> FRIDAY
                java.time.DayOfWeek.SATURDAY -> SATURDAY
                java.time.DayOfWeek.SUNDAY -> SUNDAY
            }
        }
    }
}