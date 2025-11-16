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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job

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
    private val changePasswordUseCase: ChangePasswordUseCase
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
}