package com.example.safetysec.domain.usecase.association

import com.example.safetysec.domain.repository.AssociationRepository
import javax.inject.Inject

class RemoveAssociationUseCase @Inject constructor(
    private val associationRepository: AssociationRepository
) {
    /**
     * Remove/Cancel an association
     *
     * Business Rules:
     * - Either monitor or protected can cancel the association
     * - When cancelled, all rules associated with this relationship become inactive
     * - Alert history is preserved
     */
    suspend operator fun invoke(
        associationId: String,
        userId: String
    ): Result<Unit> {
        return associationRepository.cancelAssociation(associationId, userId)
    }
}