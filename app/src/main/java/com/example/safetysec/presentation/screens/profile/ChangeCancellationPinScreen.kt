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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
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

    // Get validation error strings
    val errorCurrentPinIncorrect = stringResource(R.string.error_current_pin_incorrect)
    val errorPinMustBe4Digits = stringResource(R.string.error_pin_must_be_4_digits)
    val errorPinOnlyNumbers = stringResource(R.string.error_pin_only_numbers)
    val errorPinsDontMatch = stringResource(R.string.error_pins_dont_match)
    val errorPinWeak = stringResource(R.string.error_pin_weak)

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
                title = if (hasExistingPin)
                    stringResource(R.string.change_cancellation_pin)
                else
                    stringResource(R.string.set_cancellation_pin),
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    // Debug toggle button
                    IconButton(onClick = { showDebug = !showDebug }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = stringResource(R.string.toggle_debug)
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
                            text = stringResource(R.string.about_cancellation_pin),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.about_cancellation_pin_desc),
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
                                text = stringResource(R.string.first_time_setup),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.first_time_setup_desc),
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
                                text = stringResource(R.string.update_your_pin),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.update_pin_desc, actualPin.length),
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
                    label = { Text(stringResource(R.string.current_pin)) },
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
                                contentDescription = if (showCurrentPin)
                                    stringResource(R.string.hide_pin)
                                else
                                    stringResource(R.string.show_pin)
                            )
                        }
                    },
                    supportingText = {
                        Text(stringResource(R.string.enter_current_pin, actualPin?.length ?: 4))
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
                label = { Text(if (hasExistingPin) stringResource(R.string.new_pin) else stringResource(R.string.create_pin)) },
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
                            contentDescription = if (showNewPin)
                                stringResource(R.string.hide_pin)
                            else
                                stringResource(R.string.show_pin)
                        )
                    }
                },
                supportingText = {
                    Text(stringResource(R.string.choose_4_digit_pin))
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
                label = { Text(stringResource(R.string.confirm_pin)) },
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
                                onError = { errorMessage = it },
                                errorCurrentPinIncorrect = errorCurrentPinIncorrect,
                                errorPinMustBe4Digits = errorPinMustBe4Digits,
                                errorPinOnlyNumbers = errorPinOnlyNumbers,
                                errorPinsDontMatch = errorPinsDontMatch,
                                errorPinWeak = errorPinWeak
                            )
                        ) {
                            isProcessing = true
                            viewModel.updateCancellationPin(newPin) { success, error ->
                                isProcessing = false
                                if (success) {
                                    successMessage = if (hasExistingPin)
                                        "PIN updated successfully!"
                                    else
                                        "PIN created successfully!"
                                    scope.launch {
                                        delay(1500)
                                        navController.navigateUp()
                                    }
                                } else {
                                    errorMessage = error ?: if (hasExistingPin)
                                        "Failed to update PIN"
                                    else
                                        "Failed to create PIN"
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
                            contentDescription = if (showConfirmPin)
                                stringResource(R.string.hide_pin)
                            else
                                stringResource(R.string.show_pin)
                        )
                    }
                },
                supportingText = {
                    Text(stringResource(R.string.reenter_new_pin))
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
                    stringResource(R.string.saving)
                } else {
                    if (hasExistingPin) stringResource(R.string.update_pin) else stringResource(R.string.create_pin)
                },
                onClick = {
                    if (validateInputs(
                            hasExistingPin = hasExistingPin,
                            currentPin = currentPin,
                            newPin = newPin,
                            confirmPin = confirmPin,
                            existingPin = actualPin ?: "",
                            onError = { errorMessage = it },
                            errorCurrentPinIncorrect = errorCurrentPinIncorrect,
                            errorPinMustBe4Digits = errorPinMustBe4Digits,
                            errorPinOnlyNumbers = errorPinOnlyNumbers,
                            errorPinsDontMatch = errorPinsDontMatch,
                            errorPinWeak = errorPinWeak
                        )
                    ) {
                        isProcessing = true
                        viewModel.updateCancellationPin(newPin) { success, error ->
                            isProcessing = false
                            if (success) {
                                successMessage = if (hasExistingPin)
                                    "PIN updated successfully!"
                                else
                                    "PIN created successfully!"
                                scope.launch {
                                    delay(1500)
                                    navController.navigateUp()
                                }
                            } else {
                                errorMessage = error ?: if (hasExistingPin)
                                    "Failed to update PIN"
                                else
                                    "Failed to create PIN"
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
                        text = stringResource(R.string.pin_requirements),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    PinRequirementRow(
                        text = stringResource(R.string.pin_must_be_4_digits),
                        isMet = newPin.length == 4
                    )
                    PinRequirementRow(
                        text = stringResource(R.string.pin_only_numbers),
                        isMet = newPin.all { it.isDigit() }
                    )
                    PinRequirementRow(
                        text = stringResource(R.string.pin_must_match),
                        isMet = newPin == confirmPin && newPin.isNotEmpty()
                    )
                    if (hasExistingPin) {
                        PinRequirementRow(
                            text = stringResource(R.string.current_pin_required),
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
    onError: (String) -> Unit,
    errorCurrentPinIncorrect: String,
    errorPinMustBe4Digits: String,
    errorPinOnlyNumbers: String,
    errorPinsDontMatch: String,
    errorPinWeak: String
): Boolean {
    // Validate current PIN if user has existing PIN
    if (hasExistingPin && currentPin != existingPin) {
        onError(errorCurrentPinIncorrect)
        return false
    }

    // Validate new PIN length
    if (newPin.length != 4) {
        onError(errorPinMustBe4Digits)
        return false
    }

    // Validate new PIN is numeric
    if (!newPin.all { it.isDigit() }) {
        onError(errorPinOnlyNumbers)
        return false
    }

    // Validate PINs match
    if (newPin != confirmPin) {
        onError(errorPinsDontMatch)
        return false
    }

    // Warn about weak PINs
    if (newPin == "0000" || newPin == "1234" || newPin == "9999") {
        onError(errorPinWeak)
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