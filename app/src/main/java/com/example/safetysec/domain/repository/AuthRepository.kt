package com.example.safetysec.domain.repository

import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /**
     * Register a new user with email, password, name and role
     */

    suspend fun register(
        email: String,
        password: String,
        name: String,
        role: String
    ): AuthResult<User>

    /**
     * Log in with email and password
     */

    suspend fun login(
        email: String,
        password: String
    ): AuthResult<User>

    /**
     * Logout current user
     */

    suspend fun logout(): AuthResult<Unit>

    /**
     * Get the currently logged in user
     */

    fun getCurrentUser(): Flow<User?>

    /**
     * Check if user is authenticated
     */

    fun isUserAuthenticated(): Flow<Boolean>

    /**
     * Get role of current user
     */
    fun getUserRole(): Flow<String?>
}