package com.example.safetysec.presentation.screens.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.safetysec.presentation.components.CustomTopAppBar
import com.example.safetysec.presentation.components.NoAlertsState
import com.example.safetysec.presentation.components.LoadingDialog
import com.example.safetysec.presentation.viewmodel.AlertViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

/**
 * Enhanced Alerts Screen with Filtering
 *
 * Features:
 * - Filter by alert type
 * - Filter by date range
 * - Search by user name
 * - Export alerts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAlertsScreen(
    navController: NavController,
    viewModel: AlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedAlertType by remember { mutableStateOf<AlertType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCancelledAlerts by remember { mutableStateOf(true) }

    // Filter alerts based on selections
    val filteredAlerts = remember(
        uiState.alerts,
        selectedAlertType,
        searchQuery,
        startDate,
        endDate,
        showCancelledAlerts
    ) {
        uiState.alerts.filter { alert ->
            val typeMatch = selectedAlertType == null || alert.type == selectedAlertType
            val searchMatch = searchQuery.isEmpty() ||
                    alert.protectedUserName.contains(searchQuery, ignoreCase = true) ||
                    alert.type.toDisplayString().contains(searchQuery, ignoreCase = true)
            val dateMatch = (startDate == null || alert.timestamp.toLocalDate() >= startDate) &&
                    (endDate == null || alert.timestamp.toLocalDate() <= endDate)
            val cancelledMatch = showCancelledAlerts || !alert.isCancelled

            typeMatch && searchMatch && dateMatch && cancelledMatch
        }
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Alerts",
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                            containerColor = if (selectedAlertType != null || startDate != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter alerts"
                            )
                        }
                    }

                    // Export button
                    IconButton(onClick = {
                        viewModel.exportAlerts(filteredAlerts)
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export alerts"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Active Filters Display
            if (selectedAlertType != null || startDate != null || !showCancelledAlerts) {
                ActiveFiltersRow(
                    selectedType = selectedAlertType,
                    startDate = startDate,
                    endDate = endDate,
                    showCancelled = showCancelledAlerts,
                    onClearType = { selectedAlertType = null },
                    onClearDates = {
                        startDate = null
                        endDate = null
                    },
                    onToggleCancelled = { showCancelledAlerts = !showCancelledAlerts }
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingDialog()
                    }
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.loadAlerts() }
                    )
                }

                filteredAlerts.isEmpty() -> {
                    if (searchQuery.isNotEmpty() || selectedAlertType != null) {
                        NoSearchResultsState(
                            onClearFilters = {
                                searchQuery = ""
                                selectedAlertType = null
                                startDate = null
                                endDate = null
                            }
                        )
                    } else {
                        NoAlertsState()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stats header
                        item {
                            AlertStatsCard(
                                totalAlerts = filteredAlerts.size,
                                cancelledAlerts = filteredAlerts.count { it.isCancelled },
                                withVideo = filteredAlerts.count { it.videoUrl != null }
                            )
                        }

                        // Alert list
                        items(filteredAlerts, key = { it.id }) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = {
                                    navController.navigate("alert_detail/${alert.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertFilterDialog(
            currentType = selectedAlertType,
            startDate = startDate,
            endDate = endDate,
            showCancelled = showCancelledAlerts,
            onDismiss = { showFilterDialog = false },
            onApply = { type, start, end, cancelled ->
                selectedAlertType = type
                startDate = start
                endDate = end
                showCancelledAlerts = cancelled
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun AlertCard(
    alert: AlertEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isCancelled)
                Color(0xFFF5F5F5)
            else
                Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        imageVector = when (alert.type) {
                            com.example.safetysec.domain.model.AlertType.FALL_DETECTED -> Icons.Default.Warning
                            com.example.safetysec.domain.model.AlertType.PANIC_BUTTON -> Icons.Default.Warning
                            com.example.safetysec.domain.model.AlertType.SPEED_ALERT -> Icons.Default.Speed
                            com.example.safetysec.domain.model.AlertType.GEOFENCE_BREACH -> Icons.Default.LocationOn
                            com.example.safetysec.domain.model.AlertType.ACCIDENT_DETECTED -> Icons.Default.Warning
                            com.example.safetysec.domain.model.AlertType.PROLONGED_INACTIVITY -> Icons.Default.AccessTime
                        },
                        contentDescription = alert.type.name,
                        tint = if (alert.isCancelled) Color.Gray else Color(0xFFEF5350),
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        Text(
                            text = alert.type.toDisplayString(),
                            style = MaterialTheme.typography.titleMedium,
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
                        tint = Color(0xFF7E57C2),
                        modifier = Modifier.size(24.dp)
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
            if (alert.latitude != 0.0 && alert.longitude != 0.0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = alert.getFormattedLocation(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Details
            if (alert.details != null) {
                Text(
                    text = alert.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// Helper function for time formatting
private fun formatAlertTime(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    return when {
        timestamp.toLocalDate() == now.toLocalDate() -> {
            "Today at ${timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        timestamp.toLocalDate() == now.toLocalDate().minusDays(1) -> {
            "Yesterday at ${timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        else -> {
            timestamp.format(formatter)
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search by user name or alert type...") },
        leadingIcon = {
            Icon(Icons.Default.Search, "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear search")
                }
            }
        },
        singleLine = true
    )
}

@Composable
fun ActiveFiltersRow(
    selectedType: AlertType?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    showCancelled: Boolean,
    onClearType: () -> Unit,
    onClearDates: () -> Unit,
    onToggleCancelled: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedType != null) {
            FilterChip(
                selected = true,
                onClick = onClearType,
                label = { Text(selectedType.toDisplayString()) },
                trailingIcon = {
                    Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp))
                }
            )
        }

        if (startDate != null || endDate != null) {
            FilterChip(
                selected = true,
                onClick = onClearDates,
                label = {
                    Text(
                        "${startDate?.toString() ?: "All"} - ${endDate?.toString() ?: "Now"}"
                    )
                },
                trailingIcon = {
                    Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp))
                }
            )
        }

        if (!showCancelled) {
            FilterChip(
                selected = true,
                onClick = onToggleCancelled,
                label = { Text("Hide Cancelled") },
                trailingIcon = {
                    Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp))
                }
            )
        }
    }
}

@Composable
fun AlertStatsCard(
    totalAlerts: Int,
    cancelledAlerts: Int,
    withVideo: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Total", totalAlerts.toString(), Icons.Default.Notifications)
            StatItem("Cancelled", cancelledAlerts.toString(), Icons.Default.Cancel)
            StatItem("With Video", withVideo.toString(), Icons.Default.Videocam)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun AlertFilterDialog(
    currentType: AlertType?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    showCancelled: Boolean,
    onDismiss: () -> Unit,
    onApply: (AlertType?, LocalDate?, LocalDate?, Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedStartDate by remember { mutableStateOf(startDate) }
    var selectedEndDate by remember { mutableStateOf(endDate) }
    var includeCancelled by remember { mutableStateOf(showCancelled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Alerts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Alert Type Filter
                Text("Alert Type", fontWeight = FontWeight.Bold)
                AlertType.values().forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = if (selectedType == type) null else type }
                        )
                        Text(type.toDisplayString())
                    }
                }

                // Show Cancelled Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include Cancelled Alerts")
                    Switch(
                        checked = includeCancelled,
                        onCheckedChange = { includeCancelled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(selectedType, selectedStartDate, selectedEndDate, includeCancelled)
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NoSearchResultsState(onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No alerts match your filters",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClearFilters) {
            Text("Clear Filters")
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

fun AlertType.toDisplayString(): String = when (this) {
    AlertType.FALL_DETECTED -> "Fall Detected"
    AlertType.SPEED_ALERT -> "Speed Alert"
    AlertType.GEOFENCE_BREACH -> "Geofence Breach"
    AlertType.ACCIDENT_DETECTED -> "Accident Detected"
    AlertType.PROLONGED_INACTIVITY -> "Prolonged Inactivity"
    AlertType.PANIC_BUTTON -> "Panic Button"
}