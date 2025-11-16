package com.example.safetysec.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.navigation.AppRoutes
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()

    Column (
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Home Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.navigate(AppRoutes.SHOWCASE)
        }) {
            Text("🎨 View Components Showcase")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            navController.navigate(AppRoutes.PROFILE)
        }) {
            Text("Go to profile")
        }
        Button(onClick = {
            navController.navigate(AppRoutes.LOGIN) {
                popUpTo(AppRoutes.HOME) { inclusive = true }
            }
        }) {
            Text("Go to login")
        }

        Button(onClick = {
            viewModel.logout()
            navController.navigate(AppRoutes.LOGIN) {
                popUpTo(AppRoutes.HOME) { inclusive = true }
            }
        }) {
            Text("Logout")
        }
    }
}
