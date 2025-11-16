package com.example.safetysec.presentation.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safetysec.presentation.components.CustomTextField
import com.example.safetysec.presentation.components.EmailTextField
import com.example.safetysec.presentation.components.PasswordTextField
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.viewmodel.AuthViewModel

@Composable
fun RegistrationScreen(
    viewModel: AuthViewModel,
    onRegistrationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MONITOR") }

    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && authState.user != null) {
            onRegistrationSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        EmailTextField(
            value = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = name,
            onValueChange = { name = it },
            label = "Full Name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = password,
            onValueChange = { password = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Select Role",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Monitor Role
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedRole == "MONITOR",
                onClick = { selectedRole = "MONITOR" }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Monitor", style = MaterialTheme.typography.titleSmall)
                Text(
                    "I want to monitor others",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Protected Role
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedRole == "PROTECTED",
                onClick = { selectedRole = "PROTECTED" }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Protected", style = MaterialTheme.typography.titleSmall)
                Text(
                    "I want to be monitored",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Dual Role
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedRole == "DUAL",
                onClick = { selectedRole = "DUAL" }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Both", style = MaterialTheme.typography.titleSmall)
                Text(
                    "I want both roles",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Register",
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                    viewModel.register(email, password, name, selectedRole)
                }
            },
            isLoading = authState.isLoading
        )

        if (!authState.error.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                authState.error ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}