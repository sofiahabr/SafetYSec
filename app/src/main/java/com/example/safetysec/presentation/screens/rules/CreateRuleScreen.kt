package com.example.safetysec.presentation.screens.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.domain.model.*
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.viewmodel.CreateRuleViewModel
import java.util.UUID

@Composable
fun CreateRuleScreen(
    navController: NavController,
    viewModel: CreateRuleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate back on success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = stringResource(R.string.create_rule),
                onNavigationClick = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error alert
                if (uiState.error != null) {
                    ErrorAlert(
                        message = uiState.error ?: "",
                        onDismiss = { viewModel.clearError() }
                    )
                }

                // Protected User Selection
                if (uiState.availableProtectedUsers.isEmpty()) {
                    WarningAlert(
                        message = stringResource(R.string.no_active_associations)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.select_protected_user),
                        style = MaterialTheme.typography.titleMedium
                    )

                    uiState.availableProtectedUsers.forEach { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.selectProtectedUser(user) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedProtectedUser == user)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = user.name)
                                if (uiState.selectedProtectedUser == user) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.selected)
                                    )
                                }
                            }
                        }
                    }

                    Divider()

                    // Rule Type Selection
                    Text(
                        text = stringResource(R.string.select_rule_type),
                        style = MaterialTheme.typography.titleMedium
                    )

                    RuleType.entries.forEach { ruleType ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.selectRuleType(ruleType) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedRuleType == ruleType)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ruleType.toDisplayString(),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (uiState.selectedRuleType == ruleType) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.selected)
                                    )
                                }
                            }
                        }
                    }

                    // Parameters based on rule type
                    if (uiState.selectedRuleType.requiresParameters()) {
                        Divider()
                        RuleParametersInput(
                            ruleType = uiState.selectedRuleType,
                            maxSpeed = uiState.maxSpeed,
                            onMaxSpeedChange = { viewModel.updateMaxSpeed(it) },
                            inactivityDuration = uiState.inactivityDuration,
                            onInactivityDurationChange = { viewModel.updateInactivityDuration(it) },
                            geofenceAreas = uiState.geofenceAreas,
                            onAddGeofenceArea = { viewModel.addGeofenceArea(it) },
                            onRemoveGeofenceArea = { viewModel.removeGeofenceArea(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Create button
                    PrimaryButton(
                        text = stringResource(R.string.create_rule),
                        onClick = { viewModel.createRule() },
                        isLoading = uiState.isLoading,
                        enabled = uiState.selectedProtectedUser != null
                    )
                }
            }

            if (uiState.isLoading) {
                LoadingDialog(message = stringResource(R.string.creating_rule))
            }
        }
    }
}

@Composable
private fun RuleParametersInput(
    ruleType: RuleType,
    maxSpeed: String,
    onMaxSpeedChange: (String) -> Unit,
    inactivityDuration: String,
    onInactivityDurationChange: (String) -> Unit,
    geofenceAreas: List<GeofenceArea>,
    onAddGeofenceArea: (GeofenceArea) -> Unit,
    onRemoveGeofenceArea: (String) -> Unit
) {
    var showAddAreaDialog by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.parameters),
        style = MaterialTheme.typography.titleMedium
    )

    when (ruleType) {
        RuleType.SPEED_CONTROL -> {
            CustomTextField(
                value = maxSpeed,
                onValueChange = onMaxSpeedChange,
                label = stringResource(R.string.max_speed_kmh),
                keyboardType = KeyboardType.Number
            )
        }
        RuleType.PROLONGED_INACTIVITY -> {
            CustomTextField(
                value = inactivityDuration,
                onValueChange = onInactivityDurationChange,
                label = stringResource(R.string.duration_minutes),
                keyboardType = KeyboardType.Number
            )
        }
        RuleType.GEOFENCING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.geofence_areas_count, geofenceAreas.size),
                    style = MaterialTheme.typography.bodyMedium
                )

                geofenceAreas.forEach { area ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = area.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.meter_radius_value, area.radius.toInt()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onRemoveGeofenceArea(area.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove)
                                )
                            }
                        }
                    }
                }

                SecondaryButton(
                    text = stringResource(R.string.add_area),
                    onClick = { showAddAreaDialog = true }
                )
            }
        }
        else -> {}
    }

    if (showAddAreaDialog) {
        AddGeofenceAreaDialog(
            onDismiss = { showAddAreaDialog = false },
            onConfirm = { area ->
                onAddGeofenceArea(area)
                showAddAreaDialog = false
            }
        )
    }
}

@Composable
private fun AddGeofenceAreaDialog(
    onDismiss: () -> Unit,
    onConfirm: (GeofenceArea) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Validation functions
    fun validateLatitude(value: String): Boolean {
        val lat = value.toDoubleOrNull() ?: return false
        return lat in -90.0..90.0
    }

    fun validateLongitude(value: String): Boolean {
        val lon = value.toDoubleOrNull() ?: return false
        return lon in -180.0..180.0
    }

    fun validateRadius(value: String): Boolean {
        val rad = value.toDoubleOrNull() ?: return false
        return rad > 0 && rad <= 100000 // Max 100km radius
    }

    fun validateAndCreate() {
        when {
            name.isBlank() -> {
                errorMessage = "Area name is required"
            }
            latitude.isBlank() -> {
                errorMessage = "Latitude is required"
            }
            !validateLatitude(latitude) -> {
                errorMessage = "Latitude must be between -90 and 90"
            }
            longitude.isBlank() -> {
                errorMessage = "Longitude is required"
            }
            !validateLongitude(longitude) -> {
                errorMessage = "Longitude must be between -180 and 180"
            }
            radius.isBlank() -> {
                errorMessage = "Radius is required"
            }
            !validateRadius(radius) -> {
                errorMessage = "Radius must be between 1 and 100,000 meters"
            }
            else -> {
                val area = GeofenceArea(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    latitude = latitude.toDouble(),
                    longitude = longitude.toDouble(),
                    radius = radius.toDouble()
                )
                onConfirm(area)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_geofence_area)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                CustomTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = stringResource(R.string.area_name),
                    placeholder = stringResource(R.string.area_name_placeholder)
                )

                CustomTextField(
                    value = latitude,
                    onValueChange = {
                        latitude = it
                        errorMessage = null
                    },
                    label = stringResource(R.string.latitude),
                    placeholder = stringResource(R.string.latitude_placeholder),
                    keyboardType = KeyboardType.Decimal,
                    supportingText = stringResource(R.string.latitude_range)
                )

                CustomTextField(
                    value = longitude,
                    onValueChange = {
                        longitude = it
                        errorMessage = null
                    },
                    label = stringResource(R.string.longitude),
                    placeholder = stringResource(R.string.longitude_placeholder),
                    keyboardType = KeyboardType.Decimal,
                    supportingText = stringResource(R.string.longitude_range)
                )

                CustomTextField(
                    value = radius,
                    onValueChange = {
                        radius = it
                        errorMessage = null
                    },
                    label = stringResource(R.string.radius_meters),
                    placeholder = "100",
                    keyboardType = KeyboardType.Number,
                    supportingText = stringResource(R.string.radius_range)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndCreate() },
                enabled = name.isNotBlank() &&
                        latitude.isNotBlank() &&
                        longitude.isNotBlank() &&
                        radius.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}