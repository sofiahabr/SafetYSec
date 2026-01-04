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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.domain.model.MockUserData
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.presentation.navigation.AppRoutes
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    val user = authState.user

    if (user == null) {
        FullScreenLoading(message = stringResource(R.string.loading_profile))
        return
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    val isProtected = user.role == UserRole.PROTECTED || user.role == UserRole.DUAL

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.profile),
                actions = {
                    IconButton(onClick = {
                        navController.navigate("settings")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                name = user.name,
                email = user.email,
                isEmailVerified = user.isEmailVerified,
                initials = MockUserData.getUserInitials(user.name)
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfileInfoCard(
                name = user.name,
                email = user.email,
                phone = user.phone,
                role = user.role
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.edit_profile),
                    onClick = {
                        navController.navigate("edit_profile")
                    }
                )

                SecondaryButton(
                    text = stringResource(R.string.change_password),
                    onClick = {
                        navController.navigate("change_password")
                    }
                )

                if (isProtected) {
                    SecondaryButton(
                        text = stringResource(R.string.alert_cancellation_pin),
                        onClick = {
                            navController.navigate(AppRoutes.CHANGE_CANCELLATION_PIN)
                        }
                    )
                }

                SecondaryButton(
                    text = stringResource(R.string.settings),
                    onClick = {
                        navController.navigate("settings")
                    }
                )

                DangerButton(
                    text = stringResource(R.string.logout),
                    onClick = {
                        showLogoutDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                authViewModel.logout()
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

        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

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
                    contentDescription = stringResource(R.string.verified),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

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
                text = stringResource(R.string.account_information),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            ProfileInfoRow(
                icon = Icons.Default.Person,
                label = stringResource(R.string.name),
                value = name
            )

            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = stringResource(R.string.email),
                value = email
            )

            ProfileInfoRow(
                icon = Icons.Default.Phone,
                label = stringResource(R.string.phone),
                value = phone
            )

            ProfileInfoRow(
                icon = Icons.Default.Shield,
                label = stringResource(R.string.role),
                value = MockUserData.getRoleDisplayName(role)
            )
        }
    }
}

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
                contentDescription = stringResource(R.string.logout)
            )
        },
        title = {
            Text(stringResource(R.string.logout))
        },
        text = {
            Text(stringResource(R.string.confirm_logout))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.logout))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}