package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.screens.profile.PasswordStrengthIndicator
import com.example.safetysec.presentation.screens.profile.calculatePasswordStrength
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel

/**
 * Forgot Password Screen
 *
 * Step 1: Email input to request password reset link
 * Step 2: Password reset form with requirements after user clicks email link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var isEmailSent by remember { mutableStateOf(false) }
    var resetStep by remember { mutableStateOf(ResetStep.EMAIL_INPUT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.password_recovery),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                resetStep == ResetStep.EMAIL_INPUT && !isEmailSent -> {
                    ForgotPasswordInputContent(
                        email = email,
                        onEmailChange = { email = it },
                        onSendClick = {
                            if (email.isNotEmpty()) {
                                viewModel.sendPasswordResetEmail(email)
                                isEmailSent = true
                            }
                        },
                        isLoading = authState.isLoading,
                        error = authState.error
                    )
                }
                isEmailSent && resetStep == ResetStep.EMAIL_INPUT -> {
                    EmailSentSuccessContent(
                        email = email,
                        onBackClick = {
                            email = ""
                            isEmailSent = false
                            resetStep = ResetStep.EMAIL_INPUT
                        },
                        onResendClick = {
                            viewModel.sendPasswordResetEmail(email)
                        },
                        onContinueToReset = {
                            resetStep = ResetStep.PASSWORD_RESET
                        },
                        isLoading = authState.isLoading
                    )
                }
                resetStep == ResetStep.PASSWORD_RESET -> {
                    PasswordResetContent(
                        onBackClick = {
                            resetStep = ResetStep.EMAIL_INPUT
                        },
                        isLoading = authState.isLoading,
                        error = authState.error,
                        onResetSuccess = {
                            navController.navigate("password_recovery_success") {
                                popUpTo("forgot_password") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Step 1: Email Input Content
 */
@Composable
private fun ForgotPasswordInputContent(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.recover_password),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = stringResource(R.string.enter_email_reset_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Email Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = stringResource(R.string.email),
                    modifier = Modifier.size(64.dp),
                    tint = PrimaryPurple
                )
            }

            // Email Input Field
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.email)) },
                placeholder = { Text(stringResource(R.string.email_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.email)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                isError = error != null
            )

            // Error Message
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Info Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.what_happens_next),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )

                    Text(
                        text = stringResource(R.string.password_reset_steps),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Send Button
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = if (isLoading) stringResource(R.string.sending) else stringResource(R.string.send_reset_link),
                onClick = onSendClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotEmpty()
            )

            Text(
                text = stringResource(R.string.reset_link_expires_24_hours),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Step 2: Email Sent Success Content
 */
@Composable
private fun EmailSentSuccessContent(
    email: String,
    onBackClick: () -> Unit,
    onResendClick: () -> Unit,
    onContinueToReset: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                color = PrimaryPurple.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.email),
                        modifier = Modifier.size(48.dp),
                        tint = PrimaryPurple
                    )
                }
            }

            // Success Message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.check_your_email),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = stringResource(R.string.reset_link_sent_to),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryPurple
                )
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F4FF)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.what_to_do_next),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    listOf(
                        R.string.check_inbox_and_spam,
                        R.string.click_password_reset_link,
                        R.string.reset_link_will_take_you_to_form
                    ).forEach { instructionRes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryPurple
                            )
                            Text(
                                text = stringResource(instructionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Timer Info
            Text(
                text = stringResource(R.string.link_expires_24_hours),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimaryButton(
                text = stringResource(R.string.continue_to_reset_form),
                onClick = onContinueToReset,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            SecondaryButton(
                text = if (isLoading) stringResource(R.string.resending) else stringResource(R.string.didnt_receive_resend),
                onClick = onResendClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            SecondaryButton(
                text = stringResource(R.string.back_to_login),
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Step 3: Password Reset Form with Requirements
 */
@Composable
private fun PasswordResetContent(
    onBackClick: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onResetSuccess: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val passwordMismatchErrorText = stringResource(R.string.password_error_mismatch)
    val weakPasswordErrorText = stringResource(R.string.password_error_weak)
    val weakPasswordLabel = stringResource(R.string.password_strength_weak)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_new_password),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = stringResource(R.string.enter_strong_password),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Info Card - Password Requirements (Same as Registration)
            InfoAlert(
                title = stringResource(R.string.password_requirements),
                message = stringResource(R.string.password_requirements_desc)
            )

            // New Password Field
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

            // Password Strength Indicator (Same as Registration)
            if (newPassword.isNotEmpty()) {
                PasswordStrengthIndicator(password = newPassword)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm Password Field
            PasswordTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = stringResource(R.string.confirm_password),
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError,
                imeAction = ImeAction.Done
            )

            // Error Message
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Info Box - Security Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.security_tip),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )

                    Text(
                        text = stringResource(R.string.use_strong_password_keep_secure),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = if (isLoading) stringResource(R.string.saving) else stringResource(R.string.reset_password),
                onClick = {
                    var isValid = true

                    if (newPassword != confirmPassword) {
                        confirmPasswordError = passwordMismatchErrorText
                        isValid = false
                    }

                    if (calculatePasswordStrength(newPassword).label == weakPasswordLabel) {
                        newPasswordError = weakPasswordErrorText
                        isValid = false
                    }

                    if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && isValid) {
                        // Call viewModel to reset password
                        // For now, simulate success
                        onResetSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            SecondaryButton(
                text = stringResource(R.string.back),
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

enum class ResetStep {
    EMAIL_INPUT,
    PASSWORD_RESET
}