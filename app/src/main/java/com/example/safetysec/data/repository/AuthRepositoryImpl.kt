package com.example.safetysec.data.repository

import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override suspend fun register(
        email: String,
        password: String,
        name: String,
        role: String
    ) : AuthResult<User> = try {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

        val userRole = UserRole(valueOf(role.uppercase()))
        val user = User(
            id = userId,
            email = email,
            name = name,
            role = userRole
            alertCancellationCode = generateRandomCode()
        )
        firestore.collection("users").document(userId).set(user.toMap()).await()
        AuthResult.Success(user)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Unknown error occurred")
    }

    override suspend fun login(
        email: String,
        password: String
    ): AuthResult<User> = try {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()

        AuthResult.Success(user)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Login failed")
    }

    override suspend fun logout(): AuthResult<Unit> = try{
        firebaseAuth.signOut()
        AuthResult.Success(Unit)
        } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Logout failed")
    }

    override fun getCurrentUser(): Flow<User?> = flow {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            try {

                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                emit(userDoc.toObject(User::class.java))
            } catch (e: Exception) {
                emit(null)
            }
        } else {
            emit(null)
        }
    }
    override fun isUserAuthenticated(): Flow<Boolean> = flow {
        emit(firebaseAuth.currentUser != null)
    }
}