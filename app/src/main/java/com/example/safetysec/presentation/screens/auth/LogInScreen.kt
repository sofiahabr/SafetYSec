package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.safetysec.navigation.AppRoutes

@Composable
fun LogInScreen(navController: NavController) {
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }

        val authState by viewModel.authState.colectAsStateWithLifecycle

        LaunchedEffect(authState.isAuthenticated) {
            if (authState.isAuthenticated && authState.user != null) {
                onLoginSuccess()
            }
        }

        LaunchedEffect(authState.error) {
            if (authState.error != null) {
                snackbarHostState.showSnackbar(authState.error ?: "Unknown error")
                viewModel.clearError()
            }
        }

        Column(
            modifier = modifier
                .filMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))


        }
//    Column (
//
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text("Login Screen")
//        Button(onClick = {
//            navController.navigate(AppRoutes.HOME)
//        }) {
//            Text("Go home")
//        }
//    }
}
