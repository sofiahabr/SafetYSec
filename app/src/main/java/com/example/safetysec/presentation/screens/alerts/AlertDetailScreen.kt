package com.example.safetysec.presentation.screens.alerts

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.presentation.components.CustomTopAppBar
import com.example.safetysec.presentation.components.ErrorAlert
import com.example.safetysec.presentation.components.FullScreenLoading
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AlertViewModel
import java.time.format.DateTimeFormatter

/**
 * Alert Detail Screen
 *
 * Shows comprehensive alert information including video recording
 */
@Composable
fun AlertDetailScreen(
    alertId: String,
    navController: NavController,
    viewModel: AlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(alertId) {
        viewModel.getAlertById(alertId)
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Alert Details",
                onNavigationClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    FullScreenLoading(message = "Loading alert details...")
                }

                uiState.error != null -> {
                    ErrorAlert(message = uiState.error ?: "Unknown error")
                }

                uiState.selectedAlert != null -> {
                    AlertDetailContent(
                        alert = uiState.selectedAlert!!
                    )
                }

                else -> {
                    ErrorAlert(message = "Alert not found")
                }
            }
        }
    }
}

@Composable
fun AlertDetailContent(alert: AlertEvent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Alert Type Header
        AlertTypeHeader(alert)

        // Prominent Cancellation Banner (if cancelled)
        if (alert.isCancelled) {
            CancellationBanner(alert)
        }

        // Video Player (if available)
        if (alert.videoUrl != null) {
            VideoPlayerCard(videoUrl = alert.videoUrl)
        }

        // Alert Information
        AlertInformationCard(alert)

        // Location Information
        LocationInformationCard(alert)

        // Timeline
        AlertTimelineCard(alert)
    }
}

@Composable
fun AlertTypeHeader(alert: AlertEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isCancelled) Color(0xFFE0E0E0) else getAlertTypeColor(alert.type.name)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getAlertTypeIcon(alert.type.name),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text(
                    text = alert.type.toDisplayString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (alert.isCancelled) {
                    Text(
                        text = "Cancelled",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun CancellationBanner(alert: AlertEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4) // Light yellow background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF388E3C), // Green color
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "✓ Alert Cancelled by User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )
                if (alert.cancelledAt != null) {
                    Text(
                        text = "Cancelled at ${alert.cancelledAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555)
                    )
                }
                Text(
                    text = "The protected user successfully cancelled this alert. No emergency assistance needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerCard(videoUrl: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                prepare()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    controllerAutoShow = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AlertInformationCard(alert: AlertEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Alert Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            InfoRow(
                label = "Protected User",
                value = alert.protectedUserName,
                icon = Icons.Default.Person
            )

            InfoRow(
                label = "Date & Time",
                value = alert.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss")),
                icon = Icons.Default.Schedule
            )

            if (!alert.details.isNullOrBlank()) {
                InfoRow(
                    label = "Details",
                    value = alert.details,
                    icon = Icons.Default.Info
                )
            }

            InfoRow(
                label = "Status",
                value = if (alert.isCancelled) "Cancelled" else "Active",
                icon = if (alert.isCancelled) Icons.Default.Cancel else Icons.Default.CheckCircle,
                valueColor = if (alert.isCancelled) Color.Gray else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun LocationInformationCard(alert: AlertEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            if (alert.latitude != 0.0 && alert.longitude != 0.0) {
                InfoRow(
                    label = "Coordinates",
                    value = alert.getFormattedLocation(),
                    icon = Icons.Default.LocationOn
                )

                // Map preview could go here
                Button(
                    onClick = { /* Open in maps */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple
                    )
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View on Map")
                }
            } else {
                Text(
                    text = "Location not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AlertTimelineCard(alert: AlertEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            TimelineItem(
                time = alert.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                event = "Alert triggered",
                icon = Icons.Default.Warning,
                color = Color(0xFFEF5350)
            )

            if (alert.isCancelled && alert.cancelledAt != null) {
                TimelineItem(
                    time = alert.cancelledAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    event = "✓ Alert cancelled by user",
                    description = "User successfully cancelled the alert. No emergency response needed.",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50) // Green for success
                )
            }

            if (alert.videoUrl != null && !alert.isCancelled) {
                TimelineItem(
                    time = alert.timestamp.plusSeconds(30).format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    event = "Video recording completed",
                    icon = Icons.Default.Videocam,
                    color = PrimaryPurple
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
fun TimelineItem(
    time: String,
    event: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    description: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// Helper functions (reuse from AlertsScreen)
private fun getAlertTypeIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "FALL_DETECTED" -> Icons.Default.Person
        "SPEED_ALERT" -> Icons.Default.Speed
        "GEOFENCE_BREACH" -> Icons.Default.LocationOn
        "ACCIDENT_DETECTED" -> Icons.Default.Warning
        "PROLONGED_INACTIVITY" -> Icons.Default.Timer
        "PANIC_BUTTON" -> Icons.Default.Notifications
        else -> Icons.Default.NotificationsActive
    }
}

private fun getAlertTypeColor(type: String): Color {
    return when (type) {
        "FALL_DETECTED" -> Color(0xFFE53935)
        "SPEED_ALERT" -> Color(0xFFFB8C00)
        "GEOFENCE_BREACH" -> Color(0xFF8E24AA)
        "ACCIDENT_DETECTED" -> Color(0xFFD32F2F)
        "PROLONGED_INACTIVITY" -> Color(0xFF1976D2)
        "PANIC_BUTTON" -> Color(0xFFC62828)
        else -> Color(0xFF424242)
    }
}