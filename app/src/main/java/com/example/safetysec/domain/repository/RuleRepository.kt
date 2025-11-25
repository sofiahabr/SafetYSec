package com.example.safetysec.domain.repository

import com.example.safetysec.domain.model.Rule
import com.example.safetysec.domain.model.RuleStatus
import com.example.safetysec.domain.model.TimeWindow
import kotlinx.coroutines.flow.Flow

interface RuleRepository {

    /**
     * Create a new rule (by Monitor)
     */
    suspend fun createRule(rule: Rule): Result<Rule>

    /**
     * Update an existing rule
     */
    suspend fun updateRule(rule: Rule): Result<Rule>

    /**
     * Delete a rule
     */
    suspend fun deleteRule(ruleId: String, userId: String): Result<Unit>

    /**
     * Authorize a rule (by Protected user)
     */
    suspend fun authorizeRule(ruleId: String, protectedId: String): Result<Rule>

    /**
     * Reject a rule (by Protected user)
     */
    suspend fun rejectRule(ruleId: String, protectedId: String): Result<Rule>

    /**
     * Revoke authorization for a rule (by Protected user)
     */
    suspend fun revokeRule(ruleId: String, protectedId: String): Result<Rule>

    /**
     * Get all rules created by a Monitor
     */
    fun getRulesAsMonitor(monitorId: String): Flow<List<Rule>>

    /**
     * Get all rules for a Protected user
     */
    fun getRulesAsProtected(protectedId: String): Flow<List<Rule>>

    /**
     * Get rules by status
     */
    fun getRulesByStatus(
        userId: String,
        status: RuleStatus,
        asMonitor: Boolean
    ): Flow<List<Rule>>

    /**
     * Get a specific rule by ID
     */
    suspend fun getRuleById(ruleId: String): Result<Rule>

    /**
     * Get authorized rules for a specific association
     */
    fun getAuthorizedRules(
        monitorId: String,
        protectedId: String
    ): Flow<List<Rule>>

    // Time Window methods

    /**
     * Create a time window
     */
    suspend fun createTimeWindow(timeWindow: TimeWindow): Result<TimeWindow>

    /**
     * Update a time window
     */
    suspend fun updateTimeWindow(timeWindow: TimeWindow): Result<TimeWindow>

    /**
     * Delete a time window
     */
    suspend fun deleteTimeWindow(timeWindowId: String): Result<Unit>

    /**
     * Get time windows for a Protected user
     */
    fun getTimeWindows(protectedId: String): Flow<List<TimeWindow>>

    /**
     * Get active time windows for current time
     */
    suspend fun getActiveTimeWindows(protectedId: String): Result<List<TimeWindow>>

    /**
     * Check if monitoring is currently active for a user
     */
    suspend fun isMonitoringActive(protectedId: String): Result<Boolean>
}