package com.example.safetysec.navigation

import androidx.compose.runtime.Composable
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

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = AppRoutes.HOME,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN,
        modifier = modifier
    ) {
        composable(AppRoutes.LOGIN) {
            LogInScreen(navController = navController)
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
    }
}