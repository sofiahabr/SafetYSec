package com.example.safetysec.presentation.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.navigation.AppRoutes
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetysec.presentation.components.*

@Composable
fun DashboardScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "🎨 View Components Showcase",
            onClick = {
                navController.navigate(AppRoutes.SHOWCASE)
            }
        )
    }
}