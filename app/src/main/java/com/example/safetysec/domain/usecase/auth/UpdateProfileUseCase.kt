package com.example.safetysec.domain.usecase.auth

import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        name: String,
        phone: String,
        role: String
    ): AuthResult<User> {
        return authRepository.updateUser(email, name, phone, role)
    }
}