package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: HazeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Runtime Permission Launcher for GPS Location
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        viewModel.loadData()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GeoOrange,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = "Haze Tracker",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Penang Haze",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "PENANG • REAL-TIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    // Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = GeoOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.loadData() },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("refresh_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = GeoOrange
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Data",
                                        tint = GeoOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = state.selectedNavTab == 0,
                    onClick = { viewModel.selectNavTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("DASHBOARD", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoOrange,
                        selectedTextColor = GeoOrange,
                        indicatorColor = GeoOrangeLight,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = state.selectedNavTab == 1,
                    onClick = { viewModel.selectNavTab(1) },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("MAP", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoOrange,
                        selectedTextColor = GeoOrange,
                        indicatorColor = GeoOrangeLight,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = state.selectedNavTab == 2,
                    onClick = { viewModel.selectNavTab(2) },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = "History") },
                    label = { Text("HISTORY", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoOrange,
                        selectedTextColor = GeoOrange,
                        indicatorColor = GeoOrangeLight,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = state.selectedNavTab == 3,
                    onClick = { viewModel.selectNavTab(3) },
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = "Health") },
                    label = { Text("HEALTH", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoOrange,
                        selectedTextColor = GeoOrange,
                        indicatorColor = GeoOrangeLight,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = state.selectedNavTab == 4,
                    onClick = { viewModel.selectNavTab(4) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.alertsList.isNotEmpty()) {
                                    Badge(containerColor = GeoOrange) {
                                        Text("${state.alertsList.size}", color = PureWhite)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Alerts")
                        }
                    },
                    label = { Text("ALERTS", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoOrange,
                        selectedTextColor = GeoOrange,
                        indicatorColor = GeoOrangeLight,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state.selectedNavTab) {
                0 -> DashboardScreen(
                    state = state,
                    onSelectZone = { viewModel.selectZone(it) },
                    onNavigateToHealth = { viewModel.selectNavTab(3) },
                    onNavigateToMap = { viewModel.selectNavTab(1) },
                    onDismissAlertBanner = { viewModel.dismissBanner() },
                    onRefresh = { viewModel.loadData() }
                )
                1 -> PenangMapScreen(
                    state = state,
                    onSelectStation = { viewModel.selectMapStation(it) },
                    onSetActiveStation = { station ->
                        viewModel.selectZone(station)
                        viewModel.selectNavTab(0)
                    },
                    onViewStationHistory = { station ->
                        viewModel.selectHistoryStation(station)
                        viewModel.selectNavTab(2)
                    },
                    onToggleHeatmap = { viewModel.toggleHeatmap() },
                    onSetDistrictFilter = { viewModel.setMapDistrictFilter(it) },
                    onToggleMapRenderer = { viewModel.toggleMapRendererMode() },
                    onSetMapType = { viewModel.setGoogleMapTypeCode(it.code) }
                )
                2 -> HistoryScreen(
                    state = state,
                    onSelectRange = { viewModel.selectHistoryRange(it) },
                    onSelectStation = { viewModel.selectHistoryStation(it) },
                    onSetCustomRange = { start, end -> viewModel.setCustomDateRange(start, end) }
                )
                3 -> HealthScreen(
                    state = state,
                    onUpdateProfile = { viewModel.updateProfile(it) }
                )
                4 -> AlertsForecastScreen(
                    state = state,
                    onUpdateCustomThresholds = { good, mod, unh, nMod, nUnh, nHaz ->
                        viewModel.updateCustomAlertThresholds(
                            goodLimit = good,
                            moderateLimit = mod,
                            unhealthyLimit = unh,
                            notifyModerate = nMod,
                            notifyUnhealthy = nUnh,
                            notifyHazardous = nHaz
                        )
                    },
                    onResetThresholds = { viewModel.resetThresholdsToDefault() },
                    onTogglePush = { viewModel.updateProfile(state.userProfile.copy(pushAlertsEnabled = it)) },
                    onSimulateSpike = { viewModel.triggerSimulatedSpike(it) },
                    onResetSpike = { viewModel.resetSimulatedSpike() },
                    onClearAlerts = { viewModel.clearAllAlerts() }
                )
            }
        }
    }
}
