package com.example.safetysec.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.safetysec.presentation.theme.PrimaryPurple

/**
 * Loading Components
 *
 * Various loading indicators for different scenarios
 */

/**
 * Full Screen Loading - Covers entire screen with loading indicator
 */
@Composable
fun FullScreenLoading(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = PrimaryPurple,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryPurple,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Loading Dialog - Modal loading overlay
 */
@Composable
fun LoadingDialog(
    message: String = "Loading...",
    onDismissRequest: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = PrimaryPurple,
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Inline Loading - Small loading indicator for inline use
 */
@Composable
fun InlineLoading(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = PrimaryPurple,
            strokeWidth = 2.dp
        )

        if (message != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryPurple
            )
        }
    }
}

/**
 * Loading Box - Loading indicator in a box container
 */
@Composable
fun LoadingBox(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = PrimaryPurple,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Linear Loading - Progress bar style loading
 */
@Composable
fun LinearLoading(
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    if (progress != null) {
        // Determinate progress
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp),
            color = PrimaryPurple,
            trackColor = PrimaryPurple.copy(alpha = 0.2f)
        )
    } else {
        // Indeterminate progress
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp),
            color = PrimaryPurple,
            trackColor = PrimaryPurple.copy(alpha = 0.2f)
        )
    }
}

/**
 * Skeleton Loading - Placeholder skeleton for content loading
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Gray.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.medium
            )
    )
}

/**
 * Skeleton Text - Loading placeholder for text
 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .height(16.dp)
    )
}

/**
 * Skeleton Card - Loading placeholder for card
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonText()
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonText(modifier = Modifier.fillMaxWidth(0.5f))
        }
    }
}