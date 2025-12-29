package com.example.safetysec.presentation.protectedUser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.MonitoringState
import com.example.safetysec.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Monitoring View Model
 *
 * Manages monitoring state and controls for Protected users
 */
@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val startMonitoringUseCase: StartMonitoringUseCase,
    private val stopMonitoringUseCase: StopMonitoringUseCase,
    private val getMonitoringStateUseCase: GetMonitoringStateUseCase,
    private val checkActiveTimeWindowUseCase: CheckActiveTimeWindowUseCase,
    private val triggerPanicButtonUseCase: TriggerPanicButtonUseCase
) : ViewModel() {

    // Monitoring state from repository
    val monitoringState: StateFlow<MonitoringState> = getMonitoringStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonitoringState()
        )

    // UI state
    private val _uiState = MutableStateFlow<MonitoringUiState>(MonitoringUiState.Idle)
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

    // Permission state
    private val _hasRequiredPermissions = MutableStateFlow(false)
    val hasRequiredPermissions: StateFlow<Boolean> = _hasRequiredPermissions.asStateFlow()

    /**
     * Start monitoring
     */
    fun startMonitoring() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading

            val result = startMonitoringUseCase()

            _uiState.value = if (result.isSuccess) {
                MonitoringUiState.Success("Monitoring started successfully")
            } else {
                MonitoringUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to start monitoring"
                )
            }

            // Reset to idle after showing message
            kotlinx.coroutines.delay(2000)
            _uiState.value = MonitoringUiState.Idle
        }
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading

            val result = stopMonitoringUseCase()

            _uiState.value = if (result.isSuccess) {
                MonitoringUiState.Success("Monitoring stopped")
            } else {
                MonitoringUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to stop monitoring"
                )
            }

            // Reset to idle after showing message
            kotlinx.coroutines.delay(2000)
            _uiState.value = MonitoringUiState.Idle
        }
    }

    /**
     * Trigger panic button alert
     */
    fun triggerPanicButton() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading

            val result = triggerPanicButtonUseCase()

            _uiState.value = if (result.isSuccess) {
                MonitoringUiState.Success("Panic alert sent to monitors")
            } else {
                MonitoringUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send panic alert"
                )
            }

            // Reset to idle after showing message
            kotlinx.coroutines.delay(2000)
            _uiState.value = MonitoringUiState.Idle
        }
    }

    /**
     * Update permission status
     */
    fun updatePermissionStatus(hasPermissions: Boolean) {
        _hasRequiredPermissions.value = hasPermissions
    }

    /**
     * Check if currently in active time window
     */
    fun checkTimeWindow(protectedUserId: String) {
        viewModelScope.launch {
            try {
                val isInWindow = checkActiveTimeWindowUseCase(protectedUserId)
                // State is already updated via monitoringState flow
            } catch (e: Exception) {
                _uiState.value = MonitoringUiState.Error(
                    "Failed to check time window: ${e.message}"
                )
            }
        }
    }
}

/**
 * Monitoring UI State
 */
sealed class MonitoringUiState {
    object Idle : MonitoringUiState()
    object Loading : MonitoringUiState()
    data class Success(val message: String) : MonitoringUiState()
    data class Error(val message: String) : MonitoringUiState()
}