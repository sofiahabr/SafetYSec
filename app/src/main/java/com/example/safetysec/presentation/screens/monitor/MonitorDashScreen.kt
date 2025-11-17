package com.example.safetysec.presentation.screens.monitor


import com.example.safetysec.presentation.components.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import com.example.safetysec.presentation.viewmodel.MonitorDashboardViewModel
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.ProtectedUserSummary
import com.example.safetysec.presentation.components.BottomNavigationBar


@Composable
fun MonitorDashScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MonitorDashboardViewModel

) {
    val state = viewModel.dashboardState.value

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->

        when {
            state.isLoading -> {
                FullScreenLoading(message = "Loading dashboard data...")
            }

            state.error != null -> {
                ErrorAlert(message = state.error)
            }

            else -> {
                MonitorDashboardContent(
                    activeProtectedCount = state.activeProtectedCount,
                    recentAlerts = state.recentAlerts,
                    modifier = Modifier.padding(innerPadding),
                    activeProtected = state.activeProtected
                )
            }
        }
    }
}

@Composable
fun MonitorDashboardContent(
    activeProtectedCount: Int,
    recentAlerts: List<AlertEvent>,
    modifier: Modifier = Modifier,
    activeProtected: List<ProtectedUserSummary>
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))


        Text(
            text = "Monitor Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatInfoBox(
                label = "Active protected",
                value = activeProtectedCount.toString(),
                borderColor = Color(0xFF4CAF50),
                textColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )

            StatInfoBox(
                label = "Recent alerts",
                value = recentAlerts.size.toString(),
                borderColor = Color(0xFFEF5350),
                textColor = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }

        if (recentAlerts.isNotEmpty()) {
            Text(
                text = "Recent Alerts",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            recentAlerts.forEach { alert ->
                AlertEventCard(
                    title = alert.type.toDisplayString(),
                    subtitle = "${alert.protectedUserName} - ${getTimeAgo(alert.timestamp)}",
                    details = alert.details,
                    location = alert.getFormattedLocation(),
                    onActionClick = { /* Handle action */ },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeProtected.isNotEmpty()) {
            Text(
                text = "Protected Individuals",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            activeProtected.forEach { protectedUser ->
                ProtectedInfoCard(
                    name = protectedUser.name,
                    email = protectedUser.email,
                    isActive = protectedUser.isActive,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun getTimeAgo(timestamp: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val minutes = java.time.temporal.ChronoUnit.MINUTES.between(timestamp, now)
    val hours = java.time.temporal.ChronoUnit.HOURS.between(timestamp, now)
    val days = java.time.temporal.ChronoUnit.DAYS.between(timestamp, now)

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "$minutes mins ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}

