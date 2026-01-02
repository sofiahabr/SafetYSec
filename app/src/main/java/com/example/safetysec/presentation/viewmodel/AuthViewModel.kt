package com.example.safetysec.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import com.example.safetysec.domain.usecase.auth.LoginUseCase
import com.example.safetysec.domain.usecase.auth.LogoutUseCase
import com.example.safetysec.domain.usecase.auth.RegisterUseCase
import com.example.safetysec.domain.usecase.auth.UpdateProfileUseCase
import com.example.safetysec.domain.usecase.auth.ChangePasswordUseCase
import com.example.safetysec.domain.usecase.auth.DeleteUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.safetysec.domain.repository.AuthRepository

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteUserProfileUseCase: DeleteUserProfileUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var currentUserJob: Job? = null

    init {
        checkCurrentUser()
    }

    fun updateProfile(
        email: String,
        name: String,
        phone: String,
        role: String
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = updateProfileUseCase(email, name, phone, role)
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        user = result.data,
                        isLoading = false
                    )
                }

                is AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }

                is AuthResult.Loading -> {
                    _authState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = changePasswordUseCase(currentPassword, newPassword)
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        user = result.data,
                        isLoading = false
                    )
                }

                is AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }

                else -> {}
            }
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            val result = loginUseCase(email, password)

            when (result) {
                is AuthResult.Success -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true
                        )
                    }

                    // Register FCM token after successful login
                    result.data?.id?.let { userId ->
                        registerFCMToken(userId)
                    }
                }

                is AuthResult.Error -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                is AuthResult.Loading -> {
                    _authState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun register(email: String, password: String, name: String, phone: String, role: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            val result = registerUseCase(email, password, name, phone, role)

            when (result) {
                is AuthResult.Success -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true,
                            registrationSuccess = true
                        )
                    }

                    // Register FCM token after successful registration
                    result.data?.id?.let { userId ->
                        registerFCMToken(userId)
                    }
                }

                is AuthResult.Error -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                is AuthResult.Loading -> {
                    _authState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }

            // Clear FCM token before logout
            _authState.value.user?.id?.let { userId ->
                clearFCMToken(userId)
            }

            val result = logoutUseCase()

            when (result) {
                is AuthResult.Success -> {
                    currentUserJob?.cancel()
                    currentUserJob = null

                    _authState.value = AuthState(
                        isLoading = false,
                        user = null,
                        error = null,
                        isAuthenticated = false
                    )
                    kotlinx.coroutines.delay(100)
                    checkCurrentUser()
                }

                is AuthResult.Error -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                is AuthResult.Loading -> {
                    _authState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun deleteUserProfile() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = deleteUserProfileUseCase()
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        user = null,
                        isAuthenticated = false,
                        isLoading = false
                    )
                }
                is AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }

    private fun checkCurrentUser() {
        currentUserJob?.cancel()

        currentUserJob = viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                val newState = AuthState(
                    user = user,
                    isAuthenticated = user != null
                )
                _authState.update {
                    it.copy(
                        user = user,
                        isAuthenticated = user != null
                    )
                }
            }
        }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }

    /**
     * Register FCM token for push notifications
     */
    private fun registerFCMToken(userId: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .await()

                Log.d("AuthViewModel", "FCM token registered: ${token.take(20)}...")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to register FCM token", e)
                // Don't fail login if FCM registration fails
            }
        }
    }

    /**
     * Clear FCM token on logout
     */
    private fun clearFCMToken(userId: String) {
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", null)
                    .await()

                Log.d("AuthViewModel", "FCM token cleared")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to clear FCM token", e)
            }
        }
    }

    /**
     * Update alert cancellation PIN
     */
    fun updateCancellationPin(newPin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.updateCancellationPin(newPin)

            when (result) {
                is AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        user = result.data,
                        isLoading = false
                    )
                    onResult(true, null)
                }
                is AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                    onResult(false, result.message)
                }
                else -> {
                    onResult(false, "Unknown error")
                }
            }
        }
    }

    /**
     * Send Password Reset Email
     *
     * Sends a password reset link to the user's email address.
     * Firebase handles the reset link generation and email delivery.
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _authState.update { it.copy(
                    isLoading = false,
                    error = "Please enter a valid email address"
                ) }
                return@launch
            }

            try {
                Log.d("AuthViewModel", "Sending password reset email to: $email")

                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(email)
                    .await()

                Log.d("AuthViewModel", "Password reset email sent successfully!")
                _authState.update { it.copy(
                    isLoading = false,
                    error = null
                ) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to send password reset email", e)

                val errorMessage = when {
                    e.message?.contains("There is no user record") == true -> {
                        "No account found with this email address"
                    }
                    e.message?.contains("too many attempts") == true -> {
                        "Too many attempts. Please try again later."
                    }
                    e.message?.contains("badly formatted") == true -> {
                        "Please enter a valid email address"
                    }
                    else -> {
                        e.message ?: "Failed to send reset email"
                    }
                }

                _authState.update { it.copy(
                    isLoading = false,
                    error = errorMessage
                ) }
            }
        }
    }

    /**
     * Reset Password with Token
     *
     * Note: Firebase handles password reset via email link.
     * This method is called after user clicks the email link and enters new password.
     */
    fun resetPasswordWithToken(code: String, newPassword: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            // Validate password strength
            if (newPassword.length < 6) {
                _authState.update { it.copy(
                    isLoading = false,
                    error = "Password must be at least 6 characters long"
                ) }
                return@launch
            }

            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .confirmPasswordReset(code, newPassword)
                    .await()

                // Password reset successful
                _authState.update { it.copy(
                    isLoading = false,
                    error = null
                ) }
            } catch (e: Exception) {
                // Handle errors
                val errorMessage = when {
                    e.message?.contains("password is invalid") == true -> {
                        "Password must be at least 6 characters"
                    }
                    e.message?.contains("code is invalid") == true -> {
                        "Reset link has expired. Please request a new one."
                    }
                    else -> {
                        e.message ?: "Failed to reset password"
                    }
                }

                _authState.update { it.copy(
                    isLoading = false,
                    error = errorMessage
                ) }
            }
        }
    }

    /**
     * Verify Password Reset Code
     *
     * Verifies that the reset code is valid before allowing password change.
     */
    fun verifyPasswordResetCode(
        code: String,
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val email = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .verifyPasswordResetCode(code)
                    .await()

                onSuccess(email)
            } catch (e: Exception) {
                // Code is invalid or expired
                val errorMessage = when {
                    e.message?.contains("code is invalid") == true -> {
                        "Reset link has expired or is invalid"
                    }
                    else -> {
                        e.message ?: "Invalid reset code"
                    }
                }
                onError(errorMessage)
            }
        }
    }

    /**
     * Clear Password Recovery Error
     *
     * Clear any error messages from password recovery attempts
     */
    fun clearPasswordRecoveryError() {
        _authState.update { it.copy(error = null) }
    }
}