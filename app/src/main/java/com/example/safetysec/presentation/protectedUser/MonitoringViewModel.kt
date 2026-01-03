package com.example.safetysec.presentation.protectedUser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.MonitoringState
import com.example.safetysec.domain.model.SensorData
import com.example.safetysec.domain.repository.MonitoringRepository
import com.example.safetysec.domain.usecase.StartMonitoringUseCase
import com.example.safetysec.domain.usecase.StopMonitoringUseCase
import com.example.safetysec.domain.usecase.alert.TriggerPanicButtonUseCase
import com.example.safetysec.domain.usecase.monitoring.*
import com.example.safetysec.service.LocationTracker
import com.example.safetysec.service.MonitoringService
import com.example.safetysec.service.SensorDataCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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
    @ApplicationContext private val context: Context,
    private val startMonitoringUseCase: StartMonitoringUseCase,
    private val stopMonitoringUseCase: StopMonitoringUseCase,
    private val monitoringRepository: MonitoringRepository,
    private val sensorDataCollector: SensorDataCollector,
    private val locationTracker: LocationTracker,
    private val triggerPanicButtonUseCase: TriggerPanicButtonUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MonitoringUiState>(MonitoringUiState.Idle)
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

    // Add permission tracking
    private val _hasRequiredPermissions = MutableStateFlow(false)
    val hasRequiredPermissions: StateFlow<Boolean> = _hasRequiredPermissions.asStateFlow()

    val monitoringState: StateFlow<MonitoringState> = monitoringRepository
        .getMonitoringStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonitoringState()
        )

    // Add permission update method
    fun updatePermissionStatus(granted: Boolean) {
        _hasRequiredPermissions.value = granted
    }

    fun startMonitoring() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading

            val result = startMonitoringUseCase()
            result.onSuccess {
                _uiState.value = MonitoringUiState.Success("Monitoring started")
            }.onFailure { error ->
                _uiState.value = MonitoringUiState.Error(error.message ?: "Failed to start monitoring")
            }
        }
    }

    fun stopMonitoring() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading

            val result = stopMonitoringUseCase()
            result.onSuccess {
                _uiState.value = MonitoringUiState.Success("Monitoring stopped")
            }.onFailure { error ->
                _uiState.value = MonitoringUiState.Error(error.message ?: "Failed to stop monitoring")
            }
        }
    }

    /**
     * Trigger panic button alert
     * CRITICAL: Now routes through MonitoringService to ensure video recording
     */
    fun triggerPanicButton() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading

            try {
                // CRITICAL: Trigger through MonitoringService so video recording happens
                MonitoringService.triggerPanic(context)

                // Give it a moment to process
                delay(500)

                _uiState.value = MonitoringUiState.Success("Emergency alert sent to monitors!")

                android.util.Log.d("MonitoringViewModel", "Panic button triggered through MonitoringService")
            } catch (e: Exception) {
                _uiState.value = MonitoringUiState.Error("Error: ${e.message}")
                android.util.Log.e("MonitoringViewModel", "Failed to trigger panic", e)
            }
        }
    }

    /**
     * Refresh statistics (rules and time windows) for current user
     */
    fun refreshStatistics() {
        viewModelScope.launch {
            monitoringRepository.refreshStatistics()
        }
    }

    // Add method to clear UI state
    fun clearUiState() {
        _uiState.value = MonitoringUiState.Idle
    }
}

sealed class MonitoringUiState {
    object Idle : MonitoringUiState()
    object Loading : MonitoringUiState()
    data class Success(val message: String) : MonitoringUiState()
    data class Error(val message: String) : MonitoringUiState()
}