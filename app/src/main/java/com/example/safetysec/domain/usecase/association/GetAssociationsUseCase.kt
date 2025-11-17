package com.example.safetysec.domain.usecase.association

import com.example.safetysec.domain.model.Association
import com.example.safetysec.domain.repository.AssociationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssociationsUseCase @Inject constructor(
    private val associationRepository: AssociationRepository
) {
    fun getAsMonitor(monitorId: String): Flow<List<Association>> {
        return associationRepository.getAssociationsAsMonitor(monitorId)
    }

    fun getAsProtected(protectedId: String): Flow<List<Association>> {
        return associationRepository.getAssociationsAsProtected(protectedId)
    }

    fun getAllForUser(userId: String): Flow<List<Association>> {
        return associationRepository.getAssociationsForUser(userId)
    }
}