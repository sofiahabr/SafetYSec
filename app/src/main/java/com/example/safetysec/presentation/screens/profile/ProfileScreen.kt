package com.example.safetysec.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.domain.model.MockUserData
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.navigation.AppRoutes
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple

/**
 * Profile Screen
 *
 * Displays user profile information and provides access to:
 * - Edit profile
 * - Change password
 * - Settings
 * - Logout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val user = MockUserData.currentUser
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Profile",
                onNavigationClick = {
                    navController.navigateUp()
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("settings")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header with Avatar
            ProfileHeader(
                name = user.name,
                email = user.email,
                isEmailVerified = user.isEmailVerified,
                initials = MockUserData.getUserInitials(user.name)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Information Card
            ProfileInfoCard(
                name = user.name,
                email = user.email,
                phone = user.phone,
                role = user.role
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Edit Profile Button
                SecondaryButton(
                    text = "Edit Profile",
                    onClick = {
                        navController.navigate("edit_profile")
                    }
                )

                // Change Password Button
                SecondaryButton(
                    text = "Change Password",
                    onClick = {
                        navController.navigate("change_password")
                    }
                )

                // Settings Button
                SecondaryButton(
                    text = "Settings",
                    onClick = {
                        navController.navigate("settings")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Logout Button
                DangerButton(
                    text = "Logout",
                    onClick = {
                        showLogoutDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                // TODO: Implement actual logout
                navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }
}

/**
 * Profile Header with Avatar
 */
@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    isEmailVerified: Boolean,
    initials: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryPurple)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with Initials
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Email with verification badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            if (isEmailVerified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Profile Information Card
 */
@Composable
private fun ProfileInfoCard(
    name: String,
    email: String,
    phone: String,
    role: UserRole
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            // Name
            ProfileInfoRow(
                icon = Icons.Default.Person,
                label = "Name",
                value = name
            )

            // Email
            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = email
            )

            // Phone
            ProfileInfoRow(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = phone
            )

            // Role
            ProfileInfoRow(
                icon = Icons.Default.Shield,
                label = "Role",
                value = MockUserData.getRoleDisplayName(role)
            )
        }
    }
}

/**
 * Profile Info Row
 */
@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryPurple,
            modifier = Modifier.size(24.dp)
        )

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Logout Confirmation Dialog
 */
@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout"
            )
        },
        title = {
            Text("Logout")
        },
        text = {
            Text("Are you sure you want to logout?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}