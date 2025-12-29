package com.example.safetysec.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.*
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import com.example.safetysec.domain.usecase.rules.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rules UI State
 */
data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val filteredRules: List<Rule> = emptyList(),
    val selectedFilter: RuleStatus? = RuleStatus.AUTHORIZED,
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMonitor: Boolean = true
)

/**
 * Rules ViewModel
 *
 * Manages the state for Rules screen (both Monitor and Protected views)
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val getRulesUseCase: GetRulesUseCase,
    private val createRuleUseCase: CreateRuleUseCase,
    private val updateRuleUseCase: UpdateRuleUseCase,
    private val deleteRuleUseCase: DeleteRuleUseCase,
    private val authorizeRuleUseCase: AuthorizeRuleUseCase,
    private val rejectRuleUseCase: RejectRuleUseCase,
    private val revokeRuleUseCase: RevokeRuleUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    private val currentUserFlow = getCurrentUserUseCase()

    init {
        loadRules()
    }

    /**
     * Load rules based on user role
     */
    private fun loadRules() {
        viewModelScope.launch {
            currentUserFlow.collectLatest { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@collectLatest
                }

                // Determine if user is viewing as monitor or protected
                val isMonitor = user.role == UserRole.MONITOR || user.role == UserRole.DUAL

                _uiState.update { it.copy(isMonitor = isMonitor, isLoading = true) }

                // Load rules based on role
                if (isMonitor) {
                    getRulesUseCase.asMonitor(user.id)
                        .catch { exception ->
                            _uiState.update {
                                it.copy(
                                    error = exception.message ?: "Failed to load rules",
                                    isLoading = false
                                )
                            }
                        }
                        .collect { rules ->
                            val pendingCount = rules.count { it.status == RuleStatus.PENDING }
                            _uiState.update {
                                it.copy(
                                    rules = rules,
                                    filteredRules = filterRules(rules, it.selectedFilter),
                                    pendingCount = pendingCount,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                } else {
                    getRulesUseCase.asProtected(user.id)
                        .catch { exception ->
                            _uiState.update {
                                it.copy(
                                    error = exception.message ?: "Failed to load rules",
                                    isLoading = false
                                )
                            }
                        }
                        .collect { rules ->
                            val pendingCount = rules.count { it.status == RuleStatus.PENDING }
                            _uiState.update {
                                it.copy(
                                    rules = rules,
                                    filteredRules = filterRules(rules, it.selectedFilter),
                                    pendingCount = pendingCount,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                }
            }
        }
    }

    /**
     * Filter rules by status
     */
    fun filterByStatus(status: RuleStatus?) {
        _uiState.update {
            it.copy(
                selectedFilter = status,
                filteredRules = filterRules(it.rules, status)
            )
        }
    }

    private fun filterRules(rules: List<Rule>, status: RuleStatus?): List<Rule> {
        // Always filter by status (default to AUTHORIZED if null)
        val filterStatus = status ?: RuleStatus.AUTHORIZED
        return rules.filter { it.status == filterStatus }
    }

    /**
     * Create a new rule (Monitor only)
     */
    fun createRule(rule: Rule) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = createRuleUseCase(rule)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false)
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
        }
    }

    /**
     * Update a rule (Monitor only)
     */
    fun updateRule(rule: Rule) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = updateRuleUseCase(rule)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false)
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

    /**
     * Delete a rule
     */
    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            currentUserFlow.first { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@first true
                }

                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = deleteRuleUseCase(ruleId, user.id)

                result.fold(
                    onSuccess = {
                        // Optimistically remove the rule from UI immediately
                        _uiState.update { currentState ->
                            val updatedRules = currentState.rules.filter { it.id != ruleId }
                            currentState.copy(
                                rules = updatedRules,
                                filteredRules = filterRules(updatedRules, currentState.selectedFilter),
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                error = exception.message ?: "Failed to delete rule",
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
     * Authorize a rule (Protected only)
     */
    fun authorizeRule(ruleId: String) {
        viewModelScope.launch {
            currentUserFlow.first { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@first true
                }

                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = authorizeRuleUseCase(ruleId, user.id)

                result.fold(
                    onSuccess = { updatedRule ->
                        // Optimistically update the rule in UI
                        _uiState.update { currentState ->
                            val updatedRules = currentState.rules.map { rule ->
                                if (rule.id == ruleId) updatedRule else rule
                            }
                            currentState.copy(
                                rules = updatedRules,
                                filteredRules = filterRules(updatedRules, currentState.selectedFilter),
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                error = exception.message ?: "Failed to authorize rule",
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
     * Reject a rule (Protected only)
     */
    fun rejectRule(ruleId: String) {
        viewModelScope.launch {
            currentUserFlow.first { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@first true
                }

                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = rejectRuleUseCase(ruleId, user.id)

                result.fold(
                    onSuccess = { updatedRule ->
                        // Optimistically update the rule in UI
                        _uiState.update { currentState ->
                            val updatedRules = currentState.rules.map { rule ->
                                if (rule.id == ruleId) updatedRule else rule
                            }
                            currentState.copy(
                                rules = updatedRules,
                                filteredRules = filterRules(updatedRules, currentState.selectedFilter),
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                error = exception.message ?: "Failed to reject rule",
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
     * Revoke a rule (Protected only)
     */
    fun revokeRule(ruleId: String) {
        viewModelScope.launch {
            currentUserFlow.first { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(error = "User not authenticated")
                    }
                    return@first true
                }

                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = revokeRuleUseCase(ruleId, user.id)

                result.fold(
                    onSuccess = { updatedRule ->
                        // Optimistically update the rule in UI
                        _uiState.update { currentState ->
                            val updatedRules = currentState.rules.map { rule ->
                                if (rule.id == ruleId) updatedRule else rule
                            }
                            currentState.copy(
                                rules = updatedRules,
                                filteredRules = filterRules(updatedRules, currentState.selectedFilter),
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                error = exception.message ?: "Failed to revoke rule",
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
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}