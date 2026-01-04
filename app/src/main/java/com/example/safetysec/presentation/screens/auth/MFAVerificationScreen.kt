package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.components.SecondaryButton
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel

/**
 * MFA Verification Screen
 *
 * Shows after successful login when MFA is required.
 * User enters 6-digit code sent to their phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MFAVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
    phoneNumber: String = ""
) {
    val authState by viewModel.authState.collectAsState()
    var code by remember { mutableStateOf("") }
    var showResendButton by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableStateOf(60) }

    // Countdown timer for resend
    LaunchedEffect(showResendButton) {
        if (!showResendButton) {
            resendCountdown = 60
            repeat(60) {
                kotlinx.coroutines.delay(1000)
                resendCountdown--
                if (resendCountdown == 0) {
                    showResendButton = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.verify_identity),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
                        text = stringResource(R.string.enter_verification_code),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Text(
                        text = stringResource(R.string.code_sent_to_phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    if (phoneNumber.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.ending_in, phoneNumber.takeLast(4)),
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                        contentDescription = stringResource(R.string.phone_icon),
                        modifier = Modifier.size(64.dp),
                        tint = PrimaryPurple
                    )
                }

                // Code Input Field
                OutlinedTextField(
                    value = code,
                    onValueChange = { newValue ->
                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                            code = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.verification_code)) },
                    placeholder = { Text("000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !authState.isLoading,
                    singleLine = true,
                    isError = authState.error != null
                )

                // Error Message
                if (authState.error != null) {
                    Text(
                        text = authState.error!!,
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
                            text = stringResource(R.string.security_tip),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )

                        Text(
                            text = stringResource(R.string.never_share_code),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Resend Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showResendButton) {
                        SecondaryButton(
                            text = stringResource(R.string.resend_code),
                            onClick = {
                                code = ""
                                showResendButton = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.resend_code_in, resendCountdown),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Verify Button
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    text = if (authState.isLoading) stringResource(R.string.verifying) else stringResource(R.string.verify_code),
                    onClick = {
                        viewModel.verifyMFACode(code)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length == 6 && !authState.isLoading
                )

                Text(
                    text = stringResource(R.string.code_expires_in_10_min),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}