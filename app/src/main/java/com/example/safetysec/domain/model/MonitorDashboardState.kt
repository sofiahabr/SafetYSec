package com.example.safetysec.domain.model

data class MonitorDashboardState(
    val activeProtectedCount: Int = 0,
    val recentAlerts: List<AlertEvent> = emptyList(),
    val isLoading: Boolean = false,
    val activeProtected: List<ProtectedUserSummary> = emptyList(),
    val error: String? = null
)