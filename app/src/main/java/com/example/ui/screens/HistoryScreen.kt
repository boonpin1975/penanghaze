package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.location.PenangStationsData
import com.example.data.model.DateRangeType
import com.example.data.model.HistoricalAnalytics
import com.example.data.model.HistoricalHazeEntry
import com.example.data.model.PenangStation
import com.example.ui.HazeUiState
import com.example.ui.components.CardContainer
import com.example.ui.components.HazeTrendChart
import com.example.ui.theme.*
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    state: HazeUiState,
    onSelectRange: (DateRangeType) -> Unit,
    onSelectStation: (PenangStation?) -> Unit,
    onSetCustomRange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("history_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Date Range & Station Filter Header
        item {
            HistoryFilterControls(
                selectedRange = state.selectedHistoryRange,
                selectedStation = state.selectedHistoryStation,
                allStations = state.allStations,
                onSelectRange = { range ->
                    if (range == DateRangeType.CUSTOM) {
                        showCustomRangeDialog = true
                    } else {
                        onSelectRange(range)
                    }
                },
                onSelectStation = onSelectStation
            )
        }

        // Loading or Analytics Content
        if (state.isHistoryLoading || state.historyAnalytics == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GeoOrange)
                }
            }
        } else {
            val analytics = state.historyAnalytics

            // 2. Summary Hero Metrics & Peak Day
            item {
                HistoryHeroMetricsCard(analytics = analytics)
            }

            // 3. Interactive Trend Chart
            item {
                CardContainer(shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HISTORICAL HAZE TREND & THRESHOLDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${analytics.totalDays} DAYS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HazeTrendChart(
                            trendPoints = analytics.trendPoints,
                            thresholds = state.userProfile.customThresholds
                        )
                    }
                }
            }

            // 4. Days Distribution Breakdown Pill
            item {
                DaysDistributionCard(analytics = analytics)
            }

            // 5. Daily Records Log Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY OBSERVATION LOGS (${analytics.dailyEntries.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${analytics.startDateFormatted} - ${analytics.endDateFormatted}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 6. Daily Entry Cards
            items(analytics.dailyEntries, key = { it.dateTimestamp }) { entry ->
                DailyHistoryLogCard(entry = entry)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCustomRangeDialog) {
        CustomRangeSelectorDialog(
            onDismiss = { showCustomRangeDialog = false },
            onConfirm = { daysAgo ->
                val now = System.currentTimeMillis()
                val start = now - (daysAgo * 24L * 3600 * 1000)
                onSetCustomRange(start, now)
                showCustomRangeDialog = false
            }
        )
    }
}

@Composable
fun HistoryFilterControls(
    selectedRange: DateRangeType,
    selectedStation: PenangStation?,
    allStations: List<PenangStation>,
    onSelectRange: (DateRangeType) -> Unit,
    onSelectStation: (PenangStation?) -> Unit
) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Range Filter Chips Row
            Text(
                text = "SELECT TIMEFRAME",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateRangeType.values().forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onSelectRange(range) },
                        label = { Text(range.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoOrange,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Station Selector Dropdown / Scroll Row
            Text(
                text = "MONITORING STATION FILTER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val stationScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(stationScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStation == null,
                    onClick = { onSelectStation(null) },
                    label = { Text("All Penang (Average)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                allStations.forEach { st ->
                    FilterChip(
                        selected = selectedStation?.id == st.id,
                        onClick = { onSelectStation(st) },
                        label = { Text(st.name.substringBefore("("), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryHeroMetricsCard(analytics: HistoricalAnalytics) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Period Avg & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PERIOD AVERAGE API",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${analytics.overallAverageApi.roundToInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = GeoOrange
                        )
                        Text(
                            text = " API",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GeoOrangeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoOrangeLight)
                ) {
                    Text(
                        text = "${analytics.startDateFormatted} - ${analytics.endDateFormatted}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoOrangeDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Peak Day & Cleanest Day Comparison Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Peak Day
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = HazeRedVeryUnhealthy.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HazeRedVeryUnhealthy.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = HazeRedVeryUnhealthy,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PEAK POLLUTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = HazeRedVeryUnhealthy
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${analytics.peakPollutionDay.maxApi} API",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = HazeRedVeryUnhealthy
                        )
                        Text(
                            text = analytics.peakPollutionDay.shortDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = analytics.peakPollutionDay.stationName.substringBefore("("),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cleanest Day
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = HazeGreenGood.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HazeGreenGood.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Eco,
                                contentDescription = null,
                                tint = HazeGreenGood,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CLEANEST DAY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = HazeGreenGood
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${analytics.cleanestDay.minApi} API",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = HazeGreenGood
                        )
                        Text(
                            text = analytics.cleanestDay.shortDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = analytics.cleanestDay.stationName.substringBefore("("),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DaysDistributionCard(analytics: HistoricalAnalytics) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "AIR QUALITY CLASSIFICATION DISTRIBUTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment stacked distribution bar
            val total = analytics.totalDays.toFloat().coerceAtLeast(1f)
            val goodFrac = analytics.goodDaysCount / total
            val modFrac = analytics.moderateDaysCount / total
            val unhFrac = analytics.unhealthyDaysCount / total
            val vUnhFrac = analytics.veryUnhealthyDaysCount / total

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                shape = RoundedCornerShape(5.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (goodFrac > 0) Box(modifier = Modifier.weight(goodFrac).fillMaxHeight().background(HazeGreenGood))
                    if (modFrac > 0) Box(modifier = Modifier.weight(modFrac).fillMaxHeight().background(HazeOrangeModerate))
                    if (unhFrac > 0) Box(modifier = Modifier.weight(unhFrac).fillMaxHeight().background(HazeOrangeUnhealthy))
                    if (vUnhFrac > 0) Box(modifier = Modifier.weight(vUnhFrac).fillMaxHeight().background(HazeRedVeryUnhealthy))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Distribution Legend Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DistributionPill(label = "Good", count = analytics.goodDaysCount, color = HazeGreenGood)
                DistributionPill(label = "Moderate", count = analytics.moderateDaysCount, color = HazeOrangeModerate)
                DistributionPill(label = "Unhealthy", count = analytics.unhealthyDaysCount, color = HazeOrangeUnhealthy)
                if (analytics.veryUnhealthyDaysCount > 0) {
                    DistributionPill(label = "Severe", count = analytics.veryUnhealthyDaysCount, color = HazeRedVeryUnhealthy)
                }
            }
        }
    }
}

@Composable
fun DistributionPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = "$count days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DailyHistoryLogCard(entry: HistoricalHazeEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (entry.isTransboundarySpike) GeoOrangeLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.dateFormatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.isTransboundarySpike) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GeoOrangeBg
                        ) {
                            Text(
                                text = "HAZE SPIKE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = GeoOrangeDark,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = entry.level.containerColor
                ) {
                    Text(
                        text = "${entry.avgApi} API (${entry.level.title})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = entry.level.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${entry.stationName} • PM2.5: ${entry.pm25}µg/m³ • PM10: ${entry.pm10}µg/m³",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Range: ${entry.minApi} - ${entry.maxApi}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entry.monsoonSeasonNote?.let { note ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ℹ $note",
                    fontSize = 10.sp,
                    color = if (entry.isTransboundarySpike) GeoOrangeDark else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomRangeSelectorDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(45) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Date Range", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select historical analysis window: $selectedDays days ago to today")
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = selectedDays.toFloat(),
                    onValueChange = { selectedDays = it.roundToInt() },
                    valueRange = 5f..365f,
                    steps = 18,
                    colors = SliderDefaults.colors(thumbColor = GeoOrange, activeTrackColor = GeoOrange)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5 days", fontSize = 10.sp)
                    Text("180 days", fontSize = 10.sp)
                    Text("365 days", fontSize = 10.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDays) },
                colors = ButtonDefaults.buttonColors(containerColor = GeoOrange)
            ) {
                Text("Apply Range", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
