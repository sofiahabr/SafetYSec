package com.example.safetysec.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Change Cancellation PIN Screen
 *
 * Allows Protected users to set or change their alert cancellation PIN.
 * This PIN is used to cancel alerts within the 10-second window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCancellationPinScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showCurrentPin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }

    // Get the actual PIN value
    val actualPin = authState.user?.alertCancellationCode

    // Check if user has an existing PIN - using multiple checks
    val hasExistingPin = remember(actualPin) {
        when {
            actualPin == null -> {
                android.util.Log.d("ChangePinScreen", "PIN is NULL")
                false
            }
            actualPin.isEmpty() -> {
                android.util.Log.d("ChangePinScreen", "PIN is EMPTY STRING")
                false
            }
            actualPin.isBlank() -> {
                android.util.Log.d("ChangePinScreen", "PIN is BLANK")
                false
            }
            else -> {
                android.util.Log.d("ChangePinScreen", "PIN exists: length=${actualPin.length}")
                true
            }
        }
    }

    // Log the user state
    LaunchedEffect(authState.user) {
        android.util.Log.d("ChangePinScreen", """
            User loaded:
            - User is null: ${authState.user == null}
            - PIN value: '$actualPin'
            - PIN is null: ${actualPin == null}
            - PIN isEmpty: ${actualPin?.isEmpty()}
            - PIN isBlank: ${actualPin?.isBlank()}
            - hasExistingPin: $hasExistingPin
        """.trimIndent())
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = if (hasExistingPin) "Change Cancellation PIN" else "Set Cancellation PIN",
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    // Debug toggle button
                    IconButton(onClick = { showDebug = !showDebug }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Toggle Debug Info"
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DEBUG INFO CARD
            if (showDebug) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Yellow.copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🐛 DEBUG INFO",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("User loaded: ${authState.user != null}", style = MaterialTheme.typography.bodySmall)
                        Text("PIN value: '${actualPin ?: "NULL"}'", style = MaterialTheme.typography.bodySmall)
                        Text("PIN is null: ${actualPin == null}", style = MaterialTheme.typography.bodySmall)
                        Text("PIN isEmpty: ${actualPin?.isEmpty() ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        Text("PIN length: ${actualPin?.length ?: 0}", style = MaterialTheme.typography.bodySmall)
                        Text("hasExistingPin: $hasExistingPin", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (hasExistingPin && actualPin != null && actualPin.length != 4) {
                            Text(
                                text = "⚠️ WARNING: PIN is ${actualPin.length} digits, not 4!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryPurple
                    )
                    Column {
                        Text(
                            text = "About Cancellation PIN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your 4-digit PIN is used to cancel alerts within the 10-second window. Keep it secure and memorable!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // First-time user notice OR Old PIN warning
            if (!hasExistingPin) {
                // Show for users with no PIN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2196F3)
                        )
                        Column {
                            Text(
                                text = "First Time Setup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You haven't set a cancellation PIN yet. Choose a 4-digit PIN that you'll remember easily in emergencies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else if (actualPin != null && actualPin.length != 4) {
                // Show warning for users with old 6-digit PIN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Column {
                            Text(
                                text = "Update Your PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You have an old ${actualPin.length}-digit PIN from a previous version. Please update it to a new 4-digit PIN for better security.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current PIN Field (only if user has existing PIN)
            if (hasExistingPin) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        // Allow length of existing PIN (might be 4 or 6 digits)
                        val maxLength = actualPin?.length ?: 4
                        if (it.length <= maxLength && it.all { char -> char.isDigit() }) {
                            currentPin = it
                            errorMessage = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Current PIN") },
                    placeholder = { Text("*".repeat(actualPin?.length ?: 4)) },
                    visualTransformation = if (showCurrentPin) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPin = !showCurrentPin }) {
                            Icon(
                                imageVector = if (showCurrentPin) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (showCurrentPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    supportingText = {
                        Text("Enter your current ${actualPin?.length ?: 4}-digit PIN")
                    }
                )
            }

            // New PIN Field
            OutlinedTextField(
                value = newPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        newPin = it
                        errorMessage = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (hasExistingPin) "New PIN" else "Create PIN") },
                placeholder = { Text("****") },
                visualTransformation = if (showNewPin) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { showNewPin = !showNewPin }) {
                        Icon(
                            imageVector = if (showNewPin) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = if (showNewPin) "Hide PIN" else "Show PIN"
                        )
                    }
                },
                supportingText = {
                    Text("Choose a 4-digit PIN (0000-9999)")
                }
            )

            // Confirm PIN Field
            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        confirmPin = it
                        errorMessage = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm PIN") },
                placeholder = { Text("****") },
                visualTransformation = if (showConfirmPin) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (validateInputs(
                                hasExistingPin = hasExistingPin,
                                currentPin = currentPin,
                                newPin = newPin,
                                confirmPin = confirmPin,
                                existingPin = actualPin ?: "",
                                onError = { errorMessage = it }
                            )
                        ) {
                            isProcessing = true
                            viewModel.updateCancellationPin(newPin) { success, error ->
                                isProcessing = false
                                if (success) {
                                    successMessage = "PIN ${if (hasExistingPin) "updated" else "created"} successfully!"
                                    scope.launch {
                                        delay(1500)
                                        navController.navigateUp()
                                    }
                                } else {
                                    errorMessage = error ?: "Failed to ${if (hasExistingPin) "update" else "create"} PIN"
                                }
                            }
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPin = !showConfirmPin }) {
                        Icon(
                            imageVector = if (showConfirmPin) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = if (showConfirmPin) "Hide PIN" else "Show PIN"
                        )
                    }
                },
                supportingText = {
                    Text("Re-enter your new 4-digit PIN")
                }
            )

            // Error Message
            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Success Message
            if (successMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            PrimaryButton(
                text = if (isProcessing) {
                    "Saving..."
                } else {
                    if (hasExistingPin) "Update PIN" else "Create PIN"
                },
                onClick = {
                    if (validateInputs(
                            hasExistingPin = hasExistingPin,
                            currentPin = currentPin,
                            newPin = newPin,
                            confirmPin = confirmPin,
                            existingPin = actualPin ?: "",
                            onError = { errorMessage = it }
                        )
                    ) {
                        isProcessing = true
                        viewModel.updateCancellationPin(newPin) { success, error ->
                            isProcessing = false
                            if (success) {
                                successMessage = "PIN ${if (hasExistingPin) "updated" else "created"} successfully!"
                                scope.launch {
                                    delay(1500)
                                    navController.navigateUp()
                                }
                            } else {
                                errorMessage = error ?: "Failed to ${if (hasExistingPin) "update" else "create"} PIN"
                            }
                        }
                    }
                },
                enabled = !isProcessing && newPin.length == 4 && confirmPin.length == 4 &&
                        (!hasExistingPin || currentPin.length == (actualPin?.length ?: 4)),
                modifier = Modifier.fillMaxWidth()
            )

            // PIN Requirements
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PIN Requirements:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    PinRequirementRow(
                        text = "Must be exactly 4 digits",
                        isMet = newPin.length == 4
                    )
                    PinRequirementRow(
                        text = "Only numbers (0-9)",
                        isMet = newPin.all { it.isDigit() }
                    )
                    PinRequirementRow(
                        text = "New PIN and confirmation must match",
                        isMet = newPin == confirmPin && newPin.isNotEmpty()
                    )
                    if (hasExistingPin) {
                        PinRequirementRow(
                            text = "Current PIN must be entered",
                            isMet = currentPin.length == (actualPin?.length ?: 4)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Validate all PIN inputs before submission
 */
private fun validateInputs(
    hasExistingPin: Boolean,
    currentPin: String,
    newPin: String,
    confirmPin: String,
    existingPin: String,
    onError: (String) -> Unit
): Boolean {
    // Validate current PIN if user has existing PIN
    if (hasExistingPin && currentPin != existingPin) {
        onError("Current PIN is incorrect")
        return false
    }

    // Validate new PIN length
    if (newPin.length != 4) {
        onError("New PIN must be exactly 4 digits")
        return false
    }

    // Validate new PIN is numeric
    if (!newPin.all { it.isDigit() }) {
        onError("PIN must contain only numbers")
        return false
    }

    // Validate PINs match
    if (newPin != confirmPin) {
        onError("New PIN and confirmation do not match")
        return false
    }

    // Warn about weak PINs
    if (newPin == "0000" || newPin == "1234" || newPin == "9999") {
        onError("Consider using a more secure PIN")
        return false
    }

    return true
}

/**
 * PIN Requirement Row Component
 */
@Composable
private fun PinRequirementRow(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isMet) {
                Icons.Default.Lock
            } else {
                Icons.Default.Lock
            },
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) Color(0xFF4CAF50) else Color.Gray
        )
    }
}