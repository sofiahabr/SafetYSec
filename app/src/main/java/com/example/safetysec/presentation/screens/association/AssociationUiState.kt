package com.example.safetysec.presentation.screens.association

import com.example.safetysec.domain.model.Association

/**
 * UI State for Association Screen
 */
data class AssociationUiState(
    val isMonitorMode: Boolean = true, // true = generating OTP, false = entering OTP
    val protectedEmail: String = "",
    val monitorEmail: String = "",
    val otp: String = "",
    val generatedOTP: String? = null,
    val associations: List<Association> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)