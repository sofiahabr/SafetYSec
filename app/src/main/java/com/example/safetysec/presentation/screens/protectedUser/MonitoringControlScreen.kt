package com.example.safetysec.presentation.screens.protectedUser

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetysec.R
import com.example.safetysec.domain.model.MonitoringState
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.viewmodel.MonitoringUiState
import com.example.safetysec.presentation.viewmodel.MonitoringViewModel
import com.example.safetysec.presentation.theme.*

/**
 * Monitoring Control Screen
 *
 * Allows Protected users to start/stop monitoring and trigger panic button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringControlScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: MonitoringViewModel = hiltViewModel()
) {
    val monitoringState by viewModel.monitoringState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val hasPermissions by viewModel.hasRequiredPermissions.collectAsState()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.updatePermissionStatus(allGranted)
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Show snackbar for UI state messages
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is MonitoringUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                // Clear state after showing
                viewModel.clearUiState()
            }
            is MonitoringUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                // Clear state after showing
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.monitoring_control)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Monitoring Status Card
            MonitoringStatusCard(
                monitoringState = monitoringState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Active Rules Card
            if (monitoringState.activeRules.isNotEmpty()) {
                ActiveRulesCard(
                    monitoringState = monitoringState,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Time Window Status Card
            if (monitoringState.activeTimeWindows.isNotEmpty()) {
                TimeWindowStatusCard(
                    monitoringState = monitoringState,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Control Buttons
            if (!hasPermissions) {
                PermissionRequiredCard(
                    onRequestPermissions = {
                        val permissions = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (monitoringState.isRunning) {
                    // Panic Button - Prominent placement
                    PanicButtonSection(
                        onPanicPressed = { viewModel.triggerPanicButton() },
                        enabled = uiState !is MonitoringUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stop Monitoring Button
                    SecondaryButton(
                        text = stringResource(R.string.stop_monitoring),
                        onClick = { viewModel.stopMonitoring() },
                        enabled = uiState !is MonitoringUiState.Loading,
                        isLoading = uiState is MonitoringUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Start Monitoring Button
                    PrimaryButton(
                        text = stringResource(R.string.start_monitoring),
                        onClick = { viewModel.startMonitoring() },
                        enabled = uiState !is MonitoringUiState.Loading && hasPermissions,
                        isLoading = uiState is MonitoringUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sensor Data Card (for debugging/info)
            if (monitoringState.currentSensorData != null) {
                SensorDataCard(
                    monitoringState = monitoringState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Monitoring Status Card
 */
@Composable
private fun MonitoringStatusCard(
    monitoringState: MonitoringState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (monitoringState.isRunning) SuccessGreen else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (monitoringState.isRunning) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (monitoringState.isRunning) Color.White else PrimaryPurple
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (monitoringState.isRunning)
                        stringResource(R.string.monitoring_active)
                    else
                        stringResource(R.string.monitoring_stopped),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (monitoringState.isRunning) Color.White else TextPrimary
                )

                Text(
                    text = monitoringState.getStatusMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (monitoringState.isRunning) Color.White.copy(alpha = 0.9f) else TextSecondary
                )
            }
        }
    }
}

/**
 * Active Rules Card
 */
@Composable
private fun ActiveRulesCard(
    monitoringState: MonitoringState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Rule,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.active_rules_count, monitoringState.getActiveRuleCount()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            monitoringState.activeRules.forEach { rule ->
                if (rule.isActive()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = rule.type.toDisplayString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = rule.getDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Time Window Status Card
 */
@Composable
private fun TimeWindowStatusCard(
    monitoringState: MonitoringState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.time_windows),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (monitoringState.isInActiveTimeWindow)
                        Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (monitoringState.isInActiveTimeWindow) SuccessGreen else WarningOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (monitoringState.isInActiveTimeWindow)
                        stringResource(R.string.in_active_time_window)
                    else
                        stringResource(R.string.outside_time_windows),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Permission Required Card
 */
@Composable
private fun PermissionRequiredCard(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.permissions_required),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.permissions_required_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = stringResource(R.string.grant_permissions),
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Sensor Data Card (for debugging)
 */
@Composable
private fun SensorDataCard(
    monitoringState: MonitoringState,
    modifier: Modifier = Modifier
) {
    val sensorData = monitoringState.currentSensorData ?: return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sensor_data),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SecondaryPurple
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.acceleration_value,
                    String.format("%.2f", sensorData.getTotalAcceleration())
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (sensorData.hasLocation()) {
                Text(
                    text = stringResource(
                        R.string.speed_value,
                        sensorData.getSpeedKmh()?.let { String.format("%.1f", it) } ?: "N/A"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = stringResource(
                    R.string.activity_value,
                    sensorData.activityType.toDisplayString(),
                    sensorData.activityConfidence
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * Panic Button Section - Prominent Emergency Button
 */
@Composable
fun PanicButtonSection(
    onPanicPressed: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = stringResource(R.string.emergency_alert),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )

            Text(
                text = stringResource(R.string.emergency_alert_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onPanicPressed,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.panic_button),
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.panic_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}