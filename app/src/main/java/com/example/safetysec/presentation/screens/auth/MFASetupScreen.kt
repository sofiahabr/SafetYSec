package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.navigation.AppRoutes


/**
 * MFA Setup Screen
 *
 * Shown during registration to set up mandatory phone MFA.
 * User enters phone number and verifies with code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MFASetupScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(MFASetupStep.PHONE_INPUT) }
    val context = LocalContext.current

    // Auto-transition to verification step when verificationId is set
    LaunchedEffect(authState.verificationId) {
        if (authState.verificationId != null && step == MFASetupStep.PHONE_INPUT) {
            step = MFASetupStep.CODE_VERIFICATION
        }
    }

    // Navigate to dashboard after MFA setup is successful
    // MFA setup successful when: verificationId was set, then cleared (means verification happened)
    // and mfaPhoneNumber is also cleared
    LaunchedEffect(authState.verificationId, authState.mfaPhoneNumber) {
        if (authState.verificationId == null &&
            authState.mfaPhoneNumber == null &&
            step == MFASetupStep.CODE_VERIFICATION &&
            authState.isAuthenticated) {
            // MFA setup completed successfully, navigate to dashboard
            navController.navigate(AppRoutes.DASHBOARD) {
                popUpTo(AppRoutes.MFA_SETUP) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Set Up Two-Factor Authentication",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            when (step) {
                MFASetupStep.PHONE_INPUT -> {
                    PhoneInputStep(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it },
                        onContinue = {
                            // Call viewModel to send verification code
                            // Don't transition yet - wait for verificationId to be set via LaunchedEffect
                            verificationCode = ""  // Reset code field
                            viewModel.sendMFACode(phoneNumber, context as android.app.Activity)
                        },
                        isLoading = authState.isLoading,
                        error = authState.error
                    )
                }

                MFASetupStep.CODE_VERIFICATION -> {
                    CodeVerificationStep(
                        phoneNumber = phoneNumber,
                        verificationCode = verificationCode,
                        onCodeChange = { verificationCode = it },
                        onVerify = {
                            viewModel.verifyMFASetupCode(verificationCode)
                        },
                        onEdit = {
                            step = MFASetupStep.PHONE_INPUT
                            verificationCode = ""
                        },
                        isLoading = authState.isLoading,
                        error = authState.error
                    )
                }
            }
        }
    }
}

/**
 * Step 1: Phone Number Input
 */
@Composable
private fun PhoneInputStep(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit,
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
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Secure Your Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "Enter your phone number to receive security codes when you log in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Phone Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone setup",
                    modifier = Modifier.size(64.dp),
                    tint = PrimaryPurple
                )
            }

            // Phone Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text("Phone Number") },
                placeholder = { Text("+4754665290") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
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
                        text = "Why Two-Factor Authentication?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )

                    Text(
                        text = "2FA adds an extra layer of security to your account. Even if someone has your password, they can't access your account without this code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Benefits List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Protects sensitive health information",
                    "Prevents unauthorized access",
                    "Required on every login"
                ).forEach { benefit ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Continue Button
        PrimaryButton(
            text = if (isLoading) "Sending Code..." else "Send Code",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.isNotEmpty() && !isLoading
        )
    }
}

/**
 * Step 2: Code Verification
 */
@Composable
private fun CodeVerificationStep(
    phoneNumber: String,
    verificationCode: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onEdit: () -> Unit,
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
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Verify Your Number",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "Enter the 6-digit code we sent to your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Phone Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F4FF)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Code sent to",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                }
            }

            // Code Input
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        onCodeChange(newValue)
                    }
                },
                label = { Text("Verification Code") },
                placeholder = { Text("000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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
                        text = "Didn't receive the code?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )

                    Text(
                        text = "Check your spam folder or request a new code. This code expires in 10 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Verify Button
        PrimaryButton(
            text = if (isLoading) "Verifying..." else "Complete Setup",
            onClick = onVerify,
            modifier = Modifier.fillMaxWidth(),
            enabled = verificationCode.length == 6 && !isLoading
        )
    }
}

enum class MFASetupStep {
    PHONE_INPUT,
    CODE_VERIFICATION
}