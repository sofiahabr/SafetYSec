package com.example.safetysec.presentation.screens.association

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetysec.domain.model.Association
import com.example.safetysec.presentation.components.*
import com.example .safetysec.presentation.theme.PrimaryPurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AssociationScreen(
    viewModel: AssociationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val viewState by viewModel.viewState.collectAsState()

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Associations",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Mode Toggle Chips
                ModeToggleSection(
                    isMonitorMode = viewState.isMonitorMode,
                    onToggle = { viewModel.toggleMode() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on mode
                if (viewState.isMonitorMode) {
                    MonitorModeContent(
                        viewState = viewState,
                        onProtectedEmailChange = viewModel::updateProtectedEmail,
                        onGenerateOTP = viewModel::generateOTP
                    )
                } else {
                    ProtectedModeContent(
                        viewState = viewState,
                        onMonitorEmailChange = viewModel::updateMonitorEmail,
                        onOTPChange = viewModel::updateOTP,
                        onValidateOTP = viewModel::validateOTP
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Associations List Header
                Text(
                    text = "Active Associations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Associations List
                AssociationsList(
                    associations = viewState.associations,
                    onRemove = viewModel::removeAssociation
                )
            }

            // Error Display
            viewState.error?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ErrorAlert(
                        message = error,
                        onDismiss = { /* Clear error in viewModel if needed */ }
                    )
                }
            }

            // Loading Overlay
            if (viewState.isLoading) {
                LoadingDialog(
                    message = "Processing..."
                )
            }
        }
    }
}

/**
 * Mode Toggle Section - Switch between Monitor and Protected modes
 */
@Composable
private fun ModeToggleSection(
    isMonitorMode: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = isMonitorMode,
            onClick = { if (!isMonitorMode) onToggle() },
            label = {
                Text(
                    text = "Generate OTP",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = if (isMonitorMode) {
                {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryPurple,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )

        FilterChip(
            selected = !isMonitorMode,
            onClick = { if (isMonitorMode) onToggle() },
            label = {
                Text(
                    text = "Enter OTP",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = if (!isMonitorMode) {
                {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryPurple,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )
    }
}

/**
 * Monitor Mode Content - Generate OTP for Protected User
 */
@Composable
private fun MonitorModeContent(
    viewState: AssociationUiState,
    onProtectedEmailChange: (String) -> Unit,
    onGenerateOTP: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Generate OTP for Protected User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Email Input
            EmailTextField(
                value = viewState.protectedEmail,
                onValueChange = onProtectedEmailChange,
                label = "Protected User Email",
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (viewState.protectedEmail.isNotBlank()) {
                        onGenerateOTP()
                    }
                }
            )

            // Generated OTP Display
            if (viewState.generatedOTP != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryPurple.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Generated OTP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = viewState.generatedOTP,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            letterSpacing = 4.dp.value.toInt().sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Share this code with the protected user",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )

                        Text(
                            text = "Valid for 10 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Generate Button
            PrimaryButton(
                text = "Generate OTP",
                onClick = onGenerateOTP,
                enabled = viewState.protectedEmail.isNotBlank() && !viewState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Info Message
            if (viewState.generatedOTP == null) {
                InlineAlert(
                    message = "Enter the email of the user you want to monitor, then generate an OTP to share with them.",
                    type = AlertType.INFO
                )
            }
        }
    }
}

/**
 * Protected Mode Content - Accept Monitor Association
 */
@Composable
private fun ProtectedModeContent(
    viewState: AssociationUiState,
    onMonitorEmailChange: (String) -> Unit,
    onOTPChange: (String) -> Unit,
    onValidateOTP: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Accept Monitor Association",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Monitor Email Input
            EmailTextField(
                value = viewState.monitorEmail,
                onValueChange = onMonitorEmailChange,
                label = "Monitor Email",
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next
            )

            // OTP Input
            CustomTextField(
                value = viewState.otp,
                onValueChange = onOTPChange,
                label = "6-Digit OTP",
                placeholder = "000000",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "OTP"
                    )
                },
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (viewState.monitorEmail.isNotBlank() && viewState.otp.length == 6) {
                        onValidateOTP()
                    }
                }
            )

            // OTP Format Indicator
            if (viewState.otp.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (index < viewState.otp.length)
                                        PrimaryPurple.copy(alpha = 0.1f)
                                    else
                                        Color.Gray.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (index < viewState.otp.length)
                                            viewState.otp[index].toString()
                                        else
                                            "",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Validate Button
            PrimaryButton(
                text = "Validate & Create Association",
                onClick = onValidateOTP,
                enabled = viewState.monitorEmail.isNotBlank() &&
                        viewState.otp.length == 6 &&
                        !viewState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Info Message
            InlineAlert(
                message = "Enter the monitor's email and the OTP code they shared with you to establish the association.",
                type = AlertType.INFO
            )
        }
    }
}

/**
 * Associations List
 */
@Composable
private fun AssociationsList(
    associations: List<Association>,
    onRemove: (String) -> Unit
) {
    if (associations.isEmpty()) {
        // Empty State
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            EmptyState(
                icon = Icons.Default.PersonOff,
                title = "No Active Associations",
                message = "You don't have any active associations yet. Create an association to get started.",
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(associations, key = { it.id }) { association ->
                AssociationCard(
                    association = association,
                    onRemove = { onRemove(association.id) }
                )
            }
        }
    }
}

/**
 * Association Card - Individual association item
 */
@Composable
private fun AssociationCard(
    association: Association,
    onRemove: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Association",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${association.id.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Status Badge
                AssociationStatusBadge(status = association.status.name)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Association Details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monitor ID
                DetailRow(
                    icon = Icons.Default.Shield,
                    label = "Monitor",
                    value = association.monitorId.take(10) + "..."
                )

                // Protected ID
                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Protected",
                    value = association.protectedId.take(10) + "..."
                )

                // Created Date
                DetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Created",
                    value = dateFormat.format(association.createdAt)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remove Button
            DangerButton(
                text = "Remove Association",
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Remove Association",
            message = "Are you sure you want to remove this association? This action cannot be undone.",
            confirmText = "Remove",
            onConfirm = {
                onRemove()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            isDangerous = true
        )
    }
}

/**
 * Detail Row - Icon, Label, and Value
 */
@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Status Badge
 */
@Composable
private fun AssociationStatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "ACTIVE" -> PrimaryPurple.copy(alpha = 0.1f) to PrimaryPurple
        "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.1f) to Color(0xFFFF9800)
        "CANCELLED" -> Color(0xFFF44336).copy(alpha = 0.1f) to Color(0xFFF44336)
        else -> Color.Gray.copy(alpha = 0.1f) to Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Confirmation Dialog Component
 */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDangerous: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDangerous)
                        MaterialTheme.colorScheme.error
                    else
                        PrimaryPurple
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}