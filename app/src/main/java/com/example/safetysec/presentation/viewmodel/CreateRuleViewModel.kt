package com.example.safetysec.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.*
import com.example.safetysec.domain.repository.AssociationRepository
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import com.example.safetysec.domain.usecase.rules.CreateRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Create Rule UI State
 */
data class CreateRuleUiState(
    val selectedProtectedUser: User? = null,
    val availableProtectedUsers: List<User> = emptyList(),
    val selectedRuleType: RuleType = RuleType.FALL_DETECTION,

    // Parameters
    val maxSpeed: String = "120",
    val inactivityDuration: String = "30",
    val geofenceAreas: List<GeofenceArea> = emptyList(),

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/**
 * Create Rule ViewModel
 *
 * Manages the state for creating a new rule
 */
@HiltViewModel
class CreateRuleViewModel @Inject constructor(
    private val createRuleUseCase: CreateRuleUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val associationRepository: AssociationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRuleUiState())
    val uiState: StateFlow<CreateRuleUiState> = _uiState.asStateFlow()

    private val currentUserFlow = getCurrentUserUseCase()

    init {
        loadProtectedUsers()
    }

    /**
     * Load available protected users from active associations
     */
    private fun loadProtectedUsers() {
        viewModelScope.launch {
            currentUserFlow.collectLatest { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@collectLatest
                }

                // Get active associations where current user is monitor
                associationRepository.getAssociationsAsMonitor(user.id)
                    .catch { exception ->
                        _uiState.update {
                            it.copy(
                                error = exception.message ?: "Failed to load protected users"
                            )
                        }
                    }
                    .collect { associations ->
                        val activeAssociations = associations.filter {
                            it.status == AssociationStatus.ACTIVE
                        }

                        val protectedUsers = activeAssociations.map { association ->
                            User(
                                id = association.protectedId,
                                name = association.protectedName,
                                email = association.protectedEmail
                            )
                        }

                        _uiState.update {
                            it.copy(
                                availableProtectedUsers = protectedUsers,
                                selectedProtectedUser = protectedUsers.firstOrNull()
                            )
                        }
                    }
            }
        }
    }

    /**
     * Select protected user
     */
    fun selectProtectedUser(user: User) {
        _uiState.update { it.copy(selectedProtectedUser = user) }
    }

    /**
     * Select rule type
     */
    fun selectRuleType(ruleType: RuleType) {
        _uiState.update { it.copy(selectedRuleType = ruleType) }
    }

    /**
     * Update max speed parameter
     */
    fun updateMaxSpeed(speed: String) {
        _uiState.update { it.copy(maxSpeed = speed) }
    }

    /**
     * Update inactivity duration parameter
     */
    fun updateInactivityDuration(duration: String) {
        _uiState.update { it.copy(inactivityDuration = duration) }
    }

    /**
     * Add geofence area
     */
    fun addGeofenceArea(area: GeofenceArea) {
        _uiState.update {
            it.copy(geofenceAreas = it.geofenceAreas + area)
        }
    }

    /**
     * Remove geofence area
     */
    fun removeGeofenceArea(areaId: String) {
        _uiState.update {
            it.copy(geofenceAreas = it.geofenceAreas.filter { area -> area.id != areaId })
        }
    }

    /**
     * Create the rule
     */
    fun createRule() {
        viewModelScope.launch {
            currentUserFlow.first { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@first true
                }

                val state = _uiState.value

                // Validate
                if (state.selectedProtectedUser == null) {
                    _uiState.update {
                        it.copy(error = "Please select a protected user")
                    }
                    return@first true
                }

                // Validate parameters based on rule type
                val parameters = when (state.selectedRuleType) {
                    RuleType.GEOFENCING -> {
                        if (state.geofenceAreas.isEmpty()) {
                            _uiState.update {
                                it.copy(error = "Please add at least one geofence area")
                            }
                            return@first true
                        }
                        RuleParameters(geofenceAreas = state.geofenceAreas)
                    }
                    RuleType.SPEED_CONTROL -> {
                        val maxSpeed = state.maxSpeed.toIntOrNull()
                        if (maxSpeed == null || maxSpeed <= 0) {
                            _uiState.update {
                                it.copy(error = "Please enter a valid maximum speed")
                            }
                            return@first true
                        }
                        RuleParameters(maxSpeed = maxSpeed)
                    }
                    RuleType.PROLONGED_INACTIVITY -> {
                        val duration = state.inactivityDuration.toIntOrNull()
                        if (duration == null || duration <= 0) {
                            _uiState.update {
                                it.copy(error = "Please enter a valid duration")
                            }
                            return@first true
                        }
                        RuleParameters(inactivityDuration = duration)
                    }
                    else -> RuleParameters()
                }

                // Create rule
                _uiState.update { it.copy(isLoading = true, error = null) }

                val rule = Rule(
                    monitorId = user.id,
                    monitorName = user.name,
                    protectedId = state.selectedProtectedUser.id,
                    protectedName = state.selectedProtectedUser.name,
                    type = state.selectedRuleType,
                    parameters = parameters
                )

                val result = createRuleUseCase(rule)

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
                                error = exception.message ?: "Failed to create rule",
                                isLoading = false
                            )
                        }
                    }
                )

                true
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset success state
     */
    fun resetSuccess() {
        _uiState.update { it.copy(success = false) }
    }
}