package com.example.safetysec.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.safetysec.R

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
                        contentDescription = stringResource(R.string.dismiss),
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
    title: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val defaultTitle = title ?: stringResource(R.string.success)

    AlertCard(
        message = message,
        type = AlertType.SUCCESS,
        modifier = modifier,
        title = defaultTitle,
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
    title: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val defaultTitle = title ?: stringResource(R.string.error)

    AlertCard(
        message = message,
        type = AlertType.ERROR,
        modifier = modifier,
        title = defaultTitle,
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
    title: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val defaultTitle = title ?: stringResource(R.string.warning)

    AlertCard(
        message = message,
        type = AlertType.WARNING,
        modifier = modifier,
        title = defaultTitle,
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
    title: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val defaultTitle = title ?: stringResource(R.string.info)

    AlertCard(
        message = message,
        type = AlertType.INFO,
        modifier = modifier,
        title = defaultTitle,
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

@Composable
fun AlertEventCard(
    title: String,
    subtitle: String,
    details: String? = null,
    location: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector = Icons.Default.Videocam,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .border(
                width = 2.dp,
                color = Color(0xFFEF5350),  // Red border for alerts
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title and action button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (onActionClick != null) {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = stringResource(R.string.action),
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }

        // Subtitle (person name and time)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // Location
        if (location != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.location),
                    tint = Color.Gray,
                    modifier = Modifier.padding(0.dp)
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        // Additional details (speed, etc.)
        if (details != null) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProtectedInfoCard(
    name: String,
    email: String = "",
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .border(
                width = 2.dp,
                color = Color(0xFF4CAF50),
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (isActive) {
                Text(
                    text = stringResource(R.string.active).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (email.isNotEmpty()) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}