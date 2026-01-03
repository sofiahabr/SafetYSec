package com.example.safetysec.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safetysec.R
import com.example.safetysec.presentation.theme.PrimaryPurple

/**
 * Empty State Component
 *
 * Display when lists or data are empty
 */

/**
 * Generic Empty State
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButton: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(120.dp),
            tint = Color.Gray.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Action button (optional)
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actionButton()
        }
    }
}

/**
 * Empty List State - For empty lists
 */
@Composable
fun EmptyListState(
    title: String = stringResource(R.string.no_items_found),
    message: String = stringResource(R.string.no_items_to_display),
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null
) {
    EmptyState(
        icon = Icons.Default.List,
        title = title,
        message = message,
        modifier = modifier,
        actionButton = if (onAddClick != null) {
            {
                PrimaryButton(
                    text = stringResource(R.string.add_item),
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        } else null
    )
}

/**
 * No Search Results State
 */
@Composable
fun NoSearchResultsState(
    searchQuery: String = "",
    modifier: Modifier = Modifier,
    onClearSearch: (() -> Unit)? = null
) {
    EmptyState(
        icon = Icons.Default.SearchOff,
        title = stringResource(R.string.no_results_found),
        message = if (searchQuery.isNotEmpty()) {
            stringResource(R.string.no_results_for_query, searchQuery)
        } else {
            stringResource(R.string.try_adjusting_search)
        },
        modifier = modifier,
        actionButton = if (onClearSearch != null) {
            {
                CustomTextButton(
                    text = stringResource(R.string.clear_search),
                    onClick = onClearSearch
                )
            }
        } else null
    )
}

/**
 * No Alerts State - For empty alerts list
 */
@Composable
fun NoAlertsState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Notifications,
        title = stringResource(R.string.no_alerts),
        message = stringResource(R.string.no_alerts_message),
        modifier = modifier
    )
}

/**
 * No Monitors State - For protected users with no monitors
 */
@Composable
fun NoMonitorsState(
    modifier: Modifier = Modifier,
    onAddMonitor: () -> Unit
) {
    EmptyState(
        icon = Icons.Default.Person,
        title = stringResource(R.string.no_monitors),
        message = stringResource(R.string.no_monitors_message),
        modifier = modifier,
        actionButton = {
            PrimaryButton(
                text = stringResource(R.string.add_monitor),
                onClick = onAddMonitor,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    )
}

/**
 * No Protected Users State - For monitors with no protected users
 */
@Composable
fun NoProtectedUsersState(
    modifier: Modifier = Modifier,
    onAddProtected: () -> Unit
) {
    EmptyState(
        icon = Icons.Default.Person,
        title = stringResource(R.string.no_protected_users),
        message = stringResource(R.string.no_protected_users_message),
        modifier = modifier,
        actionButton = {
            PrimaryButton(
                text = stringResource(R.string.add_protected_user),
                onClick = onAddProtected,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    )
}

/**
 * No Rules State - For empty rules list
 */
@Composable
fun NoRulesState(
    modifier: Modifier = Modifier,
    onCreateRule: () -> Unit
) {
    EmptyState(
        icon = Icons.Default.Rule,
        title = stringResource(R.string.no_rules),
        message = stringResource(R.string.no_rules_message),
        modifier = modifier,
        actionButton = {
            PrimaryButton(
                text = stringResource(R.string.create_rule),
                onClick = onCreateRule,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    )
}

/**
 * Network Error State
 */
@Composable
fun NetworkErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    EmptyState(
        icon = Icons.Default.WifiOff,
        title = stringResource(R.string.no_connection),
        message = stringResource(R.string.no_connection_message),
        modifier = modifier,
        actionButton = {
            PrimaryButton(
                text = stringResource(R.string.retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    )
}

/**
 * Generic Error State
 */
@Composable
fun ErrorState(
    title: String = stringResource(R.string.something_went_wrong),
    message: String = stringResource(R.string.error_occurred_try_later),
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        icon = Icons.Default.ErrorOutline,
        title = title,
        message = message,
        modifier = modifier,
        actionButton = if (onRetry != null) {
            {
                PrimaryButton(
                    text = stringResource(R.string.try_again),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        } else null
    )
}