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
import com.example.safetysec.presentation.screens.auth.ForgotPasswordScreen
import com.example.safetysec.presentation.screens.auth.PasswordRecoverySuccessScreen
import com.example.safetysec.presentation.screens.auth.MFASetupScreen
import com.example.safetysec.presentation.screens.auth.MFAVerificationScreen
import com.example.safetysec.presentation.screens.dashboard.DashboardScreen
import com.example.safetysec.presentation.screens.home.HomeScreen
import com.example.safetysec.presentation.screens.monitor.MonitorDashScreen
import com.example.safetysec.presentation.screens.profile.ChangePasswordScreen
import com.example.safetysec.presentation.screens.profile.EditProfileScreen
import com.example.safetysec.presentation.screens.profile.ProfileScreen
import com.example.safetysec.presentation.screens.profile.SettingsScreen
import com.example.safetysec.presentation.screens.protectedUser.ProtectedDashboardScreen
import com.example.safetysec.presentation.screens.rules.CreateRuleScreen
import com.example.safetysec.presentation.screens.rules.EditRuleScreen
import com.example.safetysec.presentation.screens.rules.RulesScreen
import com.example.safetysec.presentation.screens.showcase.ComponentsShowcaseScreen
import com.example.safetysec.presentation.screens.timewindows.TimeWindowsScreen
import com.example.safetysec.presentation.protectedUser.MonitoringControlScreen
import com.example.safetysec.presentation.screens.alerts.AlertDetailScreen
import com.example.safetysec.presentation.screens.alerts.AlertsScreen
import com.example.safetysec.presentation.screens.administration.AdministrationScreen
import com.example.safetysec.presentation.screens.profile.ChangeCancellationPinScreen
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
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AppRoutes.FORGOT_PASSWORD)
                },
                navController = navController
            )
        }

        composable(AppRoutes.REGISTER) {
            RegistrationScreen(
                viewModel = authViewModel,
                onRegistrationSuccess = {
                },
                navController = navController
            )
        }

        composable(AppRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        composable(AppRoutes.PASSWORD_RECOVERY_SUCCESS) {
            PasswordRecoverySuccessScreen(
                navController = navController
            )
        }

        composable(AppRoutes.MFA_SETUP) {
            MFASetupScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        composable(AppRoutes.MFA_VERIFICATION) {
            MFAVerificationScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        composable(AppRoutes.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(AppRoutes.MONITOR) {
            MonitorDashScreen(
                navController = navController,
                viewModel = monitorViewModel
            )
        }

        composable(AppRoutes.PROTECTED_DASHBOARD) {
            ProtectedDashboardScreen(navController = navController)
        }

        composable(AppRoutes.MONITORING_CONTROL) {
            MonitoringControlScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

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
            AlertsScreen(navController = navController)
        }

        composable(AppRoutes.ADMINISTRATION) {
            AdministrationScreen(navController = navController)
        }

        composable(AppRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }

        composable(AppRoutes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }

        composable(AppRoutes.CHANGE_PASSWORD) {
            ChangePasswordScreen(navController = navController)
        }

        composable(AppRoutes.CHANGE_CANCELLATION_PIN) {
            ChangeCancellationPinScreen(navController = navController)
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(AppRoutes.CREATE_RULE) {
            CreateRuleScreen(navController = navController)
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

        composable(AppRoutes.TIME_WINDOWS) {
            TimeWindowsScreen(navController = navController)
        }

        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(AppRoutes.SHOWCASE) {
            ComponentsShowcaseScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = AppRoutes.ALERT_DETAIL,
            arguments = listOf(
                navArgument("alertId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            AlertDetailScreen(
                alertId = alertId,
                navController = navController
            )
        }
    }
}