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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import androidx.compose.ui.unit.sp

/**
 * Forgot Password Screen
 *
 * Allows users to recover their password by entering their email address.
 * Firebase sends a password reset link to the provided email.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Password Recovery",
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
            if (isEmailSent) {
                EmailSentSuccessContent(
                    email = email,
                    onBackClick = { navController.navigateUp() },
                    onResendClick = {
                        viewModel.sendPasswordResetEmail(email)
                    },
                    isLoading = authState.isLoading
                )
            } else {
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
        }
    }
}

/**
 * Email Input Content
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
                    text = "Recover Your Password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "Enter your email address and we'll send you a link to reset your password.",
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
                    contentDescription = "Email",
                    modifier = Modifier.size(64.dp),
                    tint = PrimaryPurple
                )
            }

            // Email Input Field
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                placeholder = { Text("your.email@example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email"
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
                        text = "What happens next?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )

                    Text(
                        text = "1. We'll send a reset link to your email\n2. Click the link to verify your identity\n3. Create a new password\n4. You're all set!",
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
                text = if (isLoading) "Sending..." else "Send Reset Link",
                onClick = onSendClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotEmpty()
            )

            Text(
                text = "The reset link will expire in 24 hours",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Email Sent Success Content
 */
@Composable
private fun EmailSentSuccessContent(
    email: String,
    onBackClick: () -> Unit,
    onResendClick: () -> Unit,
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
                        contentDescription = "Email sent",
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
                    text = "Check Your Email",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "We've sent a password reset link to:",
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
                        text = "What to do next:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    listOf(
                        "Check your email inbox (and spam folder)",
                        "Click the password reset link",
                        "Follow the instructions to set a new password",
                        "Log in with your new password"
                    ).forEach { instruction ->
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
                                text = instruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Timer Info
            Text(
                text = "Link expires in 24 hours",
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
                text = if (isLoading) "Resending..." else "Didn't receive email? Resend",
                onClick = onResendClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            SecondaryButton(
                text = "Back to Login",
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}