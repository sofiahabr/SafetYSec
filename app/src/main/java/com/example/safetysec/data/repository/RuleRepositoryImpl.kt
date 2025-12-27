package com.example.safetysec.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.safetysec.domain.model.*
import com.example.safetysec.domain.repository.RuleRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RuleRepository {

    companion object {
        private const val COLLECTION_RULES = "rules"
        private const val COLLECTION_TIME_WINDOWS = "timeWindows"
    }

    override suspend fun createRule(rule: Rule): Result<Rule> {
        return try {
            val ruleData = hashMapOf(
                "monitorId" to rule.monitorId,
                "monitorName" to rule.monitorName,
                "protectedId" to rule.protectedId,
                "protectedName" to rule.protectedName,
                "type" to rule.type.name,
                "parameters" to mapParameters(rule.parameters),
                "status" to RuleStatus.PENDING.name,
                "createdAt" to Date(),
                "updatedAt" to Date()
            )

            val docRef = firestore.collection(COLLECTION_RULES)
                .add(ruleData)
                .await()

            val createdRule = rule.copy(
                id = docRef.id,
                status = RuleStatus.PENDING,
                createdAt = Date(),
                updatedAt = Date()
            )

            Result.success(createdRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRule(rule: Rule): Result<Rule> {
        return try {
            val updates = hashMapOf<String, Any>(
                "type" to rule.type.name,
                "parameters" to mapParameters(rule.parameters),
                "updatedAt" to Date()
            )

            firestore.collection(COLLECTION_RULES)
                .document(rule.id)
                .update(updates)
                .await()

            val updatedRule = rule.copy(updatedAt = Date())
            Result.success(updatedRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRule(ruleId: String, userId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .get()
                .await()

            val rule = doc.toObject(Rule::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Rule not found"))

            // Verify user is either monitor or protected
            if (rule.monitorId != userId && rule.protectedId != userId) {
                return Result.failure(Exception("Unauthorized to delete this rule"))
            }

            // Update status to CANCELLED instead of deleting
            firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .update(
                    mapOf(
                        "status" to RuleStatus.CANCELLED.name,
                        "updatedAt" to Date()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun authorizeRule(ruleId: String, protectedId: String): Result<Rule> {
        return try {
            val doc = firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .get()
                .await()

            val rule = doc.toObject(Rule::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Rule not found"))

            if (rule.protectedId != protectedId) {
                return Result.failure(Exception("Unauthorized to authorize this rule"))
            }

            val updates = mapOf(
                "status" to RuleStatus.AUTHORIZED.name,
                "authorizedAt" to Date(),
                "updatedAt" to Date()
            )

            firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .update(updates)
                .await()

            val authorizedRule = rule.copy(
                status = RuleStatus.AUTHORIZED,
                authorizedAt = Date(),
                updatedAt = Date()
            )

            Result.success(authorizedRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectRule(ruleId: String, protectedId: String): Result<Rule> {
        return try {
            val doc = firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .get()
                .await()

            val rule = doc.toObject(Rule::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Rule not found"))

            if (rule.protectedId != protectedId) {
                return Result.failure(Exception("Unauthorized to reject this rule"))
            }

            val updates = mapOf(
                "status" to RuleStatus.REJECTED.name,
                "updatedAt" to Date()
            )

            firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .update(updates)
                .await()

            val rejectedRule = rule.copy(
                status = RuleStatus.REJECTED,
                updatedAt = Date()
            )

            Result.success(rejectedRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeRule(ruleId: String, protectedId: String): Result<Rule> {
        return try {
            val doc = firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .get()
                .await()

            val rule = doc.toObject(Rule::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Rule not found"))

            if (rule.protectedId != protectedId) {
                return Result.failure(Exception("Unauthorized to revoke this rule"))
            }

            val updates = mapOf(
                "status" to RuleStatus.REVOKED.name,
                "updatedAt" to Date()
            )

            firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .update(updates)
                .await()

            val revokedRule = rule.copy(
                status = RuleStatus.REVOKED,
                updatedAt = Date()
            )

            Result.success(revokedRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRulesAsMonitor(monitorId: String): Flow<List<Rule>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_RULES)
            .whereEqualTo("monitorId", monitorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parseRule(doc.data ?: return@mapNotNull null, doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(rules)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getRulesAsProtected(protectedId: String): Flow<List<Rule>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_RULES)
            .whereEqualTo("protectedId", protectedId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parseRule(doc.data ?: return@mapNotNull null, doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(rules)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getRulesByStatus(
        userId: String,
        status: RuleStatus,
        asMonitor: Boolean
    ): Flow<List<Rule>> = callbackFlow {
        val field = if (asMonitor) "monitorId" else "protectedId"

        val listenerRegistration = firestore.collection(COLLECTION_RULES)
            .whereEqualTo(field, userId)
            .whereEqualTo("status", status.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parseRule(doc.data ?: return@mapNotNull null, doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(rules)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getRuleById(ruleId: String): Result<Rule> {
        return try {
            val doc = firestore.collection(COLLECTION_RULES)
                .document(ruleId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("Rule not found"))
            }

            val rule = parseRule(doc.data ?: return Result.failure(Exception("Invalid rule data")), doc.id)
            Result.success(rule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAuthorizedRules(
        monitorId: String,
        protectedId: String
    ): Flow<List<Rule>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_RULES)
            .whereEqualTo("monitorId", monitorId)
            .whereEqualTo("protectedId", protectedId)
            .whereEqualTo("status", RuleStatus.AUTHORIZED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parseRule(doc.data ?: return@mapNotNull null, doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(rules)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Time Window methods

    override suspend fun createTimeWindow(timeWindow: TimeWindow): Result<TimeWindow> {
        return try {
            val timeWindowData = hashMapOf(
                "protectedId" to timeWindow.protectedId,
                "monitorId" to timeWindow.monitorId,
                "monitorName" to timeWindow.monitorName,
                "daysOfWeek" to timeWindow.daysOfWeek.map { it.name },
                "startTime" to timeWindow.startTime,
                "endTime" to timeWindow.endTime,
                "isActive" to timeWindow.isActive,
                "createdAt" to Date(),
                "updatedAt" to Date()
            )

            val docRef = firestore.collection(COLLECTION_TIME_WINDOWS)
                .add(timeWindowData)
                .await()

            val createdWindow = timeWindow.copy(
                id = docRef.id,
                createdAt = Date(),
                updatedAt = Date()
            )

            Result.success(createdWindow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTimeWindow(timeWindow: TimeWindow): Result<TimeWindow> {
        return try {
            val updates = hashMapOf<String, Any>(
                "daysOfWeek" to timeWindow.daysOfWeek.map { it.name },
                "startTime" to timeWindow.startTime,
                "endTime" to timeWindow.endTime,
                "isActive" to timeWindow.isActive,
                "updatedAt" to Date()
            )

            firestore.collection(COLLECTION_TIME_WINDOWS)
                .document(timeWindow.id)
                .update(updates)
                .await()

            val updatedWindow = timeWindow.copy(updatedAt = Date())
            Result.success(updatedWindow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTimeWindow(timeWindowId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_TIME_WINDOWS)
                .document(timeWindowId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTimeWindows(protectedId: String): Flow<List<TimeWindow>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_TIME_WINDOWS)
            .whereEqualTo("protectedId", protectedId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val timeWindows = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parseTimeWindow(doc.data ?: return@mapNotNull null, doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(timeWindows)
            }

        awaitClose { listenerRegistration.remove() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getActiveTimeWindows(protectedId: String): Result<List<TimeWindow>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_TIME_WINDOWS)
                .whereEqualTo("protectedId", protectedId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val timeWindows = snapshot.documents.mapNotNull { doc ->
                try {
                    parseTimeWindow(doc.data ?: return@mapNotNull null, doc.id)
                } catch (e: Exception) {
                    null
                }
            }.filter { it.isCurrentlyActive() }

            Result.success(timeWindows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isMonitoringActive(protectedId: String): Result<Boolean> {
        return try {
            val activeWindows = getActiveTimeWindows(protectedId).getOrNull() ?: emptyList()
            Result.success(activeWindows.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper methods

    private fun mapParameters(parameters: RuleParameters): Map<String, Any> {
        return mapOf(
            "geofenceAreas" to parameters.geofenceAreas.map { area ->
                mapOf(
                    "id" to area.id,
                    "name" to area.name,
                    "latitude" to area.latitude,
                    "longitude" to area.longitude,
                    "radius" to area.radius
                )
            },
            "maxSpeed" to parameters.maxSpeed,
            "inactivityDuration" to parameters.inactivityDuration
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRule(data: Map<String, Any>, id: String): Rule {
        val parametersMap = data["parameters"] as? Map<String, Any> ?: emptyMap()

        val geofenceAreasList = parametersMap["geofenceAreas"] as? List<Map<String, Any>> ?: emptyList()
        val geofenceAreas = geofenceAreasList.map { areaMap ->
            GeofenceArea(
                id = areaMap["id"] as? String ?: "",
                name = areaMap["name"] as? String ?: "",
                latitude = (areaMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (areaMap["longitude"] as? Number)?.toDouble() ?: 0.0,
                radius = (areaMap["radius"] as? Number)?.toDouble() ?: 100.0
            )
        }

        val parameters = RuleParameters(
            geofenceAreas = geofenceAreas,
            maxSpeed = (parametersMap["maxSpeed"] as? Number)?.toInt() ?: 120,
            inactivityDuration = (parametersMap["inactivityDuration"] as? Number)?.toInt() ?: 30
        )

        return Rule(
            id = id,
            monitorId = data["monitorId"] as? String ?: "",
            monitorName = data["monitorName"] as? String ?: "",
            protectedId = data["protectedId"] as? String ?: "",
            protectedName = data["protectedName"] as? String ?: "",
            type = RuleType.valueOf(data["type"] as? String ?: "FALL_DETECTION"),
            parameters = parameters,
            status = RuleStatus.valueOf(data["status"] as? String ?: "PENDING"),
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            authorizedAt = (data["authorizedAt"] as? com.google.firebase.Timestamp)?.toDate()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTimeWindow(data: Map<String, Any>, id: String): TimeWindow {
        val daysOfWeekList = data["daysOfWeek"] as? List<String> ?: emptyList()
        val daysOfWeek = daysOfWeekList.mapNotNull { dayStr ->
            try {
                DayOfWeek.valueOf(dayStr)
            } catch (e: Exception) {
                null
            }
        }

        return TimeWindow(
            id = id,
            protectedId = data["protectedId"] as? String ?: "",
            monitorId = data["monitorId"] as? String ?: "",
            monitorName = data["monitorName"] as? String ?: "",
            daysOfWeek = daysOfWeek,
            startTime = data["startTime"] as? String ?: "00:00",
            endTime = data["endTime"] as? String ?: "23:59",
            isActive = data["isActive"] as? Boolean ?: true,
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
        )
    }
}