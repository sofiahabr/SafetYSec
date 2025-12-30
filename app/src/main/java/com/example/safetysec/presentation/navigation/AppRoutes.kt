package com.example.safetysec.presentation.navigation

/**
 * App Navigation Routes
 *
 * Centralized location for all navigation route constants
 */
object AppRoutes {
    // Auth Routes
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main Navigation Routes (Bottom Nav)
    const val DASHBOARD = "dashboard"
    const val ASSOCIATIONS = "associations"
    const val RULES = "rules"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"

    // Alert Sub-Routes
    const val ALERT_DETAIL = "alert_detail/{alertId}"

    // Rules Sub-Routes
    const val CREATE_RULE = "create_rule"
    const val EDIT_RULE = "edit_rule/{ruleId}"
    const val RULE_DETAILS = "rule_details/{ruleId}"
    const val CHANGE_CANCELLATION_PIN = "change_cancellation_pin"

    // Time Windows Sub-Routes
    const val TIME_WINDOWS = "time_windows"
    const val CREATE_TIME_WINDOW = "create_time_window"
    const val EDIT_TIME_WINDOW = "edit_time_window/{windowId}"

    // Profile Sub-Routes
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val SETTINGS = "settings"

    // Dashboard Routes
    const val MONITOR = "monitor"
    const val PROTECTED_DASHBOARD = "protected_dashboard"

    // Monitoring Routes
    const val MONITORING_CONTROL = "monitoring_control"

    // Utility Routes
    const val SHOWCASE = "showcase"
    const val HOME = "home"

    // Helper functions for parameterized routes
    fun alertDetail(alertId: String) = "alert_detail/$alertId"
    fun editRule(ruleId: String) = "edit_rule/$ruleId"
    fun ruleDetails(ruleId: String) = "rule_details/$ruleId"
    fun editTimeWindow(windowId: String) = "edit_time_window/$windowId"
}