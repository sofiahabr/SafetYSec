package com.example.safetysec.domain.usecase.association

import com.example.safetysec.domain.model.OTPInfo
import com.example.safetysec.domain.repository.AssociationRepository
import javax.inject.Inject

class GenerateOTPUseCase @Inject constructor(
    private val associationRepository: AssociationRepository
) {
    /**
     * Generate OTP for association
     *
     * Business Rules:
     * - Monitor cannot create association with themselves
     * - Protected user must exist in the system
     * - Only active associations should be checked (not pending or cancelled)
     */
    suspend operator fun invoke(
        monitorId: String,
        protectedEmail: String,
        monitorEmail: String
    ): Result<OTPInfo> {

        // Rule: User cannot be their own monitor
        if (monitorEmail.equals(protectedEmail, ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("A user cannot be their own monitor")
            )
        }

        // Check if protected user exists
        val protectedUserResult = associationRepository.getUserByEmail(protectedEmail)
        if (protectedUserResult.isFailure) {
            return Result.failure(
                Exception("Protected user with email $protectedEmail not found")
            )
        }

        val protectedUser = protectedUserResult.getOrNull()!!

        // Check if association already exists
        val existsResult = associationRepository.checkAssociationExists(
            monitorId,
            protectedUser.id
        )

        if (existsResult.isSuccess && existsResult.getOrNull() == true) {
            return Result.failure(
                Exception("An association already exists with this user")
            )
        }

        // Generate OTP
        return associationRepository.generateOTP(monitorId, protectedEmail)
    }
}