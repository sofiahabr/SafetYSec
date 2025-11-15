package com.example.safetysec.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.domain.model.MockUserData
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.presentation.components.*

/**
 * Edit Profile Screen
 *
 * Allows users to edit their profile information:
 * - Name
 * - Email
 * - Phone
 * - Role (Monitor/Protected/Both)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val user = MockUserData.currentUser

    // Form state
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var selectedRole by remember { mutableStateOf(user.role) }

    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // UI state
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Edit Profile",
                onNavigationClick = {
                    navController.navigateUp()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Alert
            if (showSuccessAlert) {
                SuccessAlert(
                    message = "Profile updated successfully!",
                    onDismiss = {
                        showSuccessAlert = false
                    }
                )
            }

            // Name Field
            CustomTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = "Full Name",
                placeholder = "Enter your full name",
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                isError = nameError != null,
                errorMessage = nameError,
                imeAction = ImeAction.Next
            )

            // Email Field
            EmailTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                isError = emailError != null,
                errorMessage = emailError,
                imeAction = ImeAction.Next
            )

            // Phone Field
            PhoneTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                },
                isError = phoneError != null,
                errorMessage = phoneError,
                imeAction = ImeAction.Done
            )

            // Role Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showRoleDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Role",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MockUserData.getRoleDisplayName(selectedRole),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select role"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            PrimaryButton(
                text = "Save Changes",
                onClick = {
                    // Validate
                    var isValid = true

                    if (name.isBlank()) {
                        nameError = "Name is required"
                        isValid = false
                    }

                    if (email.isBlank() || !isValidEmail(email)) {
                        emailError = "Valid email is required"
                        isValid = false
                    }

                    if (phone.isBlank()) {
                        phoneError = "Phone number is required"
                        isValid = false
                    }

                    if (isValid) {
                        // Save changes
                        isSaving = true

                        // Simulate saving (replace with Firebase later)
                        MockUserData.updateUser(
                            name = name,
                            email = email,
                            phone = phone,
                            role = selectedRole
                        )

                        // Show success
                        isSaving = false
                        showSuccessAlert = true
                    }
                },
                isLoading = isSaving
            )

            // Cancel Button
            CustomTextButton(
                text = "Cancel",
                onClick = {
                    navController.navigateUp()
                }
            )
        }
    }

    // Role Selection Dialog
    if (showRoleDialog) {
        RoleSelectionDialog(
            currentRole = selectedRole,
            onRoleSelected = { role ->
                selectedRole = role
                showRoleDialog = false
            },
            onDismiss = {
                showRoleDialog = false
            }
        )
    }
}

/**
 * Role Selection Dialog
 */
@Composable
private fun RoleSelectionDialog(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Shield, contentDescription = null)
        },
        title = {
            Text("Select Your Role")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.values().forEach { role ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onRoleSelected(role) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (role == currentRole) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = MockUserData.getRoleDisplayName(role),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = getRoleDescription(role),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (role == currentRole) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Get role description
 */
private fun getRoleDescription(role: UserRole): String {
    return when (role) {
        UserRole.MONITOR -> "Monitor and protect others"
        UserRole.PROTECTED -> "Receive protection from monitors"
        UserRole.BOTH -> "Both monitor others and receive protection"
    }
}

/**
 * Validate email format
 */
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}