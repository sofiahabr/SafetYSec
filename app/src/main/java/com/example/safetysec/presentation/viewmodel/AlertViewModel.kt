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
                android.util.Log.d("AlertViewModel", "Loaded ${alerts.size} alerts from Firestore")
                _uiState.update {
                    it.copy(
                        alerts = alerts,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AlertViewModel", "Failed to load alerts", e)
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
                    android.util.Log.d("AlertViewModel", "═══════════════════════════════════")
                    android.util.Log.d("AlertViewModel", "Received alert from listener: ${alert.id}")
                    android.util.Log.d("AlertViewModel", "Alert type: ${alert.type}")
                    android.util.Log.d("AlertViewModel", "Has video: ${alert.videoUrl != null}")
                    android.util.Log.d("AlertViewModel", "Is cancelled: ${alert.isCancelled}")

                    _alertStream.value = alert

                    // Add new alert to the list (with deduplication)
                    _uiState.update { state ->
                        android.util.Log.d("AlertViewModel", "Current alerts in list: ${state.alerts.size}")
                        android.util.Log.d("AlertViewModel", "Alert IDs in list: ${state.alerts.map { it.id }}")

                        // Check if alert already exists in the list
                        val alertExists = state.alerts.any { it.id == alert.id }
                        android.util.Log.d("AlertViewModel", "Alert exists in list: $alertExists")

                        if (alertExists) {
                            // Alert already in list, update it instead of adding
                            android.util.Log.d("AlertViewModel", "→ UPDATING existing alert: ${alert.id}")
                            val updatedAlerts = state.alerts.map { existingAlert ->
                                if (existingAlert.id == alert.id) {
                                    android.util.Log.d("AlertViewModel", "  ✓ Replaced alert - Video: ${alert.videoUrl != null}, Cancelled: ${alert.isCancelled}")
                                    alert
                                } else {
                                    existingAlert
                                }
                            }
                            android.util.Log.d("AlertViewModel", "New list size: ${updatedAlerts.size}")
                            state.copy(alerts = updatedAlerts)
                        } else {
                            // New alert, add to top of list
                            android.util.Log.d("AlertViewModel", "→ ADDING new alert: ${alert.id}")
                            val updatedAlerts = listOf(alert) + state.alerts
                            android.util.Log.d("AlertViewModel", "New list size: ${updatedAlerts.size}")
                            state.copy(alerts = updatedAlerts)
                        }
                    }

                    android.util.Log.d("AlertViewModel", "═══════════════════════════════════")
                }
            } catch (e: Exception) {
                // Real-time updates failed, but we can still show cached data
                android.util.Log.e("AlertViewModel", "Error in subscribeToAlerts", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Get alert by ID and subscribe to real-time updates
     */
    fun getAlertById(alertId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load initial alert data
                val alert = monitorRepository.getAlertById(alertId)
                _uiState.update {
                    it.copy(
                        selectedAlert = alert,
                        isLoading = false,
                        error = null
                    )
                }

                // Subscribe to real-time updates for this specific alert
                monitorRepository.subscribeToAlertById(alertId).collect { updatedAlert ->
                    if (updatedAlert != null) {
                        _uiState.update {
                            it.copy(selectedAlert = updatedAlert)
                        }
                    }
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