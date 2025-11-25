package com.example.safetysec.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.DayOfWeek
import com.example.safetysec.domain.model.TimeWindow
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import com.example.safetysec.domain.usecase.timewindows.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Time Windows UI State
 */
data class TimeWindowsUiState(
    val timeWindows: List<TimeWindow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Time Window ViewModel
 *
 * Manages time windows for Protected users
 */
@HiltViewModel
class TimeWindowViewModel @Inject constructor(
    private val getTimeWindowsUseCase: GetTimeWindowsUseCase,
    private val createTimeWindowUseCase: CreateTimeWindowUseCase,
    private val updateTimeWindowUseCase: UpdateTimeWindowUseCase,
    private val deleteTimeWindowUseCase: DeleteTimeWindowUseCase,
    private val isMonitoringActiveUseCase: IsMonitoringActiveUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeWindowsUiState())
    val uiState: StateFlow<TimeWindowsUiState> = _uiState.asStateFlow()

    private val currentUserFlow = getCurrentUserUseCase()

    init {
        loadTimeWindows()
    }

    /**
     * Load time windows for current user
     */
    private fun loadTimeWindows() {
        viewModelScope.launch {
            currentUserFlow.collectLatest { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@collectLatest
                }

                _uiState.update { it.copy(isLoading = true) }

                getTimeWindowsUseCase(user.id)
                    .catch { exception ->
                        _uiState.update {
                            it.copy(
                                error = exception.message ?: "Failed to load time windows",
                                isLoading = false
                            )
                        }
                    }
                    .collect { timeWindows ->
                        _uiState.update {
                            it.copy(
                                timeWindows = timeWindows,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            }
        }
    }

    /**
     * Create a new time window
     */
    fun createTimeWindow(timeWindow: TimeWindow) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = createTimeWindowUseCase(timeWindow)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to create time window",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Update a time window
     */
    fun updateTimeWindow(timeWindow: TimeWindow) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = updateTimeWindowUseCase(timeWindow)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to update time window",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Delete a time window
     */
    fun deleteTimeWindow(timeWindowId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = deleteTimeWindowUseCase(timeWindowId)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to delete time window",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Check if monitoring is currently active
     */
    fun checkMonitoringActive() {
        viewModelScope.launch {
            currentUserFlow.first { user ->
                if (user == null) return@first true

                val result = isMonitoringActiveUseCase(user.id)
                // Result can be used to update UI if needed

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
}

/**
 * Create/Edit Time Window UI State
 */
data class TimeWindowFormState(
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val startTime: String = "00:00",
    val endTime: String = "23:59",
    val isActive: Boolean = true
)