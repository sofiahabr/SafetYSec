package com.example.safetysec.domain.usecase.monitoring

import com.example.safetysec.domain.repository.MonitorRepository
import com.example.safetysec.domain.model.ProtectedUserSummary
import javax.inject.Inject

class GetProtectedUsersUseCase @Inject constructor(
    private val monitorRepository: MonitorRepository
) {
    suspend operator fun invoke(): List<ProtectedUserSummary> {
        return monitorRepository.getProtectedUsers()
    }
}