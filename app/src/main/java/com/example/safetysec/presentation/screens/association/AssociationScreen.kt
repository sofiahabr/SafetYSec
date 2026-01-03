package com.example.safetysec.presentation.screens.association

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.domain.model.Association
import com.example.safetysec.domain.model.AssociationStatus
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AssociationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AssociationScreen(
    navController: NavController,
    viewModel: AssociationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val viewState by viewModel.viewState.collectAsState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.associations)
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
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
                    .verticalScroll(rememberScrollState())
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

                // Associations List with Filter
                AssociationsListWithFilter(
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
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            // Loading Overlay
            if (viewState.isLoading) {
                LoadingDialog(
                    message = stringResource(R.string.processing)
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
                    text = stringResource(R.string.generate_otp),
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
                    text = stringResource(R.string.enter_otp),
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
                    text = stringResource(R.string.generate_otp_for_protected),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Email Input
            EmailTextField(
                value = viewState.protectedEmail,
                onValueChange = onProtectedEmailChange,
                label = stringResource(R.string.protected_user_email),
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
                            text = stringResource(R.string.generated_otp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = viewState.generatedOTP,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            letterSpacing = 4.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.share_code),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )

                        Text(
                            text = stringResource(R.string.otp_valid),
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
                text = stringResource(R.string.generate_otp),
                onClick = onGenerateOTP,
                enabled = viewState.protectedEmail.isNotBlank() && !viewState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Info Message
            if (viewState.generatedOTP == null) {
                InlineAlert(
                    message = stringResource(R.string.otp_info),
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
                    text = stringResource(R.string.accept_monitor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Monitor Email Input
            EmailTextField(
                value = viewState.monitorEmail,
                onValueChange = onMonitorEmailChange,
                label = stringResource(R.string.monitor_email),
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next
            )

            // OTP Input
            CustomTextField(
                value = viewState.otp,
                onValueChange = onOTPChange,
                label = stringResource(R.string.otp_6_digit),
                placeholder = stringResource(R.string.otp_placeholder),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = stringResource(R.string.enter_otp)
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
                text = stringResource(R.string.validate_create_association),
                onClick = onValidateOTP,
                enabled = viewState.monitorEmail.isNotBlank() &&
                        viewState.otp.length == 6 &&
                        !viewState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Info Message
            InlineAlert(
                message = stringResource(R.string.otp_accept_info),
                type = AlertType.INFO
            )
        }
    }
}

/**
 * Associations List with Status Filter
 */
@Composable
private fun AssociationsListWithFilter(
    associations: List<Association>,
    onRemove: (String) -> Unit
) {
    // Track selected filter - null means "All"
    var selectedStatus by remember { mutableStateOf(AssociationStatus.ACTIVE) }

    Column {
        // Header
        Text(
            text = stringResource(R.string.associations),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active
            FilterChip(
                selected = selectedStatus == AssociationStatus.ACTIVE,
                onClick = { selectedStatus = AssociationStatus.ACTIVE },
                label = {
                    Text(
                        text = stringResource(R.string.active),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = if (selectedStatus == AssociationStatus.ACTIVE) {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryPurple,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            // Pending
            FilterChip(
                selected = selectedStatus == AssociationStatus.PENDING,
                onClick = { selectedStatus = AssociationStatus.PENDING },
                label = {
                    Text(
                        text = stringResource(R.string.pending),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = if (selectedStatus == AssociationStatus.PENDING) {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF9800),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            // Cancelled
            FilterChip(
                selected = selectedStatus == AssociationStatus.CANCELLED,
                onClick = { selectedStatus = AssociationStatus.CANCELLED },
                label = {
                    Text(
                        text = stringResource(R.string.cancelled),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = if (selectedStatus == AssociationStatus.CANCELLED) {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF44336),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter associations based on selected status
        val filteredAssociations = associations.filter { it.status == selectedStatus }

        // Display filtered list
        AssociationsList(
            associations = filteredAssociations,
            onRemove = onRemove
        )
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
                title = stringResource(R.string.no_associations),
                message = stringResource(R.string.no_associations_desc),
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            associations.forEach { association ->
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
    val isCancelled = association.status == AssociationStatus.CANCELLED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
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
                            text = stringResource(R.string.association),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${association.id.take(8).uppercase()}",
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
                // Monitor
                DetailRow(
                    icon = Icons.Default.Shield,
                    label = stringResource(R.string.monitor),
                    value = association.monitorName.ifEmpty {
                        association.monitorEmail.ifEmpty {
                            association.monitorId.take(10) + "..."
                        }
                    }
                )

                // Protected
                DetailRow(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.protected_user),
                    value = association.protectedName.ifEmpty {
                        association.protectedEmail.ifEmpty {
                            association.protectedId.take(10) + "..."
                        }
                    }
                )

                // Created Date
                DetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.created),
                    value = dateFormat.format(association.createdAt)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remove Button - Disabled for CANCELLED associations
            if (isCancelled) {
                // Disabled button with explanation
                OutlinedButton(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.association_cancelled_btn))
                }
            } else {
                // Active remove button
                DangerButton(
                    text = stringResource(R.string.remove_association),
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.remove_association),
            message = stringResource(R.string.remove_association_confirm),
            confirmText = stringResource(R.string.remove),
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
                Text(stringResource(R.string.cancel))
            }
        }
    )
}