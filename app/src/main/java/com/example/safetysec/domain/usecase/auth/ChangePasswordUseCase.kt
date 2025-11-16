package com.example.safetysec.domain.usecase.auth

import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String
    ): AuthResult<User> {
        return authRepository.changePassword(currentPassword, newPassword)
    }
}
