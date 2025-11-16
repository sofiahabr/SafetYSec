package com.example.safetysec.domain.model

/**
 * User Model
 *
 * Represents a user in the SafetYSec system
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val profileImageUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * User Role
 * Defines what type of user this is
 */
enum class UserRole {
    MONITOR,      // Can monitor others
    PROTECTED,    // Being monitored
    BOTH          // Can be both monitor and protected
}

/**
 * Mock User Data
 * For testing without Firebase authentication
 */
object MockUserData {

    /**
     * Current logged-in user (mock)
     */
    var currentUser = User(
        id = "user_123",
        name = "John Doe",
        email = "john.doe@example.com",
        phone = "+1 234 567 8900",
        role = UserRole.BOTH,
        profileImageUrl = null,
        isEmailVerified = true,
        createdAt = System.currentTimeMillis() - 86400000 // 1 day ago
    )

    /**
     * Update current user
     * (Later will be replaced with Firebase update)
     */
    fun updateUser(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        role: UserRole? = null
    ) {
        currentUser = currentUser.copy(
            name = name ?: currentUser.name,
            email = email ?: currentUser.email,
            phone = phone ?: currentUser.phone,
            role = role ?: currentUser.role
        )
    }

    /**
     * Get user role display name
     */
    fun getRoleDisplayName(role: UserRole): String {
        return when (role) {
            UserRole.MONITOR -> "Monitor"
            UserRole.PROTECTED -> "Protected"
            UserRole.BOTH -> "Monitor & Protected"
        }
    }

    /**
     * Get user initials for avatar
     */
    fun getUserInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}"
            parts.isNotEmpty() -> "${parts[0].first()}"
            else -> "U"
        }.uppercase()
    }
}