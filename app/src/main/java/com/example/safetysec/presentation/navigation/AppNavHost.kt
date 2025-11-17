package com.example.safetysec.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.safetysec.presentation.screens.auth.LogInScreen
import com.example.safetysec.presentation.screens.profile.ProfileScreen
import com.example.safetysec.presentation.screens.showcase.ComponentsShowcaseScreen
import com.example.safetysec.presentation.screens.profile.EditProfileScreen
import com.example.safetysec.presentation.screens.profile.ChangePasswordScreen
import com.example.safetysec.presentation.screens.profile.SettingsScreen
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.screens.auth.RegistrationScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.safetysec.presentation.screens.monitor.MonitorDashScreen
import com.example.safetysec.presentation.viewmodel.MonitorDashboardViewModel
import androidx.compose.ui.Modifier
import com.example.safetysec.navigation.AppRoutes
import com.example.safetysec.presentation.screens.association.AssociationScreen
import com.example.safetysec.presentation.screens.dashboard.DashboardScreen
// Assuming HomeScreen exists, you might need to add its import.
 import com.example.safetysec.presentation.screens.home.HomeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val monitorViewModel: MonitorDashboardViewModel = hiltViewModel()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()


    val startDestination = if (authState.isAuthenticated && authState.user != null) {
        AppRoutes.PROFILE
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

        composable(AppRoutes.MONITOR) {
            MonitorDashScreen(navController = navController, viewModel = monitorViewModel)
        }

        // Assuming you have a HomeScreen composable defined elsewhere
        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(AppRoutes.DASHBOARD) {
            MonitorDashScreen(navController = navController, viewModel = monitorViewModel)

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

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(AppRoutes.ASSOCIATIONS) {
            AssociationScreen(
                navController = navController,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable(AppRoutes.SHOWCASE) {
            ComponentsShowcaseScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
