package com.example.safetysec.domain.usecase.monitoring

import com.example.safetysec.domain.repository.MonitorRepository
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import com.example.safetysec.domain.model.ProtectedUserSummary
import javax.inject.Inject

class GetRecentAlertsUseCase @Inject constructor(
    private val repository: MonitorRepository
) {
    suspend operator fun invoke(limit: Int = 10): List<AlertEvent> {
        return repository.getRecentAlerts(limit)
    }
}