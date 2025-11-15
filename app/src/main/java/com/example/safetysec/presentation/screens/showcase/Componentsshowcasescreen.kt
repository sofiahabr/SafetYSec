package com.example.safetysec.presentation.screens.showcase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.safetysec.presentation.components.*

/**
 * Components Showcase Screen
 *
 * Demo screen showing all custom components
 * Useful for testing and documentation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsShowcaseScreen(
    onNavigateBack: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var searchValue by remember { mutableStateOf("") }
    var showLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Components Showcase",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section: Buttons
            SectionTitle("Buttons")

            PrimaryButton(
                text = "Primary Button",
                onClick = { }
            )

            SecondaryButton(
                text = "Secondary Button",
                onClick = { }
            )

            DangerButton(
                text = "Danger Button",
                onClick = { }
            )

            PrimaryButton(
                text = "Loading Button",
                onClick = { },
                isLoading = true
            )

            CustomTextButton(
                text = "Text Button",
                onClick = { }
            )

            IconTextButton(
                text = "Icon Button",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                onClick = { }
            )

            Divider()

            // Section: Text Fields
            SectionTitle("Text Fields")

            CustomTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = "Standard Text Field",
                placeholder = "Enter text..."
            )

            EmailTextField(
                value = "",
                onValueChange = { }
            )

            PasswordTextField(
                value = passwordValue,
                onValueChange = { passwordValue = it }
            )

            SearchTextField(
                value = searchValue,
                onValueChange = { searchValue = it },
                onClear = { searchValue = "" }
            )

            PhoneTextField(
                value = "",
                onValueChange = { }
            )

            Divider()

            // Section: Alert Cards
            SectionTitle("Alert Cards")

            SuccessAlert(
                message = "Operation completed successfully!",
                onDismiss = { }
            )

            ErrorAlert(
                message = "An error occurred while processing your request.",
                onDismiss = { }
            )

            WarningAlert(
                message = "Please review your settings before continuing.",
                onDismiss = { }
            )

            InfoAlert(
                message = "Here's some helpful information for you.",
                onDismiss = { }
            )

            Divider()

            // Section: Loading Components
            SectionTitle("Loading Components")

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    InlineLoading(message = "Loading data...")

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearLoading()

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearLoading(progress = 0.65f)
                }
            }

            SecondaryButton(
                text = if (showLoading) "Hide Loading Dialog" else "Show Loading Dialog",
                onClick = { showLoading = !showLoading }
            )

            Divider()

            // Section: Empty States
            SectionTitle("Empty States")

            Card {
                EmptyListState(
                    onAddClick = { }
                )
            }

            Card {
                NoSearchResultsState(
                    searchQuery = "example query",
                    onClearSearch = { }
                )
            }

            Divider()

            // Section: Skeleton Loading
            SectionTitle("Skeleton Loading")

            SkeletonCard()
        }

        // Show loading dialog
        if (showLoading) {
            LoadingDialog(
                message = "Processing your request...",
                onDismissRequest = { showLoading = false }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}