package com.example.safetysec.presentation.screens.profile

import android.app.Activity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safetysec.R
import com.example.safetysec.presentation.components.CustomTopAppBar
import com.example.safetysec.data.preferences.ThemeMode
import com.example.safetysec.data.preferences.ThemePreferences
import com.example.safetysec.data.preferences.AppLanguage
import com.example.safetysec.data.preferences.LanguagePreferences
import com.example.safetysec.util.LanguageManager
import com.example.safetysec.presentation.viewmodel.AuthViewModel
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    val user = authState.user

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Theme preferences
    val themePreferences = remember { ThemePreferences(context) }
    val selectedTheme by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    // Language preferences
    val languagePreferences = remember { LanguagePreferences(context) }
    val selectedLanguage by languagePreferences.currentLanguage.collectAsState(initial = AppLanguage.ENGLISH)

    var notificationsEnabled by remember { mutableStateOf(true) }
    var alertSoundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = stringResource(R.string.settings),
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
            SettingsSection(title = stringResource(R.string.notifications)) {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.push_notifications),
                    subtitle = stringResource(R.string.push_notifications_desc),
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )

                SettingsSwitchItem(
                    icon = Icons.Default.VolumeUp,
                    title = stringResource(R.string.alert_sound),
                    subtitle = stringResource(R.string.alert_sound_desc),
                    checked = alertSoundEnabled,
                    onCheckedChange = { alertSoundEnabled = it },
                    enabled = notificationsEnabled
                )

                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.vibration),
                    subtitle = stringResource(R.string.vibration_desc),
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    enabled = notificationsEnabled
                )
            }

            Divider()

            // Preferences Section
            SettingsSection(title = stringResource(R.string.preferences)) {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    subtitle = selectedLanguage.displayName,
                    onClick = { showLanguageDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.theme),
                    subtitle = getThemeDisplayName(selectedTheme),
                    onClick = { showThemeDialog = true }
                )
            }

            Divider()

            // Privacy & Security Section
            SettingsSection(title = stringResource(R.string.privacy_security)) {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.privacy_settings),
                    subtitle = stringResource(R.string.privacy_settings_desc),
                    onClick = { /* TODO: Navigate to privacy settings */ }
                )

                SettingsItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.security),
                    subtitle = stringResource(R.string.security_desc),
                    onClick = { /* TODO: Navigate to security settings */ }
                )
            }

            Divider()

            // Help & Support Section
            SettingsSection(title = stringResource(R.string.help_support)) {
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = stringResource(R.string.help_center),
                    subtitle = stringResource(R.string.help_center_desc),
                    onClick = { /* TODO: Navigate to help */ }
                )

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about_safetysec),
                    subtitle = stringResource(R.string.version),
                    onClick = { /* TODO: Show about dialog */ }
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.terms_privacy),
                    subtitle = stringResource(R.string.terms_privacy_desc),
                    onClick = { /* TODO: Show terms */ }
                )
            }

            Divider()

            // Account Section
            SettingsSection(title = stringResource(R.string.account)) {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.delete_account),
                    subtitle = stringResource(R.string.delete_account_desc),
                    onClick = {
                        authViewModel.deleteUserProfile()
                        navController.navigate("login")
                    },
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
                scope.launch {
                    // Save language preference
                    languagePreferences.setLanguage(language)

                    // Apply language change
                    LanguageManager.setAppLanguage(
                        context = context,
                        language = language,
                        activity = activity
                    )
                }
                showLanguageDialog = false
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
            contentDescription = stringResource(R.string.go_to, title),
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
 * Language Selection Dialog - with AppLanguage enum
 */
@Composable
private fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Language, contentDescription = null)
        },
        title = {
            Text(stringResource(R.string.select_language))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppLanguage.values().forEach { language ->
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
                            Column {
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = language.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.selected),
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
                Text(stringResource(R.string.close))
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
            Text(stringResource(R.string.select_theme))
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
                                    contentDescription = stringResource(R.string.selected),
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
                Text(stringResource(R.string.close))
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