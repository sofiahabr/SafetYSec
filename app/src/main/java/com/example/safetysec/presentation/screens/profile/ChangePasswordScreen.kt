package com.example.safetysec.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.viewmodel.AuthViewModel
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Get validation error strings
    val errorCurrentPasswordRequired = stringResource(R.string.error_current_password_required)
    val errorPasswordsDontMatch = stringResource(R.string.error_passwords_dont_match)
    val errorPasswordMustDiffer = stringResource(R.string.error_password_must_differ)

    // Form state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Error state
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // UI state
    var showSuccessAlert by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(false) }

    if (wasLoading && !authState.isLoading && authState.error == null) {
        showSuccessAlert = true
    }
    wasLoading = authState.isLoading

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = stringResource(R.string.change_password),
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
                    message = stringResource(R.string.password_changed_success),
                    onDismiss = {
                        showSuccessAlert = false
                        navController.navigateUp()
                    }
                )
            }

            // Error Alert
            if (authState.error != null) {
                ErrorAlert(
                    message = authState.error ?: stringResource(R.string.error_unknown),
                    onDismiss = {
                        authViewModel.clearError()
                    }
                )
            }

            // Info Card
            InfoAlert(
                title = stringResource(R.string.password_requirements),
                message = stringResource(R.string.password_requirements_list)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Password
            PasswordTextField(
                value = currentPassword,
                onValueChange = {
                    currentPassword = it
                    currentPasswordError = null
                },
                label = stringResource(R.string.current_password),
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
                label = stringResource(R.string.new_password),
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
                label = stringResource(R.string.confirm_new_password),
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = stringResource(R.string.change_password),
                onClick = {
                    var isValid = true

                    if (currentPassword.isBlank()) {
                        currentPasswordError = errorCurrentPasswordRequired
                        isValid = false
                    }

                    val passwordValidation = validatePassword(newPassword)
                    if (!passwordValidation.isValid) {
                        newPasswordError = passwordValidation.message
                        isValid = false
                    }

                    if (newPassword != confirmPassword) {
                        confirmPasswordError = errorPasswordsDontMatch
                        isValid = false
                    }

                    if (currentPassword == newPassword) {
                        newPasswordError = errorPasswordMustDiffer
                        isValid = false
                    }

                    if (isValid) {
                        authViewModel.changePassword(
                            currentPassword,
                            newPassword

                        )
                    }
                },
                isLoading = authState.isLoading
            )

            // Cancel Button
            CustomTextButton(
                text = stringResource(R.string.cancel),
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
fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.password_strength, strength.label),
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
data class PasswordStrength(
    val label: String,
    val progress: Float,
    val color: androidx.compose.ui.graphics.Color
)

/**
 * Calculate password strength
 */
fun calculatePasswordStrength(password: String): PasswordStrength {
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