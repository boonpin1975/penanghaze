package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.example.data.location.PenangStationsData
import com.example.data.model.HazeLevel
import com.example.data.model.PenangStation
import com.example.ui.HazeUiState
import com.example.ui.components.CardContainer
import com.example.ui.components.GoogleMapPanelView
import com.example.ui.components.GoogleMapType
import com.example.ui.components.PenangInteractiveMapView
import com.example.ui.theme.*

@Composable
fun PenangMapScreen(
    state: HazeUiState,
    onSelectStation: (PenangStation?) -> Unit,
    onSetActiveStation: (PenangStation) -> Unit,
    onViewStationHistory: (PenangStation) -> Unit,
    onToggleHeatmap: () -> Unit,
    onSetDistrictFilter: (String) -> Unit,
    onToggleMapRenderer: () -> Unit = {},
    onSetMapType: (GoogleMapType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Map station reading values
    val stationReadings = remember(state.stationComparisons, state.reading) {
        val map = mutableMapOf<String, Int>()
        state.stationComparisons.forEach { (station, api) ->
            map[station.id] = api
        }
        // If current live reading has specific station, use its real reading
        state.reading?.let { r ->
            map[r.station.id] = r.apiValue
        }
        map
    }

    val currentGoogleMapType = remember(state.googleMapTypeCode) {
        when (state.googleMapTypeCode) {
            "y" -> GoogleMapType.SATELLITE
            "p" -> GoogleMapType.TERRAIN
            "osm" -> GoogleMapType.OSM
            else -> GoogleMapType.ROADMAP
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("penang_map_screen")
    ) {
        // 1. Google Map Panel View (or Stylized Canvas)
        if (state.isGoogleMapMode) {
            GoogleMapPanelView(
                stations = state.allStations,
                stationReadings = stationReadings,
                selectedStation = state.selectedMapStation,
                onSelectStation = onSelectStation,
                isHeatmapEnabled = state.isHeatmapEnabled,
                districtFilter = state.mapDistrictFilter,
                mapType = currentGoogleMapType,
                onMapTypeChange = onSetMapType,
                userLatitude = state.reading?.station?.latitude ?: 5.4164,
                userLongitude = state.reading?.station?.longitude ?: 100.3327,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PenangInteractiveMapView(
                stations = state.allStations,
                stationReadings = stationReadings,
                selectedStation = state.selectedMapStation,
                onSelectStation = onSelectStation,
                isHeatmapEnabled = state.isHeatmapEnabled,
                districtFilter = state.mapDistrictFilter,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top Filter and Mode Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 5.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Chips: ALL / ISLAND / MAINLAND
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = state.mapDistrictFilter == "ALL",
                            onClick = { onSetDistrictFilter("ALL") },
                            label = { Text("All (11)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GeoOrange,
                                selectedLabelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = state.mapDistrictFilter == "ISLAND",
                            onClick = { onSetDistrictFilter("ISLAND") },
                            label = { Text("Island (6)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GeoOrange,
                                selectedLabelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = state.mapDistrictFilter == "MAINLAND",
                            onClick = { onSetDistrictFilter("MAINLAND") },
                            label = { Text("Mainland (5)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GeoOrange,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Heatmap toggle button
                        IconButton(
                            onClick = onToggleHeatmap,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isHeatmapEnabled) Icons.Default.Layers else Icons.Default.LayersClear,
                                contentDescription = "Toggle Heatmap",
                                tint = if (state.isHeatmapEnabled) GeoOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Map Renderer Mode Toggle (Google Map vs Vector)
                        IconButton(
                            onClick = onToggleMapRenderer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isGoogleMapMode) Icons.Default.Map else Icons.Default.Palette,
                                contentDescription = "Switch Map View",
                                tint = if (state.isGoogleMapMode) GeoBlue else GeoOrange,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Station Detail Card / Modal when station is selected
        AnimatedVisibility(
            visible = state.selectedMapStation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            state.selectedMapStation?.let { station ->
                val api = stationReadings[station.id] ?: 52
                val level = HazeLevel.fromApi(api)
                val dist = state.reading?.let {
                    PenangStationsData.calculateDistanceKm(
                        it.station.latitude, it.station.longitude,
                        station.latitude, station.longitude
                    )
                } ?: 0.0

                StationDetailMapCard(
                    station = station,
                    apiValue = api,
                    hazeLevel = level,
                    distanceKm = dist,
                    isActiveStation = state.selectedZone?.id == station.id || (state.selectedZone == null && state.reading?.station?.id == station.id),
                    onSetActive = { onSetActiveStation(station) },
                    onViewHistory = { onViewStationHistory(station) },
                    onOpenGoogleMaps = {
                        try {
                            val uri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}(${Uri.encode(station.name)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to web browser Google Maps
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${station.latitude},${station.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    onClose = { onSelectStation(null) }
                )
            }
        }
    }
}

@Composable
fun StationDetailMapCard(
    station: PenangStation,
    apiValue: Int,
    hazeLevel: HazeLevel,
    distanceKm: Double,
    isActiveStation: Boolean,
    onSetActive: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenGoogleMaps: () -> Unit = {},
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hazeLevel.color.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("station_detail_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Station Name + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = hazeLevel.containerColor
                        ) {
                            Text(
                                text = if (station.isOfficialCAQM) "OFFICIAL DOE CAQM" else "SUPPLEMENTAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = hazeLevel.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isActiveStation) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GeoGreenBg
                            ) {
                                Text(
                                    text = "ACTIVE DASHBOARD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GeoGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = station.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "${station.district} • ${station.landmark}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time AQI & Pollutant Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large AQI Pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = hazeLevel.containerColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, hazeLevel.color.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$apiValue",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = hazeLevel.color
                        )
                        Text(
                            text = hazeLevel.title.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = hazeLevel.color
                        )
                        Text(
                            text = hazeLevel.titleMalay,
                            fontSize = 9.sp,
                            color = hazeLevel.color.copy(alpha = 0.8f)
                        )
                    }
                }

                // Pollutants snapshot
                Column(
                    modifier = Modifier.weight(1.9f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pm25Val = (apiValue * 0.72).toInt()
                    val pm10Val = (apiValue * 1.15).toInt()

                    PollutantMiniBar(label = "PM2.5", value = "$pm25Val µg/m³", fraction = (pm25Val / 150f).coerceIn(0.1f, 1f), color = hazeLevel.color)
                    PollutantMiniBar(label = "PM10", value = "$pm10Val µg/m³", fraction = (pm10Val / 200f).coerceIn(0.1f, 1f), color = hazeLevel.color)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GPS: ${String.format("%.4f", station.latitude)}, ${String.format("%.4f", station.longitude)}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${distanceKm}km away",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeoOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Directions, History, Set Active
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenGoogleMaps,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoBlue)
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoOrange)
                ) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSetActive,
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActiveStation) GeoGreen else GeoOrange,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isActiveStation) Icons.Default.Check else Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isActiveStation) "Active" else "Set Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PollutantMiniBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}
