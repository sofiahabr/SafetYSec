package com.example.safetysec.domain.usecase.association

import com.example.safetysec.domain.model.Association
import com.example.safetysec.domain.repository.AssociationRepository
import javax.inject.Inject

class ValidateOTPUseCase @Inject constructor(
    private val associationRepository: AssociationRepository
) {
    /**
     * Validate OTP and create association
     *
     * Business Rules:
     * - OTP must be valid and not expired
     * - Protected user must authorize the association
     * - Once validated, association becomes ACTIVE
     */
    suspend operator fun invoke(
        protectedId: String,
        otp: String,
        monitorEmail: String
    ): Result<Association> {

        // Validate OTP format (6 digits)
        if (!otp.matches(Regex("^\\d{6}$"))) {
            return Result.failure(
                IllegalArgumentException("Invalid OTP format. Must be 6 digits.")
            )
        }

        // Get monitor user
        val monitorResult = associationRepository.getUserByEmail(monitorEmail)
        if (monitorResult.isFailure) {
            return Result.failure(
                Exception("Monitor with email $monitorEmail not found")
            )
        }

        val monitorUser = monitorResult.getOrNull()!!

        // Rule: User cannot be their own monitor
        if (monitorUser.id == protectedId) {
            return Result.failure(
                IllegalArgumentException("A user cannot be their own monitor")
            )
        }

        // Validate OTP and create association
        return associationRepository.validateAndCreateAssociation(
            protectedId,
            otp,
            monitorEmail
        )
    }
}