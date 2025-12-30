package com.example.safetysec.domain.usecase.alert

import com.example.safetysec.domain.repository.MonitoringRepository
import javax.inject.Inject

/**
 * Cancel Alert Use Case
 *
 * Cancels an alert within the 10-second window with PIN verification
 */
class CancelAlertUseCase @Inject constructor(
    private val repository: MonitoringRepository
) {
    suspend operator fun invoke(alertId: String, code: String): Result<Boolean> {
        return repository.cancelAlert(alertId, code)
    }
}