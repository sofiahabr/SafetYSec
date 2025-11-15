package com.example.safetysec.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Alert Card Components
 *
 * Colorful alert cards for different message types
 */

enum class AlertType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Get icon for alert type
 */
private fun getAlertIcon(type: AlertType): ImageVector {
    return when (type) {
        AlertType.SUCCESS -> Icons.Default.CheckCircle
        AlertType.ERROR -> Icons.Filled.Error
        AlertType.WARNING -> Icons.Default.Warning
        AlertType.INFO -> Icons.Default.Info
    }
}

/**
 * Get colors for alert type
 */
@Composable
private fun getAlertColors(type: AlertType): Pair<Color, Color> {
    return when (type) {
        AlertType.SUCCESS -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
        AlertType.ERROR -> Color(0xFFF44336) to Color(0xFFFFEBEE)
        AlertType.WARNING -> Color(0xFFFF9800) to Color(0xFFFFF3E0)
        AlertType.INFO -> Color(0xFF2196F3) to Color(0xFFE3F2FD)
    }
}

/**
 * Alert Card - Main reusable alert component
 */
@Composable
fun AlertCard(
    message: String,
    type: AlertType,
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val (iconColor, backgroundColor) = getAlertColors(type)
    val icon = getAlertIcon(type)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.87f)
                )
            }

            // Dismiss button
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Success Alert Card
 */
@Composable
fun SuccessAlert(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = "Success",
    onDismiss: (() -> Unit)? = null
) {
    AlertCard(
        message = message,
        type = AlertType.SUCCESS,
        modifier = modifier,
        title = title,
        onDismiss = onDismiss
    )
}

/**
 * Error Alert Card
 */
@Composable
fun ErrorAlert(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = "Error",
    onDismiss: (() -> Unit)? = null
) {
    AlertCard(
        message = message,
        type = AlertType.ERROR,
        modifier = modifier,
        title = title,
        onDismiss = onDismiss
    )
}

/**
 * Warning Alert Card
 */
@Composable
fun WarningAlert(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = "Warning",
    onDismiss: (() -> Unit)? = null
) {
    AlertCard(
        message = message,
        type = AlertType.WARNING,
        modifier = modifier,
        title = title,
        onDismiss = onDismiss
    )
}

/**
 * Info Alert Card
 */
@Composable
fun InfoAlert(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = "Info",
    onDismiss: (() -> Unit)? = null
) {
    AlertCard(
        message = message,
        type = AlertType.INFO,
        modifier = modifier,
        title = title,
        onDismiss = onDismiss
    )
}

/**
 * Inline Alert - Smaller alert without card elevation
 */
@Composable
fun InlineAlert(
    message: String,
    type: AlertType,
    modifier: Modifier = Modifier
) {
    val (iconColor, backgroundColor) = getAlertColors(type)
    val icon = getAlertIcon(type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.name,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = iconColor
        )
    }
}