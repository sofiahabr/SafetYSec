package com.example.safetysec.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetysec.R
import com.example.safetysec.domain.model.UserRole
import com.example.safetysec.presentation.components.*
import com.example.safetysec.presentation.screens.monitor.MonitorDashScreen
import com.example.safetysec.presentation.screens.protectedUser.ProtectedDashboardScreen
import com.example.safetysec.presentation.theme.PrimaryPurple
import com.example.safetysec.presentation.viewmodel.AuthViewModel
import com.example.safetysec.presentation.viewmodel.MonitorDashboardViewModel

/**
 * Smart Dashboard Screen
 *
 * Routes to the appropriate dashboard based on user role:
 * - MONITOR -> Monitor Dashboard
 * - PROTECTED -> Protected Dashboard
 * - DUAL -> Combined Dashboard with tabs
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user

    when {
        currentUser == null -> {
            // Loading or not authenticated
            Scaffold {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        currentUser.role == UserRole.DUAL -> {
            // User is both Monitor and Protected - show combined dashboard with tabs
            DualDashboardScreen(navController = navController)
        }

        currentUser.role == UserRole.MONITOR -> {
            // Monitor only - show monitor dashboard
            val monitorViewModel: MonitorDashboardViewModel = hiltViewModel()
            MonitorDashScreen(
                navController = navController,
                viewModel = monitorViewModel,
                showBottomBar = true
            )
        }

        currentUser.role == UserRole.PROTECTED -> {
            // Protected only - show protected dashboard
            ProtectedDashboardScreen(
                navController = navController,
                showBottomBar = true
            )
        }

        else -> {
            // Fallback - show error
            Scaffold {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.error_unknown_role))
                }
            }
        }
    }
}

/**
 * Dual Dashboard Screen
 *
 * For users who are both Monitor and Protected
 * Shows tabs to switch between views
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualDashboardScreen(
    navController: NavController,
    monitorViewModel: MonitorDashboardViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryPurple)
            ) {
                // Title bar
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dashboard),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryPurple,
                        titleContentColor = Color.White
                    )
                )

                // Tab row below title
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = stringResource(R.string.protected_user),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = stringResource(R.string.monitor),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    // Protected Dashboard (no top bar or bottom nav - parent has them)
                    ProtectedDashboardScreen(
                        navController = navController,
                        showBottomBar = false,
                        showTopBar = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // Monitor Dashboard (no top bar or bottom nav - parent has them)
                    MonitorDashScreen(
                        navController = navController,
                        viewModel = monitorViewModel,
                        showBottomBar = false,
                        showTopBar = false
                    )
                }
            }
        }
    }
}