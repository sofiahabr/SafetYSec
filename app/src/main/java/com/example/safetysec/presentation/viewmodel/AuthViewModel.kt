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
}