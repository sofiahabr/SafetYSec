package com.example.safetysec.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.safetysec.presentation.screens.association.AssociationScreen
import com.example.safetysec.presentation.screens.auth.LogInScreen
import com.example.safetysec.presentation.screens.auth.RegistrationScreen
import com.example.safetysec.presentation.screens.dashboard.DashboardScreen
import com.example.safetysec.presentation.screens.home.HomeScreen
import com.example.safetysec.presentation.screens.monitor.MonitorDashScreen
import com.example.safetysec.presentation.screens.profile.ChangePasswordScreen
import com.example.safetysec.presentation.screens.profile.EditProfileScreen
import com.example.safetysec.presentation.screens.profile.ProfileScreen
import com.example.safetysec.presentation.screens.profile.SettingsScreen
import com.example.safetysec.presentation.screens.protected.ProtectedDashboardScreen
import com.example.safetysec.presentation.screens.rules.CreateRuleScreen
import com.example.safetysec.presentation.screens.rules.EditRuleScreen
import com.example.safetysec.presentation.screens.rules.RulesScreen
import com.example.safetysec.presentation.screens.showcase.ComponentsShowcaseScreen
import com.example.safetysec.presentation.screens.timewindows.TimeWindowsScreen
import com.example.safetysec.presentation.protectedUser.MonitoringControlScreen
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.viewmodel.MonitorDashboardViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val monitorViewModel: MonitorDashboardViewModel = hiltViewModel()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val startDestination = if (authState.isAuthenticated && authState.user != null) {
        AppRoutes.DASHBOARD
    } else {
        AppRoutes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth screens
        composable(AppRoutes.LOGIN) {
            LogInScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(AppRoutes.DASHBOARD) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppRoutes.REGISTER)
                }
            )
        }

        composable(AppRoutes.REGISTER) {
            RegistrationScreen(
                viewModel = authViewModel,
                onRegistrationSuccess = {
                    navController.navigate(AppRoutes.DASHBOARD) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // Main Dashboard - Smart routing based on user role
        composable(AppRoutes.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // Role-specific dashboards (can be accessed directly)
        composable(AppRoutes.MONITOR) {
            MonitorDashScreen(
                navController = navController,
                viewModel = monitorViewModel
            )
        }

        composable(AppRoutes.PROTECTED_DASHBOARD) {
            ProtectedDashboardScreen(navController = navController)
        }

        // Monitoring Control Screen (NEW)
        composable(AppRoutes.MONITORING_CONTROL) {
            MonitoringControlScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Main screens (with bottom navigation)
        composable(AppRoutes.ASSOCIATIONS) {
            AssociationScreen(
                navController = navController,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(AppRoutes.RULES) {
            RulesScreen(navController = navController)
        }

        composable(AppRoutes.ALERTS) {
            // TODO: Implement AlertsScreen
            HomeScreen(navController = navController)
        }

        composable(AppRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }

        // Profile sub-screens
        composable(AppRoutes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }

        composable(AppRoutes.CHANGE_PASSWORD) {
            ChangePasswordScreen(navController = navController)
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        // Rules management screens
        composable(AppRoutes.CREATE_RULE) {
            CreateRuleScreen(navController = navController)
        }

        // Time windows screen
        composable(AppRoutes.TIME_WINDOWS) {
            TimeWindowsScreen(navController = navController)
        }

        // Other screens
        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(AppRoutes.SHOWCASE) {
            ComponentsShowcaseScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = "edit_rule/{ruleId}",
            arguments = listOf(navArgument("ruleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId") ?: ""
            EditRuleScreen(
                ruleId = ruleId,
                navController = navController
            )
        }
    }
}