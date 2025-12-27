package com.example.safetysec.presentation.screens.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.safetysec.domain.model.*
import com.example.safetysec.domain.usecase.rules.GetRulesUseCase
import com.example.safetysec.domain.usecase.rules.UpdateRuleUseCase
import com.example.safetysec.presentation.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Edit Rule UI State
 */
data class EditRuleUiState(
    val rule: Rule? = null,
    val maxSpeed: String = "120",
    val inactivityDuration: String = "30",
    val geofenceAreas: List<GeofenceArea> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/**
 * Edit Rule ViewModel
 */
@HiltViewModel
class EditRuleViewModel @Inject constructor(
    private val getRulesUseCase: GetRulesUseCase,
    private val updateRuleUseCase: UpdateRuleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRuleUiState())
    val uiState: StateFlow<EditRuleUiState> = _uiState.asStateFlow()

    fun loadRule(ruleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Note: In a real implementation, you'd have a getRuleById use case
            // For now, this is a placeholder
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Rule loading not yet implemented. Use the repository's getRuleById method."
                )
            }
        }
    }

    fun updateMaxSpeed(speed: String) {
        _uiState.update { it.copy(maxSpeed = speed) }
    }

    fun updateInactivityDuration(duration: String) {
        _uiState.update { it.copy(inactivityDuration = duration) }
    }

    fun addGeofenceArea(area: GeofenceArea) {
        _uiState.update {
            it.copy(geofenceAreas = it.geofenceAreas + area)
        }
    }

    fun removeGeofenceArea(areaId: String) {
        _uiState.update {
            it.copy(geofenceAreas = it.geofenceAreas.filter { area -> area.id != areaId })
        }
    }

    fun updateRule() {
        viewModelScope.launch {
            val state = _uiState.value
            val rule = state.rule ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            // Build updated parameters based on rule type
            val parameters = when (rule.type) {
                RuleType.GEOFENCING -> {
                    if (state.geofenceAreas.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                error = "Please add at least one geofence area",
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                    RuleParameters(geofenceAreas = state.geofenceAreas)
                }
                RuleType.SPEED_CONTROL -> {
                    val maxSpeed = state.maxSpeed.toIntOrNull()
                    if (maxSpeed == null || maxSpeed <= 0) {
                        _uiState.update {
                            it.copy(
                                error = "Please enter a valid maximum speed",
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                    RuleParameters(maxSpeed = maxSpeed)
                }
                RuleType.PROLONGED_INACTIVITY -> {
                    val duration = state.inactivityDuration.toIntOrNull()
                    if (duration == null || duration <= 0) {
                        _uiState.update {
                            it.copy(
                                error = "Please enter a valid duration",
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                    RuleParameters(inactivityDuration = duration)
                }
                else -> RuleParameters()
            }

            val updatedRule = rule.copy(parameters = parameters)
            val result = updateRuleUseCase(updatedRule)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            success = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to update rule",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Edit Rule Screen
 */
@Composable
fun EditRuleScreen(
    ruleId: String,
    navController: NavController,
    viewModel: EditRuleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ruleId) {
        viewModel.loadRule(ruleId)
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Edit Rule",
                onNavigationClick = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.rule == null && !uiState.isLoading) {
                EmptyState(
                    icon = Icons.Default.Delete,
                    title = "Rule Not Found",
                    message = "The rule you're trying to edit could not be found.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.error != null) {
                        ErrorAlert(
                            message = uiState.error ?: "",
                            onDismiss = { viewModel.clearError() }
                        )
                    }

                    uiState.rule?.let { rule ->
                        // Rule info
                        Text(
                            text = "Rule Type: ${rule.type.toDisplayString()}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "For: ${rule.protectedName}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Divider()

                        // Parameters based on rule type
                        if (rule.type.requiresParameters()) {
                            RuleParametersInput(
                                ruleType = rule.type,
                                maxSpeed = uiState.maxSpeed,
                                onMaxSpeedChange = { viewModel.updateMaxSpeed(it) },
                                inactivityDuration = uiState.inactivityDuration,
                                onInactivityDurationChange = { viewModel.updateInactivityDuration(it) },
                                geofenceAreas = uiState.geofenceAreas,
                                onAddGeofenceArea = { viewModel.addGeofenceArea(it) },
                                onRemoveGeofenceArea = { viewModel.removeGeofenceArea(it) }
                            )
                        } else {
                            InfoAlert(
                                title = "No Parameters",
                                message = "This rule type doesn't have configurable parameters."
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PrimaryButton(
                            text = "Save Changes",
                            onClick = { viewModel.updateRule() },
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.rule != null) {
                LoadingDialog(message = "Updating rule...")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Geofence Area") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Area Name"
                )
                CustomTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = "Latitude",
                    keyboardType = KeyboardType.Decimal
                )
                CustomTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = "Longitude",
                    keyboardType = KeyboardType.Decimal
                )
                CustomTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = "Radius (meters)",
                    keyboardType = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val area = GeofenceArea(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        latitude = latitude.toDoubleOrNull() ?: 0.0,
                        longitude = longitude.toDoubleOrNull() ?: 0.0,
                        radius = radius.toDoubleOrNull() ?: 100.0
                    )
                    onConfirm(area)
                },
                enabled = name.isNotBlank() && latitude.isNotBlank() && longitude.isNotBlank()
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