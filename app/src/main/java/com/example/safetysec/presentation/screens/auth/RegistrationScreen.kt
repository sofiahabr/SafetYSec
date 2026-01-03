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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safetysec.R
import com.example.safetysec.presentation.components.CustomTextField
import com.example.safetysec.presentation.components.PhoneTextField
import com.example.safetysec.presentation.components.EmailTextField
import com.example.safetysec.presentation.components.InfoAlert
import com.example.safetysec.presentation.components.PasswordTextField
import com.example.safetysec.presentation.components.PrimaryButton
import com.example.safetysec.presentation.screens.profile.PasswordStrengthIndicator
import com.example.safetysec.presentation.screens.profile.calculatePasswordStrength
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
    var phone by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MONITOR") }

    // Error states
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val passwordMismatchErrorText = stringResource(R.string.password_error_mismatch)
    val weakPasswordErrorText = stringResource(R.string.password_error_weak)
    val weakPasswordLabel = stringResource(R.string.password_strength_weak)

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
            stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        InfoAlert(
            title = stringResource(R.string.password_requirements),
            message = stringResource(R.string.password_requirements_desc)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email
        EmailTextField(
            value = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.full_name)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PhoneTextField(
            value = phone,
            onValueChange = { phone = it },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        PasswordTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = stringResource(R.string.password),
            isError = passwordError != null,
            errorMessage = passwordError,
            imeAction = ImeAction.Next
        )

        // Password Strength Indicator
        if (password.isNotEmpty()) {
            PasswordStrengthIndicator(password = password)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password
        PasswordTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmPasswordError = null
            },
            label = stringResource(R.string.confirm_password),
            isError = confirmPasswordError != null,
            errorMessage = confirmPasswordError,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            stringResource(R.string.select_role),
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
                Text(stringResource(R.string.monitor), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.role_monitor_desc),
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
                Text(stringResource(R.string.protected_user), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.role_protected_desc),
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
                Text(stringResource(R.string.dual), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.role_dual_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = stringResource(R.string.register),
            onClick = {
                var isValid = true

                if (password != confirmPassword) {
                    confirmPasswordError = passwordMismatchErrorText
                    isValid = false
                }

                if (calculatePasswordStrength(password).label == weakPasswordLabel) {
                    passwordError = weakPasswordErrorText
                    isValid = false
                }

                if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && phone.isNotEmpty() && isValid) {
                    viewModel.register(email, password, name, phone, selectedRole)
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