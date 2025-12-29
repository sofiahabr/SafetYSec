package com.example.safetysec.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.safetysec.domain.model.MonitoringState
import com.example.safetysec.presentation.theme.*

/**
 * Monitoring Dashboard Card
 *
 * A card component that shows monitoring status on the Protected user's dashboard
 * and provides quick access to monitoring controls
 */
@Composable
fun MonitoringDashboardCard(
    monitoringState: MonitoringState,
    onNavigateToMonitoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onNavigateToMonitoring,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (monitoringState.isRunning)
                SuccessGreen.copy(alpha = 0.1f)
            else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (monitoringState.isRunning) SuccessGreen else PrimaryPurple
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Monitoring",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Monitoring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (monitoringState.isRunning) SuccessGreen else DangerRed
                    ) {}

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (monitoringState.isRunning) {
                            "Active - ${monitoringState.getActiveRuleCount()} rule(s)"
                        } else {
                            "Inactive"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                if (monitoringState.isRunning && !monitoringState.isInActiveTimeWindow) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ Outside time window",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningOrange
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = TextSecondary
            )
        }
    }
}

/**
 * Compact Monitoring Status Badge
 *
 * A smaller badge component that can be used in top bars or headers
 */
@Composable
fun MonitoringStatusBadge(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isMonitoring) SuccessGreen.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (isMonitoring) SuccessGreen else DangerRed
            ) {}

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = if (isMonitoring) "Monitoring" else "Stopped",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isMonitoring) SuccessGreen else DangerRed
            )
        }
    }
}