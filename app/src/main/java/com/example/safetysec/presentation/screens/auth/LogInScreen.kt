package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.safetysec.presentation.components.EmailTextField
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.components.PasswordTextField
import com.example.safetysec.presentation.components.SecondaryButton
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.presentation.navigation.AppRoutes
import android.app.Activity

@Composable
fun LogInScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    // ✅ KEY FIX: Get the Activity from context for MFA
    val context = LocalContext.current
    val activity = context as? Activity

    val authState = viewModel.authState.collectAsState()

    // Handle login success - check if MFA is required
    LaunchedEffect(authState.value.mfaRequired, authState.value.isAuthenticated) {
        if (authState.value.mfaRequired) {
            // Navigate to MFA verification
            navController.navigate(AppRoutes.MFA_VERIFICATION)
        } else if (authState.value.isAuthenticated && authState.value.user != null) {
            // No MFA needed, go to dashboard
            onLoginSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Login",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        EmailTextField(
            value = email.value,
            onValueChange = { email.value = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = password.value,
            onValueChange = { password.value = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = "Login",
            onClick = {
                // ✅ KEY FIX: Pass the Activity to the login function for MFA support
                viewModel.login(email.value, password.value, activity)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !authState.value.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryButton(
            text = "Don't have an account? Register here",
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        SecondaryButton(
            text = "Forgot Password?",
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.fillMaxWidth()
        )

        if (!authState.value.error.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                authState.value.error ?: "Unknown error",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}