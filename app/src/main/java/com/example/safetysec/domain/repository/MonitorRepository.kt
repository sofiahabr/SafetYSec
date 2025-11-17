package com.example.safetysec.domain.repository

import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.ProtectedUserSummary
import kotlinx.coroutines.flow.Flow

interface MonitorRepository {

    /**
     * Get count of active protected users for the current monitor
     */
    suspend fun getActiveProtectedUsersCount(): Int

    /**
     * Get list of protected users assigned to the current monitor
     */
    suspend fun getProtectedUsers(): List<ProtectedUserSummary>

    /**
     * Get recent alerts for the current monitor
     * @param limit Maximum number of alerts to retrieve
     */
    suspend fun getRecentAlerts(limit: Int = 10): List<AlertEvent>

    /**
     * Subscribe to real-time alert updates (returns a Flow for live updates)
     */
    fun subscribeToAlerts(): Flow<AlertEvent>

    /**
     * Get alert by ID (for detailed view)
     */
    suspend fun getAlertById(alertId: String): AlertEvent?

    /**
     * Get alert statistics (counts by type, etc.)
     */
    suspend fun getAlertStatistics(): Map<String, Int>
}