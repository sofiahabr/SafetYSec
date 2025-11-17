package com.example.safetysec.data.repository

import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: String
    ): AuthResult<User> = try {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

        val userRole = UserRole.valueOf(role.uppercase())
        val user = User(
            id = userId,
            email = email,
            name = name,
            phone = phone,
            role = userRole,
            alertCancellationCode = generateRandomCode()
        )

        firestore.collection("users").document(userId).set(user.toMap()).await()
        AuthResult.Success(user)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Registration failed")
    }

    override suspend fun deleteUserProfile(): AuthResult<Unit> = try {
        val currentUser = firebaseAuth.currentUser
            ?: return AuthResult.Error("No user currently logged in")

        val userId = currentUser.uid

        firestore.collection("users").document(userId).delete().await()
        currentUser.delete().await()

        AuthResult.Success(Unit)
    } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
        AuthResult.Error("Please log in again before deleting your account")
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Failed to delete profile")
    }

    override suspend fun updateUser(
        email: String,
        name: String,
        phone: String,
        role: String
    ): AuthResult<User> = try {
        val currentUser = firebaseAuth.currentUser
            ?: return AuthResult.Error("No user currently logged in")

        val userId = currentUser.uid

        // Step 1: Update profile information (name)
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        currentUser.updateProfile(profileUpdates).await()

        // Step 2: Update email if it has changed
        if (currentUser.email != email) {
            currentUser.updateEmail(email).await()
        }


        // Step 4: Create user role from string
        val userRole = try {
            UserRole.valueOf(role.uppercase())
        } catch (e: IllegalArgumentException) {
            return AuthResult.Error("Invalid role: $role")
        }

        // Step 5: Create user object with all fields
        val user = User(
            id = userId,
            email = email,
            name = name,
            phone = phone,  // Include phone field
            role = userRole,
            isEmailVerified = currentUser.isEmailVerified,
            alertCancellationCode = generateRandomCode()
        )

        // Step 6: Update Firestore with new user data
        firestore.collection("users")
            .document(userId)
            .set(user.toMap(), com.google.firebase.firestore.SetOptions.merge())
            .await()

        AuthResult.Success(user)

    } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
        AuthResult.Error("Email is already in use by another account")
    } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
        AuthResult.Error("Invalid credentials provided")
    } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
        AuthResult.Error("Password is too weak. Use at least 6 characters")
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Failed to update user profile")
    }


    override suspend fun login(
        email: String,
        password: String
    ): AuthResult<User> = try {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("Login failed")

        val userDoc = firestore.collection("users").document(userId).get().await()
        val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

        AuthResult.Success(user)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Login failed")
    }

    override suspend fun logout(): AuthResult<Unit> = try {
        firebaseAuth.signOut()
        AuthResult.Success(Unit)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Logout failed")
    }

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                firestore.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { snapshot ->
                        val user = snapshot.toObject(User::class.java)
                        trySend(user)
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            } else {
                trySend(null)
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override fun isUserAuthenticated(): Flow<Boolean> = flow {
        emit(firebaseAuth.currentUser != null)
    }

    override fun getUserRole(): Flow<String?> = flow {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            try {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)
                emit(user?.role?.name)
            } catch (e: Exception) {
                emit(null)
            }
        } else {
            emit(null)
        }
    }
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): AuthResult<User> = try {
        val currentUser = firebaseAuth.currentUser
            ?: return AuthResult.Error("No user currently logged in")

        val userId = currentUser.uid

        // Verify current password by re-authenticating
        val credentials = com.google.firebase.auth.EmailAuthProvider.getCredential(
            currentUser.email ?: "",
            currentPassword
        )
        currentUser.reauthenticate(credentials).await()

        // Update to new password
        currentUser.updatePassword(newPassword).await()

        // Fetch updated user from Firestore
        val userDoc = firestore.collection("users").document(userId).get().await()
        val user = userDoc.toObject(User::class.java)
            ?: throw Exception("User not found")

        AuthResult.Success(user)
    } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
        AuthResult.Error("Current password is incorrect")
    } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
        AuthResult.Error("New password is too weak. Use at least 6 characters")
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Failed to change password")
    }

    private fun generateRandomCode(): String {
        return (100000..999999).random().toString()
    }

    private fun User.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "email" to email,
        "name" to name,
        "phone" to phone,
        "role" to role.name,
        "alertCancellationCode" to alertCancellationCode
    )
}