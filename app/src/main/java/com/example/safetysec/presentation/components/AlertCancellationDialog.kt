package com.example.safetysec.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Alert Cancellation Dialog
 *
 * Shows a 10-second countdown window for cancelling an alert before it's sent to monitors
 */
@Composable
fun AlertCancellationDialog(
    alert: AlertEvent,
    onCancel: () -> Unit,
    onTimeout: () -> Unit,
    initialSecondsRemaining: Int = 10
) {
    var secondsRemaining by remember { mutableStateOf(initialSecondsRemaining) }
    var isCancelling by remember { mutableStateOf(false) }

    // Countdown timer
    LaunchedEffect(key1 = alert.id) {
        while (secondsRemaining > 0 && isActive && !isCancelling) {
            delay(1000L)
            secondsRemaining--
        }
        if (secondsRemaining == 0 && !isCancelling) {
            onTimeout()
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
                // Warning Icon
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
                    text = alert.type.toDisplayString(),
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

                // Countdown Display
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = when {
                                secondsRemaining <= 3 -> Color(0xFFEF5350)
                                secondsRemaining <= 5 -> Color(0xFFFB8C00)
                                else -> Color(0xFF1976D2)
                            }.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = secondsRemaining.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                secondsRemaining <= 3 -> Color(0xFFEF5350)
                                secondsRemaining <= 5 -> Color(0xFFFB8C00)
                                else -> Color(0xFF1976D2)
                            },
                            fontSize = 56.sp,
                            modifier = Modifier.scale(scale)
                        )
                        Text(
                            text = "seconds",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel Button
                Button(
                    onClick = {
                        isCancelling = true
                        onCancel()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    ),
                    enabled = !isCancelling
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null
                            )
                            Text(
                                text = "Cancel Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Info text
                Text(
                    text = "False alarm? Cancel now to prevent notification.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Get alert type color
 */
@Composable
private fun getAlertTypeColor(type: AlertType): Color {
    return when (type) {
        AlertType.FALL_DETECTED -> Color(0xFFE53935)
        AlertType.SPEED_ALERT -> Color(0xFFFB8C00)
        AlertType.GEOFENCE_BREACH -> Color(0xFF8E24AA)
        AlertType.ACCIDENT_DETECTED -> Color(0xFFD32F2F)
        AlertType.PROLONGED_INACTIVITY -> Color(0xFF1976D2)
        AlertType.PANIC_BUTTON -> Color(0xFFC62828)
    }
}