package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HazeLevel
import com.example.data.model.PenangStation
import com.example.ui.HazeUiState
import com.example.ui.components.CardContainer
import com.example.ui.theme.*

@Composable
fun StationsScreen(
    state: HazeUiState,
    onSelectStation: (PenangStation) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("API_ASC") }

    val filteredList = remember(state.stationComparisons, selectedFilter, sortBy) {
        var list = state.stationComparisons.filter { (station, _) ->
            when (selectedFilter) {
                "ISLAND" -> station.district.contains("Island")
                "MAINLAND" -> station.district.contains("Mainland")
                else -> true
            }
        }

        list = when (sortBy) {
            "API_ASC" -> list.sortedBy { it.second }
            "API_DESC" -> list.sortedByDescending { it.second }
            "NAME" -> list.sortedBy { it.first.name }
            else -> list
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("stations_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header info
        CardContainer(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PENANG MONITORING RADAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "11 Sensor Zones State-Wide",
                            fontSize = 16.sp,
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
                            text = "DOE • LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GeoGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
                        label = { Text("All (${state.stationComparisons.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
                        label = { Text("Penang Island", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
                        label = { Text("Mainland", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoOrange,
                            selectedLabelColor = PureWhite,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        // Sorting Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SORT BY AIR QUALITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AssistChip(
                onClick = { sortBy = if (sortBy == "API_ASC") "API_DESC" else "API_ASC" },
                shape = RoundedCornerShape(10.dp),
                label = {
                    Text(
                        if (sortBy == "API_ASC") "Cleanest First ↑" else "Worst First ↓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(14.dp),
                        tint = GeoOrange
                    )
                }
            )
        }

        // Station Cards List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList, key = { it.first.id }) { (station, apiVal) ->
                val level = HazeLevel.fromApi(apiVal)
                val isCurrentlySelected = state.reading?.station?.id == station.id

                StationListItemCard(
                    station = station,
                    apiValue = apiVal,
                    level = level,
                    isSelected = isCurrentlySelected,
                    onSelect = { onSelectStation(station) }
                )
            }
        }
    }
}

@Composable
fun StationListItemCard(
    station: PenangStation,
    apiValue: Int,
    level: HazeLevel,
    isSelected: Boolean,
    onSelect: () -> Unit
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
            .clickable { onSelect() }
            .testTag("station_card_${station.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) GeoOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(GeoOrange, CircleShape)
                        )
                    }
                }

                Text(
                    text = station.district,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = station.landmark,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = if (isSelected) GeoOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
