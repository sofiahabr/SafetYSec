package com.example.safetysec.navigation

/**
 * App Navigation Routes
 *
 * Centralized location for all navigation route constants
 */
object AppRoutes {
    // Auth Routes
    const val LOGIN = "login"

    // Main Navigation Routes (Bottom Nav)
    const val DASHBOARD = "dashboard"
    const val ASSOCIATIONS = "associations"
    const val RULES = "rules"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"

    // Profile Sub-Routes
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val SETTINGS = "settings"
    const val REGISTER = "register"

    const val MONITOR = "monitor"

    // Utility Routes
    const val SHOWCASE = "showcase"
    const val HOME = "home" // Kept for backward compatibility
}