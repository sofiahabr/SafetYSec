package com.example.safetysec.domain.usecase.auth

import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.repository.AuthRepository
import javax.inject.Inject

class DeleteUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AuthResult<Unit> {
        return authRepository.deleteUserProfile()
    }
}