package com.example.safetysec.presentation.screens.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
                title = "Create Rule",
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
                        message = "You don't have any active protected users. Please establish an association first."
                    )
                } else {
                    Text(
                        text = "Select Protected User",
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
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        }
                    }

                    Divider()

                    // Rule Type Selection
                    Text(
                        text = "Select Rule Type",
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
                                        contentDescription = "Selected"
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
                        text = "Create Rule",
                        onClick = { viewModel.createRule() },
                        isLoading = uiState.isLoading,
                        enabled = uiState.selectedProtectedUser != null
                    )
                }
            }

            if (uiState.isLoading) {
                LoadingDialog(message = "Creating rule...")
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
        text = "Parameters",
        style = MaterialTheme.typography.titleMedium
    )

    when (ruleType) {
        RuleType.SPEED_CONTROL -> {
            CustomTextField(
                value = maxSpeed,
                onValueChange = onMaxSpeedChange,
                label = "Maximum Speed (km/h)",
                keyboardType = KeyboardType.Number
            )
        }
        RuleType.PROLONGED_INACTIVITY -> {
            CustomTextField(
                value = inactivityDuration,
                onValueChange = onInactivityDurationChange,
                label = "Duration (minutes)",
                keyboardType = KeyboardType.Number
            )
        }
        RuleType.GEOFENCING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Geofence Areas (${geofenceAreas.size})",
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
                                    text = "${area.radius.toInt()}m radius",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onRemoveGeofenceArea(area.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }

                SecondaryButton(
                    text = "Add Area",
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
        title = { Text("Add Geofence Area") },
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
                    label = "Area Name",
                    placeholder = "e.g., Home, School"
                )

                CustomTextField(
                    value = latitude,
                    onValueChange = {
                        latitude = it
                        errorMessage = null
                    },
                    label = "Latitude",
                    placeholder = "e.g., 41.1579",
                    keyboardType = KeyboardType.Decimal,
                    supportingText = "Range: -90 to 90"
                )

                CustomTextField(
                    value = longitude,
                    onValueChange = {
                        longitude = it
                        errorMessage = null
                    },
                    label = "Longitude",
                    placeholder = "e.g., -8.6291",
                    keyboardType = KeyboardType.Decimal,
                    supportingText = "Range: -180 to 180"
                )

                CustomTextField(
                    value = radius,
                    onValueChange = {
                        radius = it
                        errorMessage = null
                    },
                    label = "Radius (meters)",
                    placeholder = "100",
                    keyboardType = KeyboardType.Number,
                    supportingText = "Range: 1 to 100,000m"
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
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}