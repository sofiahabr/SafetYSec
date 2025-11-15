package com.example.safetysec.presentation.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.safetysec.presentation.components.CustomTopAppBar
import com.example.safetysec.data.preferences.ThemeMode
import com.example.safetysec.data.preferences.ThemePreferences
import kotlinx.coroutines.launch

/**
 * Settings Screen
 *
 * App settings and preferences:
 * - Notifications
 * - Language
 * - Theme
 * - Privacy
 * - About
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var alertSoundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }

    var showThemeDialog by remember { mutableStateOf(false) }
    val selectedTheme by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Settings",
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
        ) {
            // Notifications Section
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    subtitle = "Receive alert notifications",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )

                SettingsSwitchItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Alert Sound",
                    subtitle = "Play sound for alerts",
                    checked = alertSoundEnabled,
                    onCheckedChange = { alertSoundEnabled = it },
                    enabled = notificationsEnabled
                )

                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    subtitle = "Vibrate on alerts",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    enabled = notificationsEnabled
                )
            }

            Divider()

            // Preferences Section
            SettingsSection(title = "Preferences") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = selectedLanguage,
                    onClick = { showLanguageDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = getThemeDisplayName(selectedTheme),
                    onClick = { showThemeDialog = true }
                )
            }

            Divider()

            // Privacy & Security Section
            SettingsSection(title = "Privacy & Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Settings",
                    subtitle = "Manage your privacy",
                    onClick = { /* TODO: Navigate to privacy settings */ }
                )

                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Security",
                    subtitle = "Two-factor authentication, etc.",
                    onClick = { /* TODO: Navigate to security settings */ }
                )
            }

            Divider()

            // Help & Support Section
            SettingsSection(title = "Help & Support") {
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help Center",
                    subtitle = "Get help and support",
                    onClick = { /* TODO: Navigate to help */ }
                )

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About SafetYSec",
                    subtitle = "Version 1.0.0",
                    onClick = { /* TODO: Show about dialog */ }
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Terms & Privacy Policy",
                    subtitle = "Legal information",
                    onClick = { /* TODO: Show terms */ }
                )
            }

            Divider()

            // Account Section
            SettingsSection(title = "Account") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete Account",
                    subtitle = "Permanently delete your account",
                    onClick = { /* TODO: Show delete confirmation */ },
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                selectedLanguage = language
                showLanguageDialog = false
                // TODO: Implement language change
            },
            onDismiss = {
                showLanguageDialog = false
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = selectedTheme,
            onThemeSelected = { theme ->
                scope.launch {
                    themePreferences.setThemeMode(theme)
                }
                showThemeDialog = false
            },
            onDismiss = {
                showThemeDialog = false
            }
        )
    }
}

/**
 * Settings Section Header
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        content()
    }
}

/**
 * Settings Item (clickable)
 */
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go to $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Settings Switch Item
 */
@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.5f
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Language Selection Dialog
 */
@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf("English", "Portuguese", "Spanish", "French")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Language, contentDescription = null)
        },
        title = {
            Text("Select Language")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                languages.forEach { language ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onLanguageSelected(language) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (language == currentLanguage) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
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
 * Theme Selection Dialog
 */

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.DarkMode, contentDescription = null)
        },
        title = {
            Text("Select Theme")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { theme ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onThemeSelected(theme) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (theme == currentTheme) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = getThemeDisplayName(theme),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = getThemeDescription(theme),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (theme == currentTheme) {
                                Icon(
                                    imageVector = Icons.Default.Check,
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
 * Get theme display name
 */
private fun getThemeDisplayName(theme: ThemeMode): String {
    return when (theme) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System default"
    }
}

/**
 * Get theme description
 */
private fun getThemeDescription(theme: ThemeMode): String {
    return when (theme) {
        ThemeMode.LIGHT -> "Always use light theme"
        ThemeMode.DARK -> "Always use dark theme"
        ThemeMode.SYSTEM -> "Follow system settings"
    }
}