package com.example.safetysec.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.AlertEvent
import com.example.safetysec.domain.model.AlertType
import com.example.safetysec.domain.model.MonitorDashboardState
import com.example.safetysec.domain.repository.MonitorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.safetysec.domain.usecase.monitoring.GetActiveProtectedUseCase
import com.example.safetysec.domain.usecase.monitoring.GetRecentAlertsUseCase
import com.example.safetysec.domain.usecase.monitoring.GetProtectedUsersUseCase
import android.util.Log

@HiltViewModel
class MonitorDashboardViewModel @Inject constructor(
    private val getActiveProtectedUseCase: GetActiveProtectedUseCase,
    private val getRecentAlertsUseCase: GetRecentAlertsUseCase,
    private val getProtectedUsersUseCase: GetProtectedUsersUseCase
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(MonitorDashboardState())
    val dashboardState: StateFlow<MonitorDashboardState> = _dashboardState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                Log.d("MonitorViewModel", "Starting to load dashboard data...")
                _dashboardState.value = _dashboardState.value.copy(isLoading = true)

                Log.d("MonitorViewModel", "Fetching active protected count...")
                val activeCount = try {
                    getActiveProtectedUseCase()
                } catch (e: Exception) {
                    Log.e("MonitorViewModel", "Error getting active count", e)
                    0
                }

                Log.d("MonitorViewModel", "Fetching recent alerts...")
                val alerts = try {
                    getRecentAlertsUseCase()
                } catch (e: Exception) {
                    Log.e("MonitorViewModel", "Error getting alerts", e)
                    emptyList()
                }

                Log.d("MonitorViewModel", "Fetching protected users...")
                val protectedUsers = try {
                    getProtectedUsersUseCase()
                } catch (e: Exception) {
                    Log.e("MonitorViewModel", "Error getting protected users", e)
                    emptyList()
                }

                Log.d("MonitorViewModel", "Loaded: count=$activeCount, alerts=${alerts.size}, protected=${protectedUsers.size}")

                _dashboardState.value = MonitorDashboardState(
                    activeProtectedCount = activeCount,
                    recentAlerts = alerts,
                    activeProtected = protectedUsers,
                    isLoading = false
                )

                Log.d("MonitorViewModel", "Dashboard data loaded successfully")
            } catch (e: Exception) {
                Log.e("MonitorViewModel", "Unexpected error loading dashboard", e)
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun refreshDashboard() {
        loadDashboardData()
    }
}