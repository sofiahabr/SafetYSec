package com.example.safetysec.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.safetysec.domain.model.Association
import com.example.safetysec.domain.model.AssociationStatus
import com.example.safetysec.domain.model.OTPInfo
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.repository.AssociationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

class AssociationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AssociationRepository {

    companion object {
        private const val COLLECTION_ASSOCIATIONS = "associations"
        private const val COLLECTION_USERS = "users"
        private const val OTP_EXPIRATION_MINUTES = 10
    }

    override suspend fun generateOTP(
        monitorId: String,
        protectedEmail: String
    ): Result<OTPInfo> = try {
        // Generate 6-digit OTP
        val otp = String.format("%06d", Random.nextInt(0, 1000000))

        // Get protected user ID
        val protectedUser = getUserByEmail(protectedEmail).getOrThrow()

        // Calculate expiration time
        val expiresAt = Calendar.getInstance().apply {
            add(Calendar.MINUTE, OTP_EXPIRATION_MINUTES)
        }.time

        // Create pending association with OTP
        val association = hashMapOf(
            "monitorId" to monitorId,
            "protectedId" to protectedUser.id,
            "status" to AssociationStatus.PENDING.name,
            "otp" to otp,
            "otpExpiresAt" to expiresAt,
            "createdAt" to Date(),
            "updatedAt" to Date()
        )

        // Save to Firestore
        firestore.collection(COLLECTION_ASSOCIATIONS)
            .add(association)
            .await()

        Result.success(OTPInfo(otp, OTP_EXPIRATION_MINUTES))

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun validateAndCreateAssociation(
        protectedId: String,
        otp: String,
        monitorEmail: String
    ): Result<Association> {
        return try {

            // Get monitor user
            val monitorUser = getUserByEmail(monitorEmail).getOrThrow()

            // Find pending association with matching OTP
            val querySnapshot = firestore.collection(COLLECTION_ASSOCIATIONS)
                .whereEqualTo("monitorId", monitorUser.id)
                .whereEqualTo("protectedId", protectedId)
                .whereEqualTo("status", AssociationStatus.PENDING.name)
                .whereEqualTo("otp", otp)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Invalid or expired OTP"))
            }

            val document = querySnapshot.documents.first()
            val association = document.toObject(Association::class.java)
                ?: return Result.failure(Exception("Failed to parse association"))

            // Check if OTP is expired
            val now = Date()
            if (association.otpExpiresAt != null && now.after(association.otpExpiresAt)) {
                return Result.failure(Exception("OTP has expired"))
            }

            // Update association to ACTIVE and remove OTP
            val updates = hashMapOf<String, Any>(
                "status" to AssociationStatus.ACTIVE.name,
                "otp" to "",
                "otpExpiresAt" to "",
                "updatedAt" to Date()
            )

            document.reference.update(updates).await()

            // Return updated association
            val updatedAssociation = association.copy(
                id = document.id,
                status = AssociationStatus.ACTIVE,
                otp = null,
                otpExpiresAt = null,
                updatedAt = Date()
            )

            Result.success(updatedAssociation)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAssociationsForUser(userId: String): Flow<List<Association>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_ASSOCIATIONS)
            .whereEqualTo("status", AssociationStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val associations = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Association::class.java)?.copy(id = doc.id)
                    }
                    ?.filter { it.monitorId == userId || it.protectedId == userId }
                    ?: emptyList()

                trySend(associations)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getAssociationsAsMonitor(monitorId: String): Flow<List<Association>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_ASSOCIATIONS)
            .whereEqualTo("monitorId", monitorId)
            .whereEqualTo("status", AssociationStatus.ACTIVE.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val associations = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Association::class.java)?.copy(id = doc.id)
                    }
                    ?: emptyList()

                trySend(associations)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getAssociationsAsProtected(protectedId: String): Flow<List<Association>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_ASSOCIATIONS)
            .whereEqualTo("protectedId", protectedId)
            .whereEqualTo("status", AssociationStatus.ACTIVE.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val associations = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Association::class.java)?.copy(id = doc.id)
                    }
                    ?: emptyList()

                trySend(associations)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun cancelAssociation(
        associationId: String,
        userId: String
    ): Result<Unit> {
        return try {

            // Get association document
            val document = firestore.collection(COLLECTION_ASSOCIATIONS)
                .document(associationId)
                .get()
                .await()

            if (!document.exists()) {
                return Result.failure(Exception("Association not found"))
            }

            val association = document.toObject(Association::class.java)
                ?: return Result.failure(Exception("Failed to parse association"))

            // Verify user is part of the association
            if (association.monitorId != userId && association.protectedId != userId) {
                return Result.failure(Exception("Unauthorized to cancel this association"))
            }

            // Update status to CANCELLED
            document.reference.update(
                mapOf(
                    "status" to AssociationStatus.CANCELLED.name,
                    "updatedAt" to Date()
                )
            ).await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkAssociationExists(
        monitorId: String,
        protectedId: String
    ): Result<Boolean> = try {

        val querySnapshot = firestore.collection(COLLECTION_ASSOCIATIONS)
            .whereEqualTo("monitorId", monitorId)
            .whereEqualTo("protectedId", protectedId)
            .whereEqualTo("status", AssociationStatus.ACTIVE.name)
            .get()
            .await()

        Result.success(!querySnapshot.isEmpty)

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserByEmail(email: String): Result<User> {
        return try {

            val querySnapshot = firestore.collection(COLLECTION_USERS)
                .whereEqualTo("email", email.lowercase())
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("User not found with email: $email"))
            }

            val document = querySnapshot.documents.first()
            val user = document.toObject(User::class.java)?.copy(id = document.id)
                ?: return Result.failure(Exception("Failed to parse user"))

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}