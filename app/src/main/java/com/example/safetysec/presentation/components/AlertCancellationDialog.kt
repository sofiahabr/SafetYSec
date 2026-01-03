package com.example.safetysec.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.safetysec.domain.model.AlertType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Alert Cancellation Dialog
 *
 * Shows a countdown dialog with PIN entry for cancelling alerts within 10 seconds
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCancellationDialog(
    alertId: String,
    alertType: AlertType,
    userCancellationCode: String = "0000",
    onCancel: (code: String) -> Unit,
    onDismiss: () -> Unit
) {
    var secondsRemaining by remember { mutableStateOf(10) }
    var cancellationCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0 && isActive) {
            delay(1000)
            secondsRemaining--
        }
        if (secondsRemaining == 0) {
            onDismiss() // Auto-dismiss when time runs out
        }
    }

    // Animation for the countdown
    val scale by animateFloatAsState(
        targetValue = if (secondsRemaining <= 3) 1.2f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "countdown_scale"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Dialog(
        onDismissRequest = { }, // Prevent dismissal by tapping outside
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Icon with pulse animation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFFEF5350).copy(alpha = pulseAlpha),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Alert Type
                Text(
                    text = alertType.toDisplayString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Alert Message
                Text(
                    text = "An alert has been detected and will be sent to your monitors unless cancelled.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                Divider()

                // Countdown Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Time Remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = if (secondsRemaining <= 3) Color(0xFFEF5350) else Color(0xFF2196F3),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$secondsRemaining",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = MaterialTheme.typography.displayLarge.fontSize * scale
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Divider()

                // PIN Entry Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter Your Cancellation PIN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = cancellationCode,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                cancellationCode = it
                                errorMessage = "" // Clear error when typing
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("****") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (cancellationCode.length == 4) {
                                    handleCancellation(
                                        enteredCode = cancellationCode,
                                        correctCode = userCancellationCode,
                                        onSuccess = { onCancel(cancellationCode) },
                                        onError = { errorMessage = it },
                                        setProcessing = { isProcessing = it }
                                    )
                                }
                            }
                        ),
                        isError = errorMessage.isNotEmpty(),
                        supportingText = {
                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !isProcessing
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Let Alert Send Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text("Let Alert Send")
                    }

                    // Cancel Alert Button
                    Button(
                        onClick = {
                            handleCancellation(
                                enteredCode = cancellationCode,
                                correctCode = userCancellationCode,
                                onSuccess = { onCancel(cancellationCode) },
                                onError = { errorMessage = it },
                                setProcessing = { isProcessing = it }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = cancellationCode.length == 4 && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel Alert")
                        }
                    }
                }

                // Helper text
                Text(
                    text = "Enter your 4-digit PIN to cancel this alert",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Handle cancellation logic with PIN verification
 */
private fun handleCancellation(
    enteredCode: String,
    correctCode: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    setProcessing: (Boolean) -> Unit
) {
    if (enteredCode.length != 4) {
        onError("Please enter a 4-digit PIN")
        return
    }

    setProcessing(true)

    // Verify PIN
    if (enteredCode == correctCode) {
        onSuccess()
    } else {
        setProcessing(false)
        onError("Incorrect PIN. Please try again.")
    }
}

/**
 * Extension function to display alert types
 */
fun AlertType.toDisplayString(): String = when (this) {
    AlertType.FALL_DETECTED -> "⚠Fall Detected"
    AlertType.SPEED_ALERT -> "Speed Alert"
    AlertType.GEOFENCING -> "Geofence Breach"
    AlertType.ACCIDENT_DETECTED -> "Accident Detected"
    AlertType.PROLONGED_INACTIVITY -> "Inactivity Alert"
    AlertType.PANIC_BUTTON -> "Panic Button"
}