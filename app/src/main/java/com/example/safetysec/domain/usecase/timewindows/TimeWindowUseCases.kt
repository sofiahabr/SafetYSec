package com.example.safetysec.domain.usecase.timewindows

import com.example.safetysec.domain.model.TimeWindow
import com.example.safetysec.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Create Time Window Use Case (Protected)
 */
class CreateTimeWindowUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(timeWindow: TimeWindow): Result<TimeWindow> {
        // Validate time window
        if (timeWindow.daysOfWeek.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("At least one day must be selected")
            )
        }

        if (timeWindow.startTime >= timeWindow.endTime) {
            return Result.failure(
                IllegalArgumentException("Start time must be before end time")
            )
        }

        return ruleRepository.createTimeWindow(timeWindow)
    }
}

/**
 * Update Time Window Use Case (Protected)
 */
class UpdateTimeWindowUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(timeWindow: TimeWindow): Result<TimeWindow> {
        // Validate time window
        if (timeWindow.daysOfWeek.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("At least one day must be selected")
            )
        }

        if (timeWindow.startTime >= timeWindow.endTime) {
            return Result.failure(
                IllegalArgumentException("Start time must be before end time")
            )
        }

        return ruleRepository.updateTimeWindow(timeWindow)
    }
}

/**
 * Delete Time Window Use Case (Protected)
 */
class DeleteTimeWindowUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(timeWindowId: String): Result<Unit> {
        return ruleRepository.deleteTimeWindow(timeWindowId)
    }
}

/**
 * Get Time Windows Use Case (Protected)
 */
class GetTimeWindowsUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    operator fun invoke(protectedId: String): Flow<List<TimeWindow>> {
        return ruleRepository.getTimeWindows(protectedId)
    }
}

/**
 * Get Active Time Windows Use Case
 */
class GetActiveTimeWindowsUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(protectedId: String): Result<List<TimeWindow>> {
        return ruleRepository.getActiveTimeWindows(protectedId)
    }
}

/**
 * Check Monitoring Active Use Case
 */
class IsMonitoringActiveUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(protectedId: String): Result<Boolean> {
        return ruleRepository.isMonitoringActive(protectedId)
    }
}