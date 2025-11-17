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
                _dashboardState.value = _dashboardState.value.copy(isLoading = true)
                 val activeCount = getActiveProtectedUseCase()
                 val alerts = getRecentAlertsUseCase()
                val protectedUsers = getProtectedUsersUseCase()

                _dashboardState.value = MonitorDashboardState(
                    activeProtectedCount = activeCount,
                    recentAlerts = alerts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refreshDashboard() {
        loadDashboardData()
    }
}