package com.example.safetysec.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.safetysec.presentation.screens.auth.LogInScreen
import com.example.safetysec.presentation.screens.home.HomeScreen
import com.example.safetysec.presentation.screens.profile.ProfileScreen
import com.example.safetysec.presentation.screens.showcase.ComponentsShowcaseScreen
import com.example.safetysec.presentation.screens.profile.EditProfileScreen
import com.example.safetysec.presentation.screens.profile.ChangePasswordScreen
import com.example.safetysec.presentation.screens.profile.SettingsScreen
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.screens.auth.RegistrationScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.safetysec.navigation.AppRoutes
import com.example.safetysec.presentation.screens.association.AssociationScreen
import com.example.safetysec.presentation.screens.dashboard.DashboardScreen
import com.example.safetysec.presentation.screens.rules.RulesScreen
import com.example.safetysec.presentation.screens.alerts.AlertsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Start at Dashboard instead of HOME when authenticated
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
        // Auth Screens
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

        // Main Navigation Screens (with bottom nav)
        composable(AppRoutes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }

        composable(AppRoutes.ASSOCIATIONS) {
            AssociationScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable(AppRoutes.RULES) {
            RulesScreen(navController = navController)
        }

        composable(AppRoutes.ALERTS) {
            AlertsScreen(navController = navController)
        }

        composable(AppRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }

        // Profile Sub-Screens
        composable(AppRoutes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }

        composable(AppRoutes.CHANGE_PASSWORD) {
            ChangePasswordScreen(navController = navController)
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        // Utility Screens
        composable(AppRoutes.SHOWCASE) {
            ComponentsShowcaseScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Kept for backward compatibility, but redirects to Dashboard
        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }
    }
}