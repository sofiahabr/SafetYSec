package com.example.safetysec.presentation.screens.monitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.ProtectedUserSummary
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.navigation.AppRoutes
import com.example.safetysec.presentation.theme.*
import com.example.safetysec.presentation.viewmodel.MonitorDashboardViewModel

/**
 * Enhanced Monitor Dashboard Screen
 *
 * Modern dashboard for Monitor users showing:
 * - Overview statistics
 * - Protected users status
 * - Recent alerts
 * - Quick action buttons
 */
@Composable
fun MonitorDashScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MonitorDashboardViewModel = hiltViewModel(),
    showBottomBar: Boolean = true,
    showTopBar: Boolean = true
) {
    val state by viewModel.dashboardState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                MainTopAppBar(
                    title = stringResource(R.string.monitor_dashboard)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                FullScreenLoading(message = stringResource(R.string.loading_dashboard))
            }

            state.error != null -> {
                ErrorAlert(message = state.error!!)
            }

            else -> {
                MonitorDashboardContent(
                    navController = navController,
                    activeProtectedCount = state.activeProtectedCount,
                    recentAlerts = state.recentAlerts,
                    activeProtected = state.activeProtected,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun MonitorDashboardContent(
    navController: NavController,
    activeProtectedCount: Int,
    recentAlerts: List<AlertEvent>,
    activeProtected: List<ProtectedUserSummary>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatInfoBox(
                label = stringResource(R.string.protected_users),
                value = activeProtectedCount.toString(),
                borderColor = SuccessGreen,
                textColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatInfoBox(
                label = stringResource(R.string.recent_alerts),
                value = recentAlerts.size.toString(),
                borderColor = DangerRed,
                textColor = DangerRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions Section
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Quick Action Cards - 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonitorQuickActionCard(
                icon = Icons.Default.Add,
                label = stringResource(R.string.add_protected_user),
                onClick = { navController.navigate(AppRoutes.ASSOCIATIONS) },
                modifier = Modifier.weight(1f)
            )

            MonitorQuickActionCard(
                icon = Icons.Default.Rule,
                label = stringResource(R.string.create_rule),
                onClick = { navController.navigate(AppRoutes.CREATE_RULE) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonitorQuickActionCard(
                icon = Icons.Default.Notifications,
                label = stringResource(R.string.view_alerts),
                onClick = { navController.navigate(AppRoutes.ALERTS) },
                modifier = Modifier.weight(1f)
            )

            MonitorQuickActionCard(
                icon = Icons.Default.People,
                label = stringResource(R.string.manage_users),
                onClick = { navController.navigate(AppRoutes.ASSOCIATIONS) },
                modifier = Modifier.weight(1f)
            )
        }

        // Protected Users Section
        if (activeProtected.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.protected_individuals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { navController.navigate(AppRoutes.ASSOCIATIONS) }) {
                    Text(stringResource(R.string.view_all))
                }
            }

            activeProtected.take(3).forEach { protectedUser ->
                ProtectedUserCard(
                    protectedUser = protectedUser,
                    onClick = { /* Navigate to user details */ },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (activeProtected.size > 3) {
                Card(
                    onClick = { navController.navigate(AppRoutes.ASSOCIATIONS) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = LightPurple
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.all_protected_users, activeProtected.size - 3),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Empty state for protected users
            EmptyProtectedUsersCard(
                onAddClick = { navController.navigate(AppRoutes.ASSOCIATIONS) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent Alerts Section
        if (recentAlerts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_alerts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { navController.navigate(AppRoutes.ALERTS) }) {
                    Text(stringResource(R.string.view_all))
                }
            }

            recentAlerts.take(3).forEach { alert ->
                AlertEventCard(
                    title = alert.type.toDisplayString(),
                    subtitle = "${alert.protectedUserName}  ${getTimeAgo(alert.timestamp)}",
                    details = alert.details,
                    location = alert.getFormattedLocation(),
                    onActionClick = { /* Handle action */ },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Monitor Quick Action Card
 */
@Composable
fun MonitorQuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryPurple,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Protected User Card - Enhanced version
 */
@Composable
fun ProtectedUserCard(
    protectedUser: ProtectedUserSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (protectedUser.isActive)
                        SuccessGreen.copy(alpha = 0.1f)
                    else Color.Gray.copy(alpha = 0.1f)
                ) {
                    Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (protectedUser.isActive) SuccessGreen else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = protectedUser.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = MaterialTheme.shapes.small,
                            color = if (protectedUser.isActive) SuccessGreen else Color.Gray
                        ) {}

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = stringResource(
                                if (protectedUser.isActive) R.string.monitoring_active
                                else R.string.monitoring_inactive
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.view_details),
                tint = TextSecondary
            )
        }
    }
}

/**
 * Empty Protected Users Card
 */
@Composable
fun EmptyProtectedUsersCard(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = LightPurple
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_protected_users),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.no_protected_users_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = stringResource(R.string.add_protected_user),
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Helper function to format time ago
 */
private fun getTimeAgo(timestamp: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val minutes = java.time.temporal.ChronoUnit.MINUTES.between(timestamp, now)
    val hours = java.time.temporal.ChronoUnit.HOURS.between(timestamp, now)
    val days = java.time.temporal.ChronoUnit.DAYS.between(timestamp, now)

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}