package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.HazeLevel
import com.example.data.model.PenangStation
import com.example.ui.HazeUiState
import com.example.ui.components.CardContainer
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    state: HazeUiState,
    onSelectStation: (PenangStation) -> Unit,
    onSetActiveStation: (PenangStation) -> Unit,
    onViewStationHistory: (PenangStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("API_ASC") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedStationId by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(state.stationComparisons, selectedFilter, sortBy, searchQuery) {
        var list = state.stationComparisons.filter { (station, _) ->
            val matchesDistrict = when (selectedFilter) {
                "ISLAND" -> station.district.contains("Island", ignoreCase = true)
                "MAINLAND" -> station.district.contains("Mainland", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    station.name.contains(searchQuery, ignoreCase = true) ||
                    station.district.contains(searchQuery, ignoreCase = true) ||
                    station.landmark.contains(searchQuery, ignoreCase = true)

            matchesDistrict && matchesSearch
        }

        list = when (sortBy) {
            "API_ASC" -> list.sortedBy { it.second }
            "API_DESC" -> list.sortedByDescending { it.second }
            "NAME" -> list.sortedBy { it.first.name }
            else -> list
        }
        list
    }

    // Quick Stats
    val avgApi = remember(state.stationComparisons) {
        if (state.stationComparisons.isNotEmpty()) {
            state.stationComparisons.map { it.second }.average().toInt()
        } else 52
    }

    val cleanestStation = remember(state.stationComparisons) {
        state.stationComparisons.minByOrNull { it.second }
    }

    val highestStation = remember(state.stationComparisons) {
        state.stationComparisons.maxByOrNull { it.second }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("stations_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Overview Card
        CardContainer(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PENANG SENSOR RADAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "11 Live Monitoring Stations",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GeoGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoGreenLight)
                    ) {
                        Text(
                            text = "LIVE CAQM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GeoGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Mini Summary KPI Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avg KPI
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STATE AVG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$avgApi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = GeoOrange
                            )
                        }
                    }

                    // Cleanest KPI
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = GeoGreenBg
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CLEANEST",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoGreen
                            )
                            Text(
                                text = "${cleanestStation?.second ?: "--"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = GeoGreen
                            )
                        }
                    }

                    // Highest KPI
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = GeoOrangeBg
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PEAK ZONE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoOrangeDark
                            )
                            Text(
                                text = "${highestStation?.second ?: "--"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = GeoOrangeDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Island / Mainland Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("All (${state.stationComparisons.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoOrange,
                            selectedLabelColor = PureWhite,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "ISLAND",
                        onClick = { selectedFilter = "ISLAND" },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Island (6)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoOrange,
                            selectedLabelColor = PureWhite,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "MAINLAND",
                        onClick = { selectedFilter = "MAINLAND" },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Mainland (5)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoOrange,
                            selectedLabelColor = PureWhite,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        // Search and Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Penang station...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeoOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            )

            // Sort Toggle Button
            FilledTonalButton(
                onClick = {
                    sortBy = when (sortBy) {
                        "API_ASC" -> "API_DESC"
                        "API_DESC" -> "NAME"
                        else -> "API_ASC"
                    }
                },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(16.dp),
                        tint = GeoOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (sortBy) {
                            "API_ASC" -> "API ↑"
                            "API_DESC" -> "API ↓"
                            else -> "Name"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Station Cards List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList, key = { it.first.id }) { (station, apiVal) ->
                val level = HazeLevel.fromApi(apiVal)
                val isCurrentlyActive = state.reading?.station?.id == station.id
                val isExpanded = expandedStationId == station.id

                StationDetailCard(
                    station = station,
                    apiValue = apiVal,
                    level = level,
                    isActiveZone = isCurrentlyActive,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedStationId = if (isExpanded) null else station.id
                    },
                    onSetActive = {
                        onSetActiveStation(station)
                    },
                    onViewHistory = {
                        onViewStationHistory(station)
                    },
                    onOpenInExternalMap = {
                        try {
                            val geoUri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}(${Uri.encode(station.name)})")
                            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${station.latitude},${station.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StationDetailCard(
    station: PenangStation,
    apiValue: Int,
    level: HazeLevel,
    isActiveZone: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSetActive: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenInExternalMap: () -> Unit
) {
    val levelColor = when (level) {
        HazeLevel.GOOD -> GeoGreen
        HazeLevel.MODERATE -> GeoOrange
        HazeLevel.UNHEALTHY -> GeoOrangeDark
        HazeLevel.VERY_UNHEALTHY -> HazeRedVeryUnhealthy
        HazeLevel.HAZARDOUS -> HazeHazardous
    }

    val levelBg = when (level) {
        HazeLevel.GOOD -> GeoGreenLight
        HazeLevel.MODERATE -> GeoOrangeLight
        HazeLevel.UNHEALTHY -> Color(0xFFFFEDD5)
        HazeLevel.VERY_UNHEALTHY -> Color(0xFFFEE2E2)
        HazeLevel.HAZARDOUS -> Color(0xFFFCE7F3)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("station_card_${station.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            if (isActiveZone) 2.dp else 1.dp,
            if (isActiveZone) GeoOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // API Geometric Square Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = levelBg,
                    modifier = Modifier.size(54.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$apiValue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = levelColor
                        )
                        Text(
                            text = "API",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Station Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = station.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isActiveZone) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GeoOrangeLight
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GeoOrangeDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${station.district} • ${level.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = levelColor
                    )

                    Text(
                        text = station.landmark,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar preview
                    LinearProgressIndicator(
                        progress = { (apiValue / 300f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = levelColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { onToggleExpand() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Station Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded Quick Action Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coordinates: ${String.format("%.4f", station.latitude)}°N, ${String.format("%.4f", station.longitude)}°E",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (station.isOfficialCAQM) GeoGreenBg else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (station.isOfficialCAQM) "DOE Certified" else "Hyperlocal",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (station.isOfficialCAQM) GeoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Set As Active Station
                        Button(
                            onClick = onSetActive,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActiveZone) GeoGreen else GeoOrange,
                                contentColor = PureWhite
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isActiveZone) Icons.Default.CheckCircle else Icons.Default.RadioButtonChecked,
                                contentDescription = "Active Zone",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isActiveZone) "Active Zone" else "Set Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. View History
                        OutlinedButton(
                            onClick = onViewHistory,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "History",
                                modifier = Modifier.size(14.dp),
                                tint = GeoOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "History",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. Directions
                        IconButton(
                            onClick = onOpenInExternalMap,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Directions,
                                contentDescription = "Get Directions in Google Maps",
                                tint = GeoBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
