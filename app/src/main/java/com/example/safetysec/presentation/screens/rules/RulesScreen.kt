package com.example.safetysec.presentation.screens.rules

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.domain.model.Rule
import com.example.safetysec.domain.model.RuleStatus
import com.example.safetysec.domain.model.RuleType
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.RulesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RulesScreen(
    navController: NavController,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "Safety Rules"
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            if (uiState.isMonitor) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = PrimaryPurple
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Rule",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.rules.isEmpty() -> {
                    FullScreenLoading(message = "Loading rules...")
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        ErrorAlert(
                            message = uiState.error ?: "An error occurred",
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                else -> {
                    RulesContent(
                        rules = uiState.filteredRules,
                        isMonitor = uiState.isMonitor,
                        selectedFilter = uiState.selectedFilter,
                        pendingCount = uiState.pendingCount,
                        navController = navController, // ADDED
                        onFilterChanged = { viewModel.filterByStatus(it) },
                        onAuthorize = { viewModel.authorizeRule(it) },
                        onReject = { viewModel.rejectRule(it) },
                        onRevoke = { viewModel.revokeRule(it) },
                        onDelete = { viewModel.deleteRule(it) }
                    )
                }
            }

            // Loading overlay
            if (uiState.isLoading && uiState.rules.isNotEmpty()) {
                LoadingDialog(message = "Processing...")
            }
        }
    }

    // Create Rule Dialog
    if (showCreateDialog) {
        // Navigate to create rule screen
        navController.navigate("create_rule")
        showCreateDialog = false
    }
}

@Composable
private fun RulesContent(
    rules: List<Rule>,
    isMonitor: Boolean,
    selectedFilter: RuleStatus?,
    pendingCount: Int,
    navController: NavController, // ADDED
    onFilterChanged: (RuleStatus?) -> Unit,
    onAuthorize: (String) -> Unit,
    onReject: (String) -> Unit,
    onRevoke: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter chips
        StatusFilterChips(
            selectedFilter = selectedFilter,
            pendingCount = pendingCount,
            onFilterChanged = onFilterChanged,
            isMonitor = isMonitor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Rules list
        if (rules.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Rule,
                title = "No Rules",
                message = if (isMonitor) {
                    "You haven't created any rules yet. Tap the + button to create your first rule."
                } else {
                    "You don't have any rules from monitors yet."
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rules) { rule ->
                    RuleCard(
                        rule = rule,
                        isMonitor = isMonitor,
                        navController = navController, // ADDED
                        onAuthorize = { onAuthorize(rule.id) },
                        onReject = { onReject(rule.id) },
                        onRevoke = { onRevoke(rule.id) },
                        onDelete = { onDelete(rule.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusFilterChips(
    selectedFilter: RuleStatus?,
    pendingCount: Int,
    onFilterChanged: (RuleStatus?) -> Unit,
    isMonitor: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Authorized filter
        FilterChip(
            selected = selectedFilter == RuleStatus.AUTHORIZED,
            onClick = { onFilterChanged(RuleStatus.AUTHORIZED) },
            label = { Text("Authorized") },
            leadingIcon = if (selectedFilter == RuleStatus.AUTHORIZED) {
                {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4CAF50),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )

        // Pending filter - with badge
        BadgedBox(
            badge = {
                if (pendingCount > 0) {
                    Badge(
                        containerColor = Color(0xFFFF5722),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = pendingCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        ) {
            FilterChip(
                selected = selectedFilter == RuleStatus.PENDING,
                onClick = { onFilterChanged(RuleStatus.PENDING) },
                label = { Text("Pending") },
                leadingIcon = if (selectedFilter == RuleStatus.PENDING) {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF9800),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        // Cancelled filter
        FilterChip(
            selected = selectedFilter == RuleStatus.CANCELLED,
            onClick = { onFilterChanged(RuleStatus.CANCELLED) },
            label = { Text("Cancelled") },
            leadingIcon = if (selectedFilter == RuleStatus.CANCELLED) {
                {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF9E9E9E),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    isMonitor: Boolean,
    navController: NavController, // ADDED
    onAuthorize: () -> Unit,
    onReject: () -> Unit,
    onRevoke: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAuthorizeDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showRevokeDialog by remember { mutableStateOf(false) }

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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getRuleIcon(rule.type),
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = rule.type.toDisplayString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMonitor) {
                                "For: ${rule.protectedName}"
                            } else {
                                "By: ${rule.monitorName}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Status Badge
                RuleStatusBadge(status = rule.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = rule.getDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            // Parameters display
            if (rule.type.requiresParameters()) {
                Spacer(modifier = Modifier.height(8.dp))
                RuleParametersDisplay(rule = rule)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Created date
            val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
            Text(
                text = "Created: ${dateFormat.format(rule.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Action buttons row for monitors
            if (isMonitor && rule.status != RuleStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ADDED: Edit button (only for authorized rules)
                    if (rule.status == RuleStatus.AUTHORIZED) {
                        IconButton(
                            onClick = {
                                navController.navigate("edit_rule/${rule.id}")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Rule",
                                tint = PrimaryPurple
                            )
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Rule",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Action buttons for protected users
            if (!isMonitor && rule.status == RuleStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Reject",
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Authorize",
                        onClick = { showAuthorizeDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!isMonitor && rule.status == RuleStatus.AUTHORIZED) {
                Spacer(modifier = Modifier.height(16.dp))
                DangerButton(
                    text = "Revoke Authorization",
                    onClick = { showRevokeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Confirmation dialogs
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Rule",
            message = "Are you sure you want to delete this rule? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            isDangerous = true
        )
    }

    if (showAuthorizeDialog) {
        ConfirmationDialog(
            title = "Authorize Rule",
            message = "Do you authorize this monitoring rule?",
            confirmText = "Authorize",
            onConfirm = {
                onAuthorize()
                showAuthorizeDialog = false
            },
            onDismiss = { showAuthorizeDialog = false }
        )
    }

    if (showRejectDialog) {
        ConfirmationDialog(
            title = "Reject Rule",
            message = "Are you sure you want to reject this rule?",
            confirmText = "Reject",
            onConfirm = {
                onReject()
                showRejectDialog = false
            },
            onDismiss = { showRejectDialog = false },
            isDangerous = true
        )
    }

    if (showRevokeDialog) {
        ConfirmationDialog(
            title = "Revoke Authorization",
            message = "Are you sure you want to revoke authorization for this rule?",
            confirmText = "Revoke",
            onConfirm = {
                onRevoke()
                showRevokeDialog = false
            },
            onDismiss = { showRevokeDialog = false },
            isDangerous = true
        )
    }
}

@Composable
private fun RuleParametersDisplay(rule: Rule) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = PrimaryPurple.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (rule.type) {
                RuleType.GEOFENCING -> {
                    Text(
                        text = "Geofence Areas:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    rule.parameters.geofenceAreas.forEach { area ->
                        Text(
                            text = "• ${area.name} (${area.radius.toInt()}m)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                RuleType.SPEED_CONTROL -> {
                    Text(
                        text = "Max Speed: ${rule.parameters.maxSpeed} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                RuleType.PROLONGED_INACTIVITY -> {
                    Text(
                        text = "Duration: ${rule.parameters.inactivityDuration} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun RuleStatusBadge(status: RuleStatus) {
    val (backgroundColor, textColor) = when (status) {
        RuleStatus.PENDING -> Color(0xFFFF9800).copy(alpha = 0.1f) to Color(0xFFFF9800)
        RuleStatus.AUTHORIZED -> Color(0xFF4CAF50).copy(alpha = 0.1f) to Color(0xFF4CAF50)
        RuleStatus.REJECTED -> Color(0xFFF44336).copy(alpha = 0.1f) to Color(0xFFF44336)
        RuleStatus.REVOKED -> Color(0xFF9E9E9E).copy(alpha = 0.1f) to Color(0xFF9E9E9E)
        RuleStatus.CANCELLED -> Color(0xFF9E9E9E).copy(alpha = 0.1f) to Color(0xFF9E9E9E)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.toDisplayString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun getRuleIcon(type: RuleType) = when (type) {
    RuleType.FALL_DETECTION -> Icons.Default.Person
    RuleType.ACCIDENT_DETECTION -> Icons.Default.Warning
    RuleType.GEOFENCING -> Icons.Default.LocationOn
    RuleType.SPEED_CONTROL -> Icons.Default.Speed
    RuleType.PROLONGED_INACTIVITY -> Icons.Default.EventBusy
    RuleType.PANIC_BUTTON -> Icons.Default.NotificationImportant
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDangerous: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDangerous)
                        MaterialTheme.colorScheme.error
                    else
                        PrimaryPurple
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}