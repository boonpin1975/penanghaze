package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PenangStation
import com.example.data.model.RecommendationUrgency
import com.example.ui.HazeUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    state: HazeUiState,
    onSelectZone: (PenangStation?) -> Unit,
    onNavigateToHealth: () -> Unit,
    onDismissAlertBanner: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Penang Zone selector chip carousel
        PenangZoneSelectorRow(
            selectedZone = state.selectedZone,
            allStations = state.allStations,
            onSelectZone = onSelectZone
        )

        // Real-Time Alert Banner if threshold reached
        AnimatedVisibility(
            visible = state.activeAlertBanner != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            state.activeAlertBanner?.let { bannerText ->
                AlertBanner(
                    message = bannerText,
                    onDismiss = onDismissAlertBanner
                )
            }
        }

        if (state.isLoading && state.reading == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GeoOrange)
            }
        } else if (state.reading != null) {
            val reading = state.reading

            // 1. Primary Geometric Haze Gauge
            HazeGauge(reading = reading)

            // 2. 2x2 Geometric Action & Precaution Grid (Outdoors, Sensitive, Health, Home)
            GeometricActionGrid(reading = reading)

            // 3. Station Type / Live Connection Dark Pill
            LocationSourceBadge(reading = reading)

            // 4. Top Automated Health Recommendation Highlight
            val topRec = state.recommendations.firstOrNull()
            if (topRec != null) {
                TopHealthAdvisoryCard(
                    recommendation = topRec,
                    onViewAllClick = onNavigateToHealth
                )
            }

            // 5. Live Wind & Atmospheric Dispersion
            WeatherAndWindCard(
                temp = reading.temperature,
                humidity = reading.humidity,
                windSpeed = reading.windSpeedKmH,
                windDir = reading.windDirection
            )

            // 6. Full Pollutant Breakdown (PM2.5, PM10, O3, NO2, SO2, CO)
            PollutantGrid(reading = reading)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PenangZoneSelectorRow(
    selectedZone: PenangStation?,
    allStations: List<PenangStation>,
    onSelectZone: (PenangStation?) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Auto-detect chip
        FilterChip(
            selected = selectedZone == null,
            onClick = { onSelectZone(null) },
            shape = RoundedCornerShape(12.dp),
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Auto GPS",
                        modifier = Modifier.size(14.dp),
                        tint = if (selectedZone == null) PureWhite else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Detect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GeoOrange,
                selectedLabelColor = PureWhite,
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedZone == null,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                selectedBorderColor = GeoOrange
            ),
            modifier = Modifier.testTag("zone_chip_auto")
        )

        allStations.forEach { station ->
            val isSelected = selectedZone?.id == station.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelectZone(station) },
                shape = RoundedCornerShape(12.dp),
                label = {
                    Text(
                        text = station.name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GeoOrange,
                    selectedLabelColor = PureWhite,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = GeoOrange
                ),
                modifier = Modifier.testTag("zone_chip_${station.id}")
            )
        }
    }
}

@Composable
fun AlertBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GeoOrangeBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoOrange),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_alert_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = GeoOrangeDark,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GeoOrangeDark,
                modifier = Modifier.weight(1f),
                lineHeight = 18.sp
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = GeoOrangeDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TopHealthAdvisoryCard(
    recommendation: com.example.data.model.HealthRecommendation,
    onViewAllClick: () -> Unit
) {
    val isUrgent = recommendation.urgency != RecommendationUrgency.NORMAL

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewAllClick() }
            .testTag("top_health_advisory_card"),
        shape = RoundedCornerShape(24.dp),
        color = if (isUrgent) GeoOrangeBg else MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUrgent) GeoOrangeLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isUrgent) GeoOrange else GeoGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HEALTH ADVISORY • ${recommendation.category.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUrgent) GeoOrangeDark else GeoGreen,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "View All →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoOrange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = recommendation.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = recommendation.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (recommendation.maskType != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GeoOrangeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoOrangeLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Masks,
                            contentDescription = "Mask",
                            tint = GeoOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mask: ${recommendation.maskType}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GeoOrangeDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherAndWindCard(
    temp: Double,
    humidity: Int,
    windSpeed: Double,
    windDir: String
) {
    CardContainer(
        modifier = Modifier.testTag("weather_and_wind_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PENANG ATMOSPHERE & WIND RADAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherMiniStat(
                    icon = Icons.Default.Air,
                    label = "Wind Velocity",
                    value = "$windSpeed km/h",
                    sub = windDir
                )
                WeatherMiniStat(
                    icon = Icons.Default.Thermostat,
                    label = "Temperature",
                    value = "$temp°C",
                    sub = "Penang Maritime"
                )
                WeatherMiniStat(
                    icon = Icons.Default.WaterDrop,
                    label = "Humidity",
                    value = "$humidity%",
                    sub = "Moisture Trap"
                )
            }
        }
    }
}

@Composable
fun WeatherMiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    sub: String
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GeoGreenBg,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = GeoGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sub,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
