package com.example.safetysec.data.repository

import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import com.example.safetysec.domain.model.ProtectedUserSummary
import com.example.safetysec.domain.repository.MonitorRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import com.google.firebase.firestore.snapshots
import javax.inject.Inject


class MonitorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : MonitorRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    /**
     * Get count of active protected users assigned to current monitor
     */
    override suspend fun getActiveProtectedUsersCount(): Int {
        return try {
            val querySnapshot = firestore
                .collection("associations")
                .whereEqualTo("monitorId", currentUserId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            querySnapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get list of protected users assigned to current monitor
     */
    override suspend fun getProtectedUsers(): List<ProtectedUserSummary> {
        return try {
            val querySnapshot = firestore
                .collection("associations")
                .whereEqualTo("monitorId", currentUserId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    ProtectedUserSummary(
                        id = doc.getString("protectedId") ?: return@mapNotNull null,
                        name = doc.getString("protectedName") ?: "Unknown",
                        email = doc.getString("protectedEmail") ?: "",
                        isActive = doc.getString("status") == "ACTIVE"
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get recent alerts for current monitor
     */
    override suspend fun getRecentAlerts(limit: Int): List<AlertEvent> {
        return try {
            val querySnapshot = firestore
                .collection("alerts")
                .whereArrayContains("monitorIds", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    AlertEvent(
                        id = doc.id,
                        type = AlertType.valueOf(doc.getString("type") ?: "PANIC_BUTTON"),
                        protectedUserId = doc.getString("protectedUserId")
                            ?: return@mapNotNull null,
                        protectedUserName = doc.getString("protectedUserName")
                            ?: "Unknown User",
                        timestamp = doc.getTimestamp("timestamp")?.toLocalDateTime()
                            ?: LocalDateTime.now(),
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        details = doc.getString("details"),
                        videoUrl = doc.getString("videoUrl"),
                        monitorIds = doc.get("monitorIds") as? List<String> ?: emptyList(),
                        isCancelled = doc.getBoolean("cancelled") ?: false,
                        cancelledAt = doc.getTimestamp("cancelledAt")?.toLocalDateTime()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Subscribe to real-time alert updates
     * Emits for all document changes (ADDED, MODIFIED, REMOVED)
     * Deduplication handled in AlertViewModel
     */
    override fun subscribeToAlerts(): Flow<AlertEvent> {
        return firestore
            .collection("alerts")
            .whereArrayContains("monitorIds", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                android.util.Log.d("MonitorRepository", "Snapshot received: ${querySnapshot.documentChanges.size} changes")

                querySnapshot.documentChanges.mapNotNull { change ->
                    val doc = change.document
                    android.util.Log.d("MonitorRepository", "Change type: ${change.type}, Doc ID: ${doc.id}")

                    try {
                        AlertEvent(
                            id = doc.id,
                            type = AlertType.valueOf(doc.getString("type") ?: "PANIC_BUTTON"),
                            protectedUserId = doc.getString("protectedUserId")
                                ?: return@mapNotNull null,
                            protectedUserName = doc.getString("protectedUserName")
                                ?: "Unknown User",
                            timestamp = doc.getTimestamp("timestamp")?.toLocalDateTime()
                                ?: LocalDateTime.now(),
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            details = doc.getString("details"),
                            videoUrl = doc.getString("videoUrl"),
                            monitorIds = doc.get("monitorIds") as? List<String> ?: emptyList(),
                            isCancelled = doc.getBoolean("cancelled") ?: false,
                            cancelledAt = doc.getTimestamp("cancelledAt")?.toLocalDateTime()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("MonitorRepository", "Error parsing alert", e)
                        null
                    }
                }
            }
            .map { alerts ->
                android.util.Log.d("MonitorRepository", "Emitting ${alerts.size} alerts from this snapshot")
                alerts.firstOrNull() ?: throw Exception("No alerts")
            }
    }

    /**
     * Get specific alert by ID
     */
    override suspend fun getAlertById(alertId: String): AlertEvent? {
        return try {
            val doc = firestore
                .collection("alerts")
                .document(alertId)
                .get()
                .await()

            if (!doc.exists()) return null

            AlertEvent(
                id = doc.id,
                type = AlertType.valueOf(doc.getString("type") ?: "PANIC_BUTTON"),
                protectedUserId = doc.getString("protectedUserId") ?: return null,
                protectedUserName = doc.getString("protectedUserName") ?: "Unknown User",
                timestamp = doc.getTimestamp("timestamp")?.toLocalDateTime()
                    ?: LocalDateTime.now(),
                latitude = doc.getDouble("latitude") ?: 0.0,
                longitude = doc.getDouble("longitude") ?: 0.0,
                details = doc.getString("details"),
                videoUrl = doc.getString("videoUrl"),
                monitorIds = doc.get("monitorIds") as? List<String> ?: emptyList(),
                isCancelled = doc.getBoolean("cancelled") ?: false,
                cancelledAt = doc.getTimestamp("cancelledAt")?.toLocalDateTime()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Subscribe to real-time updates for a specific alert by ID
     */
    override fun subscribeToAlertById(alertId: String): Flow<AlertEvent?> {
        return firestore
            .collection("alerts")
            .document(alertId)
            .snapshots()
            .map { doc ->
                if (!doc.exists()) return@map null

                try {
                    AlertEvent(
                        id = doc.id,
                        type = AlertType.valueOf(doc.getString("type") ?: "PANIC_BUTTON"),
                        protectedUserId = doc.getString("protectedUserId") ?: return@map null,
                        protectedUserName = doc.getString("protectedUserName") ?: "Unknown User",
                        timestamp = doc.getTimestamp("timestamp")?.toLocalDateTime()
                            ?: LocalDateTime.now(),
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        details = doc.getString("details"),
                        videoUrl = doc.getString("videoUrl"),
                        monitorIds = doc.get("monitorIds") as? List<String> ?: emptyList(),
                        isCancelled = doc.getBoolean("cancelled") ?: false,
                        cancelledAt = doc.getTimestamp("cancelledAt")?.toLocalDateTime()
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    /**
     * Get alert statistics
     */
    override suspend fun getAlertStatistics(): Map<String, Int> {
        return try {
            val alerts = firestore
                .collection("alerts")
                .whereArrayContains("monitorIds", currentUserId)
                .get()
                .await()

            val stats = mutableMapOf<String, Int>()
            alerts.documents.forEach { doc ->
                val type = doc.getString("type") ?: "UNKNOWN"
                stats[type] = stats.getOrDefault(type, 0) + 1
            }

            stats
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun com.google.firebase.Timestamp.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(this.seconds, this.nanoseconds.toLong()),
            ZoneId.systemDefault()
        )
    }
}