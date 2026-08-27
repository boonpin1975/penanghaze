package com.example.ui.screens

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
import com.example.data.local.HazeAlertEntity
import com.example.data.model.CustomAlertThresholds
import com.example.data.model.HourlyForecast
import com.example.data.model.UserHealthProfile
import com.example.ui.HazeUiState
import com.example.ui.components.CardContainer
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun AlertsForecastScreen(
    state: HazeUiState,
    onUpdateCustomThresholds: (Int, Int, Int, Boolean, Boolean, Boolean) -> Unit,
    onResetThresholds: () -> Unit,
    onTogglePush: (Boolean) -> Unit,
    onSimulateSpike: (Int) -> Unit,
    onResetSpike: () -> Unit,
    onClearAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("alerts_forecast_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Customizable Alert Thresholds Card (Good, Moderate, Unhealthy)
        item {
            CustomizableAlertThresholdsCard(
                thresholds = state.userProfile.customThresholds,
                pushEnabled = state.userProfile.pushAlertsEnabled,
                onUpdate = onUpdateCustomThresholds,
                onReset = onResetThresholds,
                onTogglePush = onTogglePush
            )
        }

        // 2. 24-Hour Forecast Chart Card
        item {
            Forecast24HCard(hourlyForecast = state.hourlyForecast)
        }

        // 3. Real-Time Alert Simulation Playground Card
        item {
            AlertSimulationCard(
                isSimulating = state.isSimulatedSpike,
                simulatedValue = state.simulatedSpikeValue,
                customThresholds = state.userProfile.customThresholds,
                onSimulate = onSimulateSpike,
                onReset = onResetSpike
            )
        }

        // 4. Alert History Header & Clear Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME ALERT LOGS (${state.alertsList.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.alertsList.isNotEmpty()) {
                    TextButton(onClick = onClearAlerts) {
                        Text("Clear All", fontSize = 12.sp, color = GeoOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Alert History Items
        if (state.alertsList.isEmpty()) {
            item {
                CardContainer(shape = RoundedCornerShape(24.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No threshold breach alerts logged",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(state.alertsList, key = { it.id }) { alert ->
                AlertLogItemCard(alert = alert)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CustomizableAlertThresholdsCard(
    thresholds: CustomAlertThresholds,
    pushEnabled: Boolean,
    onUpdate: (Int, Int, Int, Boolean, Boolean, Boolean) -> Unit,
    onReset: () -> Unit,
    onTogglePush: (Boolean) -> Unit
) {
    var goodLimit by remember(thresholds.goodLimit) { mutableStateOf(thresholds.goodLimit) }
    var moderateLimit by remember(thresholds.moderateLimit) { mutableStateOf(thresholds.moderateLimit) }
    var unhealthyLimit by remember(thresholds.unhealthyLimit) { mutableStateOf(thresholds.unhealthyLimit) }
    var notifyMod by remember(thresholds.notifyOnModerate) { mutableStateOf(thresholds.notifyOnModerate) }
    var notifyUnh by remember(thresholds.notifyOnUnhealthy) { mutableStateOf(thresholds.notifyOnUnhealthy) }
    var notifyHaz by remember(thresholds.notifyOnHazardous) { mutableStateOf(thresholds.notifyOnHazardous) }

    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOMIZABLE ALERT THRESHOLDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onReset) {
                    Text("Reset Standards", fontSize = 11.sp, color = GeoOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Set your preferred cutoffs for Good, Moderate, and Unhealthy AQI categories. Live notifications and health advisories will adapt to your personalized sensitivity:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Spectrum Preview Bar
            Text(
                text = "PERSONALIZED AQI SPECTRUM PREVIEW",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    val goodFraction = (goodLimit / 300f).coerceIn(0.1f, 0.4f)
                    val modFraction = ((moderateLimit - goodLimit) / 300f).coerceIn(0.1f, 0.4f)
                    val unhFraction = ((unhealthyLimit - moderateLimit) / 300f).coerceIn(0.1f, 0.4f)
                    val hazFraction = (1f - goodFraction - modFraction - unhFraction).coerceAtLeast(0.1f)

                    Box(modifier = Modifier.weight(goodFraction).fillMaxHeight().background(HazeGreenGood))
                    Box(modifier = Modifier.weight(modFraction).fillMaxHeight().background(HazeOrangeModerate))
                    Box(modifier = Modifier.weight(unhFraction).fillMaxHeight().background(HazeOrangeUnhealthy))
                    Box(modifier = Modifier.weight(hazFraction).fillMaxHeight().background(HazeRedVeryUnhealthy))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 - $goodLimit (Good)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HazeGreenGood)
                Text("${goodLimit + 1} - $moderateLimit (Moderate)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HazeOrangeModerate)
                Text("${moderateLimit + 1} - $unhealthyLimit (Unhealthy)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HazeOrangeUnhealthy)
                Text(">$unhealthyLimit (Severe)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HazeRedVeryUnhealthy)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // 1. Slider: Good Air Quality Max Limit
            ThresholdSliderRow(
                categoryName = "Good Air Quality Ceiling",
                currentValue = goodLimit,
                color = HazeGreenGood,
                valueRange = 25f..75f,
                steps = 9,
                onValueChange = {
                    goodLimit = it
                    if (moderateLimit <= goodLimit) moderateLimit = goodLimit + 15
                    if (unhealthyLimit <= moderateLimit) unhealthyLimit = moderateLimit + 30
                    onUpdate(goodLimit, moderateLimit, unhealthyLimit, notifyMod, notifyUnh, notifyHaz)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Slider: Moderate Air Quality Max Limit
            ThresholdSliderRow(
                categoryName = "Moderate Air Quality Ceiling",
                currentValue = moderateLimit,
                color = HazeOrangeModerate,
                valueRange = 60f..150f,
                steps = 17,
                onValueChange = {
                    moderateLimit = it
                    if (goodLimit >= moderateLimit) goodLimit = moderateLimit - 15
                    if (unhealthyLimit <= moderateLimit) unhealthyLimit = moderateLimit + 30
                    onUpdate(goodLimit, moderateLimit, unhealthyLimit, notifyMod, notifyUnh, notifyHaz)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Slider: Unhealthy Air Quality Max Limit
            ThresholdSliderRow(
                categoryName = "Unhealthy Air Quality Ceiling",
                currentValue = unhealthyLimit,
                color = HazeOrangeUnhealthy,
                valueRange = 120f..300f,
                steps = 17,
                onValueChange = {
                    unhealthyLimit = it
                    if (moderateLimit >= unhealthyLimit) moderateLimit = unhealthyLimit - 30
                    onUpdate(goodLimit, moderateLimit, unhealthyLimit, notifyMod, notifyUnh, notifyHaz)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Notification Trigger Switches
            Text(
                text = "PERSONALIZED PUSH NOTIFICATION TRIGGERS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            NotificationTriggerRow(
                label = "Alert when air crosses into Moderate (> $goodLimit API)",
                subtitle = "Recommended for asthma, respiratory & elderly users",
                color = HazeOrangeModerate,
                checked = notifyMod,
                onCheckedChange = {
                    notifyMod = it
                    onUpdate(goodLimit, moderateLimit, unhealthyLimit, notifyMod, notifyUnh, notifyHaz)
                }
            )

            NotificationTriggerRow(
                label = "Alert when air crosses into Unhealthy (> $moderateLimit API)",
                subtitle = "Mandates N95 masks & cancellation of outdoor sports",
                color = HazeOrangeUnhealthy,
                checked = notifyUnh,
                onCheckedChange = {
                    notifyUnh = it
                    onUpdate(goodLimit, moderateLimit, unhealthyLimit, notifyMod, notifyUnh, notifyHaz)
                }
            )

            NotificationTriggerRow(
                label = "Emergency Alert for Severe / Hazardous (> $unhealthyLimit API)",
                subtitle = "School closures, air filtration & emergency advisory",
                color = HazeRedVeryUnhealthy,
                checked = notifyHaz,
                onCheckedChange = {
                    notifyHaz = it
                    onUpdate(goodLimit, moderateLimit, unhealthyLimit, notifyMod, notifyUnh, notifyHaz)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Master Push Notification Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Master Real-Time Push Alerts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enable background sensor monitoring in Penang",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pushEnabled,
                    onCheckedChange = onTogglePush,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = GeoOrange,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun ThresholdSliderRow(
    categoryName: String,
    currentValue: Int,
    color: Color,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = categoryName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "$currentValue API",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Slider(
            value = currentValue.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun NotificationTriggerRow(
    label: String,
    subtitle: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = color,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun Forecast24HCard(hourlyForecast: List<HourlyForecast>) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "24-HOUR HAZE PROJECTION",
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
                        text = "HOURLY API",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                hourlyForecast.forEach { hour ->
                    val barHeightFraction = (hour.apiValue / 250f).coerceIn(0.15f, 1f)
                    val barColor = hour.hazeLevel.color

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(38.dp)
                    ) {
                        Text(
                            text = "${hour.apiValue}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            modifier = Modifier
                                .width(18.dp)
                                .height(80.dp),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(barHeightFraction),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                    color = barColor
                                ) {}
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = hour.timeLabel,
                            fontSize = 10.sp,
                            fontWeight = if (hour.isCurrentHour) FontWeight.Black else FontWeight.Normal,
                            color = if (hour.isCurrentHour) GeoOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertSimulationCard(
    isSimulating: Boolean,
    simulatedValue: Int?,
    customThresholds: CustomAlertThresholds,
    onSimulate: (Int) -> Unit,
    onReset: () -> Unit
) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME ALERT TESTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSimulating) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GeoOrange
                    ) {
                        Text(
                            text = "SIM ACTIVE ($simulatedValue API)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Trigger test spikes to evaluate your personalized Good (${customThresholds.goodLimit}), Moderate (${customThresholds.moderateLimit}), and Unhealthy (${customThresholds.unhealthyLimit}) notification alerts:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulate(customThresholds.goodLimit + 15) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HazeOrangeModerate)
                ) {
                    Text("API ${customThresholds.goodLimit + 15}\n(Moderate)", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                }

                OutlinedButton(
                    onClick = { onSimulate(customThresholds.moderateLimit + 25) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HazeOrangeUnhealthy)
                ) {
                    Text("API ${customThresholds.moderateLimit + 25}\n(Unhealthy)", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                }

                OutlinedButton(
                    onClick = { onSimulate(customThresholds.unhealthyLimit + 35) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HazeRedVeryUnhealthy)
                ) {
                    Text("API ${customThresholds.unhealthyLimit + 35}\n(Severe)", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                }
            }

            if (isSimulating) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoOrange,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset to Live Penang Sensors", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AlertLogItemCard(alert: HazeAlertEntity) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(alert.timestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GeoOrangeBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoOrangeLight),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NotificationImportant,
                        contentDescription = "Alert",
                        tint = GeoOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alert.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = alert.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
