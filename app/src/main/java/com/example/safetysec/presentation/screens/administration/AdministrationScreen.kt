package com.example.safetysec.presentation.screens.administration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Administration Screen for Dual Users
 *
 * Allows Dual users to switch between:
 * - Alerts: View both their own alerts and alerts from protected users
 * - Time Windows: View and manage time windows
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministrationScreen(navController: NavController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    val user = authState.user

    if (user == null) {
        FullScreenLoading(message = "Loading administration...")
        return
    }

    // Track which administration view is active
    var currentView by remember { mutableStateOf(AdministrationView.ALERTS) }

    Scaffold(
        topBar = {
            MainTopAppBar(title = "Administration")
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selector for Alerts / Time Windows
            AdministrationTabSelector(
                currentView = currentView,
                onViewSelected = { currentView = it }
            )

            // Content based on selected view
            when (currentView) {
                AdministrationView.ALERTS -> {
                    AlertsView(navController = navController)
                }
                AdministrationView.TIME_WINDOWS -> {
                    TimeWindowsView(navController = navController)
                }
            }
        }
    }
}

/**
 * Tab selector for Administration views
 */
@Composable
private fun AdministrationTabSelector(
    currentView: AdministrationView,
    onViewSelected: (AdministrationView) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryPurple)
            .padding(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alerts Tab
            AdministrationTabButton(
                isSelected = currentView == AdministrationView.ALERTS,
                label = "Alerts",
                onClick = { onViewSelected(AdministrationView.ALERTS) },
                modifier = Modifier.weight(1f)
            )

            // Time Windows Tab
            AdministrationTabButton(
                isSelected = currentView == AdministrationView.TIME_WINDOWS,
                label = "Time Windows",
                onClick = { onViewSelected(AdministrationView.TIME_WINDOWS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual tab button for Administration
 */
@Composable
private fun AdministrationTabButton(
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = if (isSelected) PrimaryPurple else Color.White
        ),
        shape = RoundedCornerShape(0.dp),
        elevation = if (isSelected) ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp) else null
    ) {
        Text(
            label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Alerts view - shows both personal and monitored alerts
 */
@Composable
private fun AlertsView(navController: NavController) {
    var showPersonalAlerts by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toggle between Personal and Monitored
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showPersonalAlerts,
                onClick = { showPersonalAlerts = true },
                label = { Text("My Alerts") },
                leadingIcon = if (showPersonalAlerts) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )

            FilterChip(
                selected = !showPersonalAlerts,
                onClick = { showPersonalAlerts = false },
                label = { Text("Monitored Alerts") },
                leadingIcon = if (!showPersonalAlerts) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )
        }

        // Alerts list
        if (showPersonalAlerts) {
            PersonalAlertsList()
        } else {
            MonitoredAlertsList()
        }
    }
}

/**
 * Personal alerts list
 */
@Composable
private fun PersonalAlertsList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = "No alerts",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Text(
                "No personal alerts",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Monitored alerts list
 */
@Composable
private fun MonitoredAlertsList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "All safe",
                modifier = Modifier.size(64.dp),
                tint = Color.Green
            )
            Text(
                "All protected users are safe",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Green
            )
        }
    }
}

/**
 * Time Windows view
 */
@Composable
private fun TimeWindowsView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Time Window Button
        Button(
            onClick = {
                navController.navigate("add_time_window")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Time Window")
        }

        // Time windows list
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "No time windows",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Text(
                    "No time windows set",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Text(
                    "Create a time window to define monitoring periods",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Enum for Administration views
 */
enum class AdministrationView {
    ALERTS,
    TIME_WINDOWS
}