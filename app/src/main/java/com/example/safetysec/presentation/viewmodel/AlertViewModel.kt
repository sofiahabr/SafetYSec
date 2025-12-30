package com.example.safetysec.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.repository.MonitorRepository
import com.example.safetysec.domain.usecase.monitoring.GetRecentAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Alert ViewModel
 *
 * Manages alert state, history, and real-time updates
 */
@HiltViewModel
class AlertViewModel @Inject constructor(
    private val getRecentAlertsUseCase: GetRecentAlertsUseCase,
    private val monitorRepository: MonitorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    private val _alertStream = MutableStateFlow<AlertEvent?>(null)
    val alertStream: StateFlow<AlertEvent?> = _alertStream.asStateFlow()

    init {
        loadAlerts()
        subscribeToAlerts()
    }

    /**
     * Load recent alerts
     */
    fun loadAlerts(limit: Int = 50) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val alerts = getRecentAlertsUseCase(limit)
                _uiState.update {
                    it.copy(
                        alerts = alerts,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load alerts"
                    )
                }
            }
        }
    }

    /**
     * Subscribe to real-time alert updates
     */
    private fun subscribeToAlerts() {
        viewModelScope.launch {
            try {
                monitorRepository.subscribeToAlerts().collect { alert ->
                    _alertStream.value = alert

                    // Add new alert to the list
                    _uiState.update { state ->
                        val updatedAlerts = listOf(alert) + state.alerts
                        state.copy(alerts = updatedAlerts)
                    }
                }
            } catch (e: Exception) {
                // Real-time updates failed, but we can still show cached data
                e.printStackTrace()
            }
        }
    }

    /**
     * Get alert by ID
     */
    fun getAlertById(alertId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val alert = monitorRepository.getAlertById(alertId)
                _uiState.update {
                    it.copy(
                        selectedAlert = alert,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load alert details"
                    )
                }
            }
        }
    }

    /**
     * Filter alerts by type
     */
    fun filterAlertsByType(type: String?) {
        _uiState.update { it.copy(selectedFilter = type) }
    }

    /**
     * Clear selected alert
     */
    fun clearSelectedAlert() {
        _uiState.update { it.copy(selectedAlert = null) }
    }

    /**
     * Refresh alerts
     */
    fun refresh() {
        loadAlerts()
    }

    /**
     * Export alerts to CSV (placeholder for now)
     */
    fun exportAlerts(alerts: List<AlertEvent>) {
        viewModelScope.launch {
            try {
                // TODO: Implement export functionality
                // For now, just log
                android.util.Log.d("AlertViewModel", "Exporting ${alerts.size} alerts")

                // Future implementation:
                // - Generate CSV string
                // - Save to file or share
                // - Show success message
            } catch (e: Exception) {
                android.util.Log.e("AlertViewModel", "Export failed", e)
            }
        }
    }
}

/**
 * Alert UI State
 */
data class AlertUiState(
    val alerts: List<AlertEvent> = emptyList(),
    val selectedAlert: AlertEvent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: String? = null
) {
    /**
     * Get filtered alerts
     */
    fun getFilteredAlerts(): List<AlertEvent> {
        return if (selectedFilter == null) {
            alerts
        } else {
            alerts.filter { it.type.name == selectedFilter }
        }
    }

    /**
     * Get alert statistics
     */
    fun getAlertStatistics(): Map<String, Int> {
        return alerts.groupingBy { it.type.name }.eachCount()
    }
}