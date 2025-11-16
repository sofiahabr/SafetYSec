package com.example.safetysec.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Change Password Screen
 *
 * Allows users to change their password with validation:
 * - Current password verification
 * - New password strength validation
 * - Password confirmation matching
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    // Form state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Error state
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // UI state
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Change Password",
                onNavigationClick = {
                    navController.navigateUp()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Alert
            if (showSuccessAlert) {
                SuccessAlert(
                    message = "Password changed successfully!",
                    onDismiss = {
                        showSuccessAlert = false
                        navController.navigateUp()
                    }
                )
            }

            // Error Alert
            if (showErrorAlert) {
                ErrorAlert(
                    message = errorMessage,
                    onDismiss = {
                        showErrorAlert = false
                    }
                )
            }

            // Info Card
            InfoAlert(
                title = "Password Requirements",
                message = "• At least 8 characters\n• Contains uppercase and lowercase\n• Contains at least one number\n• Contains special character"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Password
            PasswordTextField(
                value = currentPassword,
                onValueChange = {
                    currentPassword = it
                    currentPasswordError = null
                },
                label = "Current Password",
                isError = currentPasswordError != null,
                errorMessage = currentPasswordError,
                imeAction = ImeAction.Next
            )

            // New Password
            PasswordTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    newPasswordError = null
                },
                label = "New Password",
                isError = newPasswordError != null,
                errorMessage = newPasswordError,
                imeAction = ImeAction.Next
            )

            // Password Strength Indicator
            if (newPassword.isNotEmpty()) {
                PasswordStrengthIndicator(password = newPassword)
            }

            // Confirm Password
            PasswordTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = "Confirm New Password",
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Change Password Button
            PrimaryButton(
                text = "Change Password",
                onClick = {
                    // Validate
                    var isValid = true

                    // Current password check
                    if (currentPassword.isBlank()) {
                        currentPasswordError = "Current password is required"
                        isValid = false
                    }

                    // New password validation
                    val passwordValidation = validatePassword(newPassword)
                    if (!passwordValidation.isValid) {
                        newPasswordError = passwordValidation.message
                        isValid = false
                    }

                    // Confirm password check
                    if (newPassword != confirmPassword) {
                        confirmPasswordError = "Passwords do not match"
                        isValid = false
                    }

                    // Same password check
                    if (currentPassword == newPassword) {
                        newPasswordError = "New password must be different from current password"
                        isValid = false
                    }

                    if (isValid) {
                        isSaving = true

                        // Simulate password change (replace with Firebase later)
                        // In real app: verify current password with Firebase Auth
                        // For now, just show success

                        // Simulate API call delay
                        scope.launch {
                            delay(1000)
                            isSaving = false
                            showSuccessAlert = true
                        }

                    }
                },
                isLoading = isSaving
            )

            // Cancel Button
            CustomTextButton(
                text = "Cancel",
                onClick = {
                    navController.navigateUp()
                }
            )
        }
    }
}

/**
 * Password Strength Indicator
 */
@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Password Strength: ${strength.label}",
            style = MaterialTheme.typography.bodySmall,
            color = strength.color
        )

        LinearProgressIndicator(
            progress = strength.progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = strength.color
        )
    }
}

/**
 * Password Strength Data
 */
private data class PasswordStrength(
    val label: String,
    val progress: Float,
    val color: androidx.compose.ui.graphics.Color
)

/**
 * Calculate password strength
 */
private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0

    // Length check
    if (password.length >= 8) score++
    if (password.length >= 12) score++

    // Contains lowercase
    if (password.any { it.isLowerCase() }) score++

    // Contains uppercase
    if (password.any { it.isUpperCase() }) score++

    // Contains number
    if (password.any { it.isDigit() }) score++

    // Contains special character
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 2 -> PasswordStrength(
            "Weak",
            0.33f,
            androidx.compose.ui.graphics.Color.Red
        )
        score <= 4 -> PasswordStrength(
            "Medium",
            0.66f,
            androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        )
        else -> PasswordStrength(
            "Strong",
            1.0f,
            androidx.compose.ui.graphics.Color.Green
        )
    }
}

/**
 * Password Validation Result
 */
private data class PasswordValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * Validate password requirements
 */
private fun validatePassword(password: String): PasswordValidationResult {
    return when {
        password.length < 8 -> PasswordValidationResult(
            false,
            "Password must be at least 8 characters"
        )
        !password.any { it.isUpperCase() } -> PasswordValidationResult(
            false,
            "Password must contain an uppercase letter"
        )
        !password.any { it.isLowerCase() } -> PasswordValidationResult(
            false,
            "Password must contain a lowercase letter"
        )
        !password.any { it.isDigit() } -> PasswordValidationResult(
            false,
            "Password must contain a number"
        )
        !password.any { !it.isLetterOrDigit() } -> PasswordValidationResult(
            false,
            "Password must contain a special character"
        )
        else -> PasswordValidationResult(true, "")
    }
}