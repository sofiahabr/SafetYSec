package com.example.safetysec.presentation.screens.association

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.usecase.association.*
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

            val currentUser = getCurrentUserUseCase() ?: run {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "User not authenticated. Please login again."
                    )
                }
                return@launch
            }

            // Validate email format
            if (!isValidEmail(_viewState.value.protectedEmail)) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Please enter a valid email address"
                    )
                }
                return@launch
            }

            val result = generateOTPUseCase(
                monitorId = currentUser.id,
                protectedEmail = _viewState.value.protectedEmail,
                monitorEmail = currentUser.email
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
        }
    }

    /**
     * Validate OTP and Create Association
     */
    fun validateOTP() {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            val currentUser = getCurrentUserUseCase() ?: run {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "User not authenticated. Please login again."
                    )
                }
                return@launch
            }

            // Validate inputs
            if (!isValidEmail(_viewState.value.monitorEmail)) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Please enter a valid monitor email address"
                    )
                }
                return@launch
            }

            if (_viewState.value.otp.length != 6) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "OTP must be 6 digits"
                    )
                }
                return@launch
            }

            val result = validateOTPUseCase(
                protectedId = currentUser.id,
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
        }
    }

    /**
     * Remove Association
     */
    fun removeAssociation(associationId: String) {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            val currentUser = getCurrentUserUseCase() ?: run {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                }
                return@launch
            }

            val result = removeAssociationUseCase(associationId, currentUser.id)

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
        }
    }

    /**
     * Load Associations
     */
    private fun loadAssociations() {
        viewModelScope.launch {
            val currentUser = getCurrentUserUseCase() ?: return@launch

            getAssociationsUseCase.getAllForUser(currentUser.id)
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
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}