package com.example.safetysec.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.presentation.navigation.AppRoutes
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel

/**
 * Bottom Navigation Bar Component
 *
 * Automatically role-aware - fetches user role from AuthViewModel and filters navigation items.
 * For Dual users: Shows Administration tab instead of separate Alerts and Time Windows
 * For other roles: Shows role-specific navigation items
 */

/**
 * Navigation Item Data Class
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int? = null,
    val visibleForRoles: List<UserRole> = listOf(UserRole.MONITOR, UserRole.PROTECTED, UserRole.DUAL)
)

/**
 * Bottom Navigation Items List
 */
fun getBottomNavItems(): List<BottomNavItem> {
    return listOf(
        BottomNavItem(
            route = AppRoutes.DASHBOARD,
            label = "Dashboard",
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            visibleForRoles = listOf(UserRole.MONITOR, UserRole.PROTECTED, UserRole.DUAL)
        ),
        BottomNavItem(
            route = AppRoutes.ASSOCIATIONS,
            label = "Association",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            visibleForRoles = listOf(UserRole.MONITOR, UserRole.PROTECTED, UserRole.DUAL)
        ),
        BottomNavItem(
            route = AppRoutes.RULES,
            label = "Rules",
            selectedIcon = Icons.Filled.Rule,
            unselectedIcon = Icons.Outlined.Rule,
            visibleForRoles = listOf(UserRole.MONITOR, UserRole.PROTECTED, UserRole.DUAL)
        ),
        BottomNavItem(
            route = AppRoutes.ALERTS,
            label = "Alerts",
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            visibleForRoles = listOf(UserRole.MONITOR)
        ),
        BottomNavItem(
            route = AppRoutes.TIME_WINDOWS,
            label = "Time Windows",
            selectedIcon = Icons.Filled.Schedule,
            unselectedIcon = Icons.Outlined.Schedule,
            visibleForRoles = listOf(UserRole.PROTECTED)
        ),
        BottomNavItem(
            route = AppRoutes.ADMINISTRATION,
            label = "Administration",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            visibleForRoles = listOf(UserRole.DUAL)
        ),
        BottomNavItem(
            route = AppRoutes.PROFILE,
            label = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            visibleForRoles = listOf(UserRole.MONITOR, UserRole.PROTECTED, UserRole.DUAL)
        )
    )
}

/**
 * Main Bottom Navigation Bar
 *
 * Automatically fetches user role from AuthViewModel and filters navigation items accordingly.
 * No need to pass userRole - it's handled internally.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    items: List<BottomNavItem> = getBottomNavItems()
) {
    // Fetch user role from AuthViewModel
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val userRole = authState.user?.role

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Filter items based on user role if available
    val visibleItems = if (userRole != null) {
        items.filter { item ->
            item.visibleForRoles.contains(userRole)
        }
    } else {
        items
    }

    NavigationBar(
        containerColor = Color.White,
        contentColor = PrimaryPurple,
        tonalElevation = 8.dp
    ) {
        visibleItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == item.route
            } == true

            NavigationBarItem(
                icon = {
                    if (item.badgeCount != null && item.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        // Special handling for Dashboard - always clear back stack
                        if (item.route == AppRoutes.DASHBOARD) {
                            navController.navigate(item.route) {
                                popUpTo(AppRoutes.DASHBOARD) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            // For other tabs, pop to dashboard and navigate
                            navController.navigate(item.route) {
                                popUpTo(AppRoutes.DASHBOARD) {
                                    saveState = true
                                    inclusive = false
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryPurple,
                    selectedTextColor = PrimaryPurple,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = PrimaryPurple.copy(alpha = 0.1f)
                )
            )
        }
    }
}

/**
 * Bottom Navigation Bar with Custom Badge Count
 * Useful for showing alert counts
 */
@Composable
fun BottomNavigationBarWithBadges(
    navController: NavController,
    alertCount: Int = 0
) {
    val items = getBottomNavItems().map { item ->
        if (item.route == AppRoutes.ALERTS) {
            item.copy(badgeCount = alertCount)
        } else {
            item
        }
    }

    BottomNavigationBar(
        navController = navController,
        items = items
    )
}