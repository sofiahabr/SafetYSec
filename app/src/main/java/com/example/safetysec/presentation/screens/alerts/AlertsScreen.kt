package com.example.safetysec.presentation.screens.alerts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.*
import com.example.safetysec.presentation.viewmodel.AlertViewModel
import java.time.format.DateTimeFormatter

/**
 * Alerts Screen
 *
 * Shows comprehensive alert history with filtering and statistics
 */
@Composable
fun AlertsScreen(
    navController: NavController,
    viewModel: AlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "Alerts",
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.alerts.isEmpty() -> {
                    FullScreenLoading(message = "Loading alerts...")
                }

                uiState.error != null && uiState.alerts.isEmpty() -> {
                    ErrorAlert(message = uiState.error ?: "Unknown error")
                }

                uiState.alerts.isEmpty() -> {
                    NoAlertsState()
                }

                else -> {
                    AlertsContent(
                        alerts = uiState.getFilteredAlerts(),
                        statistics = uiState.getAlertStatistics(),
                        selectedFilter = uiState.selectedFilter,
                        onFilterChange = { viewModel.filterAlertsByType(it) },
                        onAlertClick = { alert ->
                            // Navigate to alert detail screen with video playback
                            navController.navigate("alert_detail/${alert.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertsContent(
    alerts: List<AlertEvent>,
    statistics: Map<String, Int>,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit,
    onAlertClick: (AlertEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Section
        item {
            AlertStatisticsCard(statistics = statistics)
        }

        // Filter Chips
        item {
            AlertFilterChips(
                selectedFilter = selectedFilter,
                statistics = statistics,
                onFilterChange = onFilterChange
            )
        }

        // Alerts Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedFilter == null) "All Alerts" else "Filtered Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${alerts.size} alert${if (alerts.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // Alert List
        items(alerts) { alert ->
            AlertHistoryCard(
                alert = alert,
                onClick = { onAlertClick(alert) }
            )
        }
    }
}

@Composable
fun AlertStatisticsCard(statistics: Map<String, Int>) {
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
                text = "Alert Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (statistics.isEmpty()) {
                Text(
                    text = "No statistics available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                statistics.forEach { (type, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getAlertTypeIcon(type),
                                contentDescription = null,
                                tint = getAlertTypeColor(type),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = AlertType.valueOf(type).toDisplayString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = getAlertTypeColor(type).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = getAlertTypeColor(type),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertFilterChips(
    selectedFilter: String?,
    statistics: Map<String, Int>,
    onFilterChange: (String?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Filter by type",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All filter chip
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") },
                leadingIcon = if (selectedFilter == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )

            // Type-specific chips
            AlertType.values().forEach { type ->
                val count = statistics[type.name] ?: 0
                if (count > 0) {
                    FilterChip(
                        selected = selectedFilter == type.name,
                        onClick = { onFilterChange(type.name) },
                        label = { Text("${type.toDisplayString()} ($count)") },
                        leadingIcon = if (selectedFilter == type.name) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun AlertHistoryCard(
    alert: AlertEvent,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isCancelled) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getAlertTypeIcon(alert.type.name),
                        contentDescription = null,
                        tint = if (alert.isCancelled) Color.Gray else getAlertTypeColor(alert.type.name),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = alert.type.toDisplayString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (alert.isCancelled) Color.Gray else Color.Black
                        )
                        if (alert.isCancelled) {
                            Text(
                                text = "Cancelled",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (alert.videoUrl != null) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Has video",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider()

            // User and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = alert.protectedUserName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatAlertTime(alert.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Location
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = alert.getFormattedLocation(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Details
            if (!alert.details.isNullOrBlank()) {
                Text(
                    text = alert.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}

// Helper functions
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

private fun formatAlertTime(timestamp: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val duration = java.time.Duration.between(timestamp, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        else -> timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}