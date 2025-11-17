package com.example.safetysec.domain.usecase.monitoring

import com.example.safetysec.domain.repository.MonitorRepository
import javax.inject.Inject

class GetActiveProtectedUseCase @Inject constructor(
    private val monitorRepository: MonitorRepository
) {
    suspend operator fun invoke(): Int {
        return monitorRepository.getActiveProtectedUsersCount()
    }
}