package com.example.safetysec.domain.usecase.rules

import com.example.safetysec.domain.model.Rule
import com.example.safetysec.domain.repository.RuleRepository
import javax.inject.Inject

/**
 * Create Rule Use Case (Monitor)
 */
class CreateRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(rule: Rule): Result<Rule> {
        // Validate rule parameters based on type
        if (!rule.type.requiresParameters()) {
            return ruleRepository.createRule(rule)
        }

        // Validate specific parameters
        val validationError = when (rule.type) {
            com.example.safetysec.domain.model.RuleType.GEOFENCING -> {
                if (rule.parameters.geofenceAreas.isEmpty()) {
                    "At least one geofence area is required"
                } else null
            }
            com.example.safetysec.domain.model.RuleType.SPEED_CONTROL -> {
                if (rule.parameters.maxSpeed <= 0) {
                    "Maximum speed must be greater than 0"
                } else null
            }
            com.example.safetysec.domain.model.RuleType.PROLONGED_INACTIVITY -> {
                if (rule.parameters.inactivityDuration <= 0) {
                    "Inactivity duration must be greater than 0"
                } else null
            }
            else -> null
        }

        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        return ruleRepository.createRule(rule)
    }
}

/**
 * Update Rule Use Case (Monitor)
 */
class UpdateRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(rule: Rule): Result<Rule> {
        return ruleRepository.updateRule(rule)
    }
}

/**
 * Delete Rule Use Case
 */
class DeleteRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(ruleId: String, userId: String): Result<Unit> {
        return ruleRepository.deleteRule(ruleId, userId)
    }
}

/**
 * Authorize Rule Use Case (Protected)
 */
class AuthorizeRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(ruleId: String, protectedId: String): Result<Rule> {
        return ruleRepository.authorizeRule(ruleId, protectedId)
    }
}

/**
 * Reject Rule Use Case (Protected)
 */
class RejectRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(ruleId: String, protectedId: String): Result<Rule> {
        return ruleRepository.rejectRule(ruleId, protectedId)
    }
}

/**
 * Revoke Rule Use Case (Protected)
 */
class RevokeRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(ruleId: String, protectedId: String): Result<Rule> {
        return ruleRepository.revokeRule(ruleId, protectedId)
    }
}

/**
 * Get Rules Use Case
 */
class GetRulesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    fun asMonitor(monitorId: String) = ruleRepository.getRulesAsMonitor(monitorId)

    fun asProtected(protectedId: String) = ruleRepository.getRulesAsProtected(protectedId)

    fun byStatus(
        userId: String,
        status: com.example.safetysec.domain.model.RuleStatus,
        asMonitor: Boolean
    ) = ruleRepository.getRulesByStatus(userId, status, asMonitor)
}

/**
 * Get Authorized Rules Use Case
 */
class GetAuthorizedRulesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    operator fun invoke(monitorId: String, protectedId: String) =
        ruleRepository.getAuthorizedRules(monitorId, protectedId)
}