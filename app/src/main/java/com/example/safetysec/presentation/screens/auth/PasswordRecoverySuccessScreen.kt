package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.navigation.AppRoutes
import com.example.safetysec.presentation.theme.PrimaryPurple

/**
 * Password Recovery Success Screen
 *
 * Shown after password reset is successful.
 * User can now log in with their new password.
 */
@Composable
fun PasswordRecoverySuccessScreen(
    navController: NavController
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
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.large,
                color = PrimaryPurple.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        modifier = Modifier.size(64.dp),
                        tint = PrimaryPurple
                    )
                }
            }

            // Success Message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Password Reset Successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "Your password has been successfully reset. You can now log in with your new password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Info Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Security Tip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Security Tip",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17)
                        )

                        Text(
                            text = "Make sure to use a strong password and keep it secure. Never share your password with anyone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Next Steps
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F4FF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Next Steps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        listOf(
                            "Go back to the login screen",
                            "Enter your email and new password",
                            "Tap 'Log In' to access your account"
                        ).forEach { step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryPurple
                                )
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Button
        PrimaryButton(
            text = "Return to Login",
            onClick = {
                navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(AppRoutes.FORGOT_PASSWORD) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}