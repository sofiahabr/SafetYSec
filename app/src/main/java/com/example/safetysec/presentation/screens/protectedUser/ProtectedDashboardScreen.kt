package com.example.safetysec.presentation.screens.protectedUser

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.navigation.AppRoutes
import com.example.safetysec.presentation.protectedUser.MonitoringViewModel
import com.example.safetysec.presentation.theme.*

/**
 * Protected User Dashboard Screen
 *
 * Main dashboard for Protected users showing:
 * - Monitoring status and controls
 * - Active rules
 * - Recent alerts
 * - Time windows
 */
@Composable
fun ProtectedDashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = true,
    showTopBar: Boolean = true,
    monitoringViewModel: MonitoringViewModel = hiltViewModel()
) {
    val monitoringState by monitoringViewModel.monitoringState.collectAsState()

    // Refresh statistics when screen is displayed
    LaunchedEffect(Unit) {
        monitoringViewModel.refreshStatistics()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                MainTopAppBar(
                    title = stringResource(R.string.protected_dashboard)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Monitoring Control Card - Primary feature
            MonitoringDashboardCard(
                monitoringState = monitoringState,
                onNavigateToMonitoring = {
                    navController.navigate(AppRoutes.MONITORING_CONTROL)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatInfoBox(
                    label = stringResource(R.string.active_rules),
                    value = monitoringState.activeRules.size.toString(),
                    borderColor = PrimaryPurple,
                    textColor = SecondaryPurple,
                    modifier = Modifier.weight(1f)
                )

                StatInfoBox(
                    label = stringResource(R.string.time_windows),
                    value = monitoringState.activeTimeWindows.size.toString(),
                    borderColor = InfoBlue,
                    textColor = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.AccessTime,
                    label = stringResource(R.string.time_windows),
                    onClick = { navController.navigate(AppRoutes.TIME_WINDOWS) },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.Rule,
                    label = stringResource(R.string.my_rules),
                    onClick = { navController.navigate(AppRoutes.RULES) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.alerts),
                    onClick = { navController.navigate(AppRoutes.ALERTS) },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.People,
                    label = stringResource(R.string.monitors),
                    onClick = { navController.navigate(AppRoutes.ASSOCIATIONS) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Quick Action Card Component
 */
@Composable
fun QuickActionCard(
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
                color = TextPrimary
            )
        }
    }
}