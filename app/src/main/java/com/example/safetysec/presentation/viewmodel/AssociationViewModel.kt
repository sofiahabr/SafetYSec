package com.example.safetysec.presentation.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.usecase.association.GenerateOTPUseCase
import com.example.safetysec.domain.usecase.association.GetAssociationsUseCase
import com.example.safetysec.domain.usecase.association.RemoveAssociationUseCase
import com.example.safetysec.domain.usecase.association.ValidateOTPUseCase
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import com.example.safetysec.presentation.screens.association.AssociationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssociationViewModel @Inject constructor(
    private val generateOTPUseCase: GenerateOTPUseCase,
    private val validateOTPUseCase: ValidateOTPUseCase,
    private val removeAssociationUseCase: RemoveAssociationUseCase,
    private val getAssociationsUseCase: GetAssociationsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _viewState = MutableStateFlow(AssociationUiState())
    val viewState: StateFlow<AssociationUiState> = _viewState.asStateFlow()

    // Current user flow
    private val currentUserFlow = getCurrentUserUseCase()

    init {
        loadAssociations()
    }

    /**
     * Toggle between Monitor and Protected modes
     */
    fun toggleMode() {
        _viewState.update {
            it.copy(
                isMonitorMode = !it.isMonitorMode,
                error = null,
                generatedOTP = null,
                protectedEmail = "",
                monitorEmail = "",
                otp = ""
            )
        }
    }

    /**
     * Update protected user email
     */
    fun updateProtectedEmail(email: String) {
        _viewState.update { it.copy(protectedEmail = email, error = null) }
    }

    /**
     * Update monitor email
     */
    fun updateMonitorEmail(email: String) {
        _viewState.update { it.copy(monitorEmail = email, error = null) }
    }

    /**
     * Update OTP - Only allow digits, max 6 characters
     */
    fun updateOTP(otp: String) {
        val filtered = otp.filter { it.isDigit() }.take(6)
        _viewState.update { it.copy(otp = filtered, error = null) }
    }

    /**
     * Generate OTP for Protected User
     */
    fun generateOTP() {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            // Get current user from flow
            currentUserFlow.first { user ->
                if (user == null) {
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not authenticated. Please login again."
                        )
                    }
                    return@first true
                }

                // Validate email format
                if (!isValidEmail(_viewState.value.protectedEmail)) {
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = "Please enter a valid email address"
                        )
                    }
                    return@first true
                }

                val result = generateOTPUseCase(
                    monitorId = user.id,
                    protectedEmail = _viewState.value.protectedEmail,
                    monitorEmail = user.email
                )

                result.fold(
                    onSuccess = { otpInfo ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                generatedOTP = otpInfo.otp,
                                error = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to generate OTP. Please try again."
                            )
                        }
                    }
                )

                true
            }
        }
    }

    /**
     * Validate OTP and Create Association
     */
    fun validateOTP() {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            // Get current user from flow
            currentUserFlow.first { user ->
                if (user == null) {
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not authenticated. Please login again."
                        )
                    }
                    return@first true
                }

                // Validate inputs
                if (!isValidEmail(_viewState.value.monitorEmail)) {
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = "Please enter a valid monitor email address"
                        )
                    }
                    return@first true
                }

                if (_viewState.value.otp.length != 6) {
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = "OTP must be 6 digits"
                        )
                    }
                    return@first true
                }

                val result = validateOTPUseCase(
                    protectedId = user.id,
                    otp = _viewState.value.otp,
                    monitorEmail = _viewState.value.monitorEmail
                )

                result.fold(
                    onSuccess = { association ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                otp = "",
                                monitorEmail = "",
                                error = null
                            )
                        }
                        // Associations will be automatically updated via Flow
                    },
                    onFailure = { exception ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to validate OTP. Please check the code and try again."
                            )
                        }
                    }
                )

                true
            }
        }
    }

    /**
     * Remove Association
     */
    fun removeAssociation(associationId: String) {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            // Get current user from flow
            currentUserFlow.first { user ->
                if (user == null) {
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not authenticated"
                        )
                    }
                    return@first true
                }

                val result = removeAssociationUseCase(associationId, user.id)

                result.fold(
                    onSuccess = {
                        _viewState.update { it.copy(isLoading = false) }
                        // Associations will be automatically updated via Flow
                    },
                    onFailure = { exception ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to remove association. Please try again."
                            )
                        }
                    }
                )

                true
            }
        }
    }

    /**
     * Load Associations
     */
    private fun loadAssociations() {
        viewModelScope.launch {
            currentUserFlow.collectLatest { user ->
                if (user == null) {
                    _viewState.update {
                        it.copy(error = "Failed to load associations. Please login again.")
                    }
                    return@collectLatest
                }

                getAssociationsUseCase.getAllForUser(user.id)
                    .catch { exception ->
                        _viewState.update {
                            it.copy(
                                error = exception.message ?: "Failed to load associations"
                            )
                        }
                    }
                    .collect { associations ->
                        _viewState.update { it.copy(associations = associations) }
                    }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    /**
     * Clear generated OTP
     */
    fun clearGeneratedOTP() {
        _viewState.update { it.copy(generatedOTP = null) }
    }

    /**
     * Email validation helper
     */
    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}