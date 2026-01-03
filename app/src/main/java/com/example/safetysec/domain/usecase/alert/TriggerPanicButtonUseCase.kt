package com.example.safetysec.domain.usecase.alert

import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import com.example.safetysec.domain.model.DetectionResult
import com.example.safetysec.domain.model.RuleType
import com.example.safetysec.domain.model.SensorData
import com.example.safetysec.domain.repository.MonitoringRepository
import com.example.safetysec.domain.repository.RuleRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

/**
 * Trigger Panic Button Use Case
 *
 * Creates an immediate alert when the protected user presses the panic button
 */
class TriggerPanicButtonUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository,
    private val ruleRepository: RuleRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(sensorData: SensorData): Result<AlertEvent> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Get all panic button rules for this user
            val rules = monitoringRepository.getActiveRules(currentUserId)
            val panicButtonRule = rules.find { it.type == RuleType.PANIC_BUTTON }
                ?: return Result.failure(Exception("No panic button rule found"))

            // Create detection result for panic button
            val detectionResult = DetectionResult(
                ruleId = panicButtonRule.id,
                ruleType = RuleType.PANIC_BUTTON,
                isTriggered = true,
                confidence = 1.0f,
                details = "Panic button pressed by user",
                sensorData = sensorData
            )

            // Create alert immediately
            monitoringRepository.createAlert(
                detectionResult,
                panicButtonRule,
                sensorData
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}