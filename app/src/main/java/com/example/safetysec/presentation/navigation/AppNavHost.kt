package com.example.safetysec.navigation

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
import com.example.safetysec.presentation.screens.showcase.ComponentsShowcaseScreen
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.screens.auth.RegistrationScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.safetysec.presentation.screens.monitor.MonitorDashScreen
import com.example.safetysec.presentation.viewmodel.MonitorDashboardViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val monitorViewModel: MonitorDashboardViewModel = hiltViewModel()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val startDestination = if (authState.isAuthenticated && authState.user != null) {
        AppRoutes.HOME
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
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppRoutes.REGISTER)
                }
            )
        }

        composable(AppRoutes.MONITOR) {
            MonitorDashScreen(navController = navController, viewModel = monitorViewModel)
        }
        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(AppRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(AppRoutes.SHOWCASE) {
            ComponentsShowcaseScreen(
                onNavigateBack = {
                    navController.navigate(AppRoutes.LOGIN)
                }
            )
        }
        composable(AppRoutes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }
        composable(AppRoutes.CHANGE_PASSWORD) {
            ChangePasswordScreen(navController = navController)
        }
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(AppRoutes.SHOWCASE) {
            ComponentsShowcaseScreen(
                onNavigateBack = {
                    navController.navigate(AppRoutes.LOGIN)
                }
            )
        }
        composable(AppRoutes.REGISTER) {
            RegistrationScreen(
                viewModel = authViewModel,
                onRegistrationSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
    }
}