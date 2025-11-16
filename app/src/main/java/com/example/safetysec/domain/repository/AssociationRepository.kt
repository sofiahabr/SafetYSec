package com.example.safetysec.domain.repository

import com.example.safetysec.domain.model.Association
import com.example.safetysec.domain.model.AssociationStatus
import com.example.safetysec.domain.model.OTPInfo
import com.example.safetysec.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AssociationRepository {

    /**
     * Generate OTP for association request
     * @param monitorId ID of the monitor user
     * @param protectedEmail Email of the protected user to associate with
     * @return OTPInfo containing the generated OTP and expiration time
     */
    suspend fun generateOTP(monitorId: String, protectedEmail: String): Result<OTPInfo>

    /**
     * Validate OTP and create association
     * @param protectedId ID of the protected user
     * @param otp OTP code to validate
     * @param monitorEmail Email of the monitor who generated the OTP
     * @return Association object if successful
     */
    suspend fun validateAndCreateAssociation(
        protectedId: String,
        otp: String,
        monitorEmail: String
    ): Result<Association>

    /**
     * Get all associations for a user (as monitor or protected)
     * @param userId User ID
     * @return Flow of associations
     */
    fun getAssociationsForUser(userId: String): Flow<List<Association>>

    /**
     * Get associations where user is monitor
     */
    fun getAssociationsAsMonitor(monitorId: String): Flow<List<Association>>

    /**
     * Get associations where user is protected
     */
    fun getAssociationsAsProtected(protectedId: String): Flow<List<Association>>

    /**
     * Cancel an association
     * @param associationId Association to cancel
     * @param userId User requesting cancellation (must be monitor or protected in the association)
     */
    suspend fun cancelAssociation(associationId: String, userId: String): Result<Unit>

    /**
     * Check if association exists between two users
     */
    suspend fun checkAssociationExists(monitorId: String, protectedId: String): Result<Boolean>

    /**
     * Get user details by email (for association lookup)
     */
    suspend fun getUserByEmail(email: String): Result<User>
}