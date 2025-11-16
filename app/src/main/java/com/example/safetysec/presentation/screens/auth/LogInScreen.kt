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
import androidx.compose.ui.unit.dp
import com.example.safetysec.presentation.components.EmailTextField
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.components.PasswordTextField
import com.example.safetysec.presentation.components.SecondaryButton
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.viewmodel.AuthState
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LogInScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email = remember { mutableStateOf("") }
    var password = remember { mutableStateOf("") }

    // Create state directly - don't use collectAsStateWithLifecycle
    val authState = remember { mutableStateOf(AuthState()) }

    LaunchedEffect(Unit) {
        viewModel.authState.collect { state ->
            println("LogInScreen collecting fresh state: isAuthenticated=${state.isAuthenticated}, user=${state.user}")
            authState.value = state

            if (state.isAuthenticated && state.user != null) {
                println("Navigating to HOME")
                onLoginSuccess()
            }
        }
    }

    println("LogInScreen rendering: isAuthenticated=${authState.value.isAuthenticated}, user=${authState.value.user}")

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
            onClick = { viewModel.login(email.value, password.value) },
            isLoading = authState.value.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryButton(
            text = "Don't have an account? Register here",
            onClick = onNavigateToRegister,
            isLoading = authState.value.isLoading
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