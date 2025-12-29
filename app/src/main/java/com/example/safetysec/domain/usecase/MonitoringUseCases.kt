package com.example.safetysec.domain.usecase

import android.content.Context
import com.example.safetysec.domain.model.MonitoringState
import com.example.safetysec.domain.repository.MonitoringRepository
import com.example.safetysec.service.MonitoringService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Start Monitoring Use Case
 *
 * Starts the monitoring foreground service and initializes monitoring
 */
class StartMonitoringUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            // Start monitoring in repository
            val result = monitoringRepository.startMonitoring()

            // Start foreground service
            if (result.isSuccess) {
                MonitoringService.startMonitoring(context)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Stop Monitoring Use Case
 *
 * Stops the monitoring foreground service
 */
class StopMonitoringUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            // Stop monitoring in repository
            val result = monitoringRepository.stopMonitoring()

            // Stop foreground service
            MonitoringService.stopMonitoring(context)

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Get Monitoring State Use Case
 *
 * Gets the current monitoring state as a flow
 */
class GetMonitoringStateUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    operator fun invoke(): Flow<MonitoringState> {
        return monitoringRepository.getMonitoringStateFlow()
    }
}

/**
 * Check Active Time Window Use Case
 *
 * Checks if current time is within an active time window
 */
class CheckActiveTimeWindowUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(protectedUserId: String): Boolean {
        val timeWindows = monitoringRepository.getActiveTimeWindows(protectedUserId)
        return monitoringRepository.isInActiveTimeWindow(timeWindows)
    }
}

/**
 * Trigger Panic Button Use Case
 *
 * Manually triggers a panic button alert
 */
class TriggerPanicButtonUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            // Get current state
            val state = monitoringRepository.getMonitoringStateFlow()

            // TODO: Create panic button alert
            // This will be implemented in Phase 6 with the alert system

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}