package com.example.safetysec.presentation.screens.timewindows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.domain.model.DayOfWeek
import com.example.safetysec.domain.model.TimeWindow
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.TimeWindowViewModel

@Composable
fun TimeWindowsScreen(
    navController: NavController,
    viewModel: TimeWindowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingWindow by remember { mutableStateOf<TimeWindow?>(null) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.monitoring_time_windows),
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryPurple
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_time_window),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.timeWindows.isEmpty() -> {
                    FullScreenLoading(message = stringResource(R.string.loading_time_windows))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        ErrorAlert(
                            message = uiState.error ?: stringResource(R.string.error_unknown),
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                else -> {
                    TimeWindowsContent(
                        timeWindows = uiState.timeWindows,
                        onEdit = { editingWindow = it },
                        onDelete = { viewModel.deleteTimeWindow(it) },
                        onToggleActive = { window ->
                            viewModel.updateTimeWindow(
                                window.copy(isActive = !window.isActive)
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading && uiState.timeWindows.isNotEmpty()) {
                LoadingDialog(message = stringResource(R.string.processing))
            }
        }
    }

    // Create/Edit Dialog
    if (showCreateDialog || editingWindow != null) {
        TimeWindowDialog(
            timeWindow = editingWindow,
            onDismiss = {
                showCreateDialog = false
                editingWindow = null
            },
            onSave = { days, startTime, endTime ->
                if (editingWindow != null) {
                    // Update existing window
                    viewModel.updateTimeWindow(
                        editingWindow!!.copy(
                            daysOfWeek = days,
                            startTime = startTime,
                            endTime = endTime
                        )
                    )
                } else {
                    // Create new window
                    viewModel.createTimeWindow(
                        daysOfWeek = days,
                        startTime = startTime,
                        endTime = endTime
                    )
                }
                showCreateDialog = false
                editingWindow = null
            }
        )
    }
}

@Composable
private fun TimeWindowsContent(
    timeWindows: List<TimeWindow>,
    onEdit: (TimeWindow) -> Unit,
    onDelete: (String) -> Unit,
    onToggleActive: (TimeWindow) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Info card
        InfoAlert(
            title = stringResource(R.string.about_time_windows),
            message = stringResource(R.string.about_time_windows_desc)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (timeWindows.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.no_time_windows),
                message = stringResource(R.string.no_time_windows_desc),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(timeWindows) { window ->
                    TimeWindowCard(
                        timeWindow = window,
                        onEdit = { onEdit(window) },
                        onDelete = { onDelete(window.id) },
                        onToggleActive = { onToggleActive(window) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeWindowCard(
    timeWindow: TimeWindow,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = timeWindow.getTimeRangeString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (timeWindow.monitorName.isNotBlank()) {
                            Text(
                                text = "${stringResource(R.string.monitor)}: ${timeWindow.monitorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Active toggle
                Switch(
                    checked = timeWindow.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = timeWindow.getDaysString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status indicator
            if (timeWindow.isCurrentlyActive()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.currently_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Edit and Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.edit),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )

                DangerButton(
                    text = stringResource(R.string.delete),
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_time_window)) },
            text = { Text(stringResource(R.string.delete_time_window_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TimeWindowDialog(
    timeWindow: TimeWindow? = null,
    onDismiss: () -> Unit,
    onSave: (List<DayOfWeek>, String, String) -> Unit
) {
    var selectedDays by remember {
        mutableStateOf(timeWindow?.daysOfWeek?.toSet() ?: setOf())
    }
    var startTime by remember {
        mutableStateOf(timeWindow?.startTime ?: "09:00")
    }
    var endTime by remember {
        mutableStateOf(timeWindow?.endTime ?: "17:00")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (timeWindow == null)
                    stringResource(R.string.create_time_window)
                else
                    stringResource(R.string.edit_time_window)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Days selection
                Text(
                    text = stringResource(R.string.select_days),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                DayOfWeek.values().forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = day.toDisplayString())
                        Checkbox(
                            checked = selectedDays.contains(day),
                            onCheckedChange = {
                                selectedDays = if (it) {
                                    selectedDays + day
                                } else {
                                    selectedDays - day
                                }
                            }
                        )
                    }
                }

                // Time selection
                Text(
                    text = stringResource(R.string.time_range),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                CustomTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = stringResource(R.string.start_time_hhmm),
                    placeholder = "09:00"
                )

                CustomTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = stringResource(R.string.end_time_hhmm),
                    placeholder = "17:00"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedDays.toList(), startTime, endTime)
                },
                enabled = selectedDays.isNotEmpty() && startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                Text(
                    if (timeWindow == null)
                        stringResource(R.string.create)
                    else
                        stringResource(R.string.save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}