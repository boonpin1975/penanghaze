package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AirQualityReading
import com.example.data.model.HazeLevel
import com.example.ui.theme.*

@Composable
fun HazeGauge(
    reading: AirQualityReading,
    modifier: Modifier = Modifier
) {
    val targetSweep = (reading.apiValue.toFloat() / 350f).coerceIn(0.08f, 1f) * 360f
    val animatedSweep = remember { Animatable(0f) }

    LaunchedEffect(reading.apiValue) {
        animatedSweep.animateTo(
            targetValue = targetSweep,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val statusColor = when (reading.hazeLevel) {
        HazeLevel.GOOD -> GeoGreen
        HazeLevel.MODERATE -> GeoOrange
        HazeLevel.UNHEALTHY -> GeoOrangeDark
        HazeLevel.VERY_UNHEALTHY -> HazeRedVeryUnhealthy
        HazeLevel.HAZARDOUS -> HazeHazardous
    }

    val trackBgColor = when (reading.hazeLevel) {
        HazeLevel.GOOD -> GeoGreenLight.copy(alpha = 0.6f)
        HazeLevel.MODERATE -> GeoOrangeLight.copy(alpha = 0.6f)
        HazeLevel.UNHEALTHY -> Color(0xFFFFEDD5)
        HazeLevel.VERY_UNHEALTHY -> Color(0xFFFEE2E2)
        HazeLevel.HAZARDOUS -> Color(0xFFFCE7F3)
    }

    CardContainer(
        modifier = modifier.testTag("haze_gauge_card"),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${reading.station.name}, Penang",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Geometric Circle Gauge with Floating Bottom Pill
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circle Canvas
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val startAngle = -90f

                    // Background Track
                    drawArc(
                        color = trackBgColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth)
                    )

                    // Active Progress Ring
                    drawArc(
                        color = statusColor,
                        startAngle = startAngle,
                        sweepAngle = animatedSweep.value,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Inner Reading Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${reading.apiValue}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "API",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor.copy(alpha = 0.8f),
                        letterSpacing = 2.sp
                    )
                }

                // Floating Status Pill at bottom
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    color = statusColor
                ) {
                    Text(
                        text = reading.hazeLevel.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Concise Health Summary sentence
            Text(
                text = when (reading.hazeLevel) {
                    HazeLevel.GOOD -> "Air quality is considered satisfactory, and air pollution poses little or no risk."
                    HazeLevel.MODERATE -> "Air quality is acceptable; however, there may be a risk for sensitive individuals."
                    HazeLevel.UNHEALTHY -> "Members of sensitive groups may experience health effects. General public should limit exertion."
                    HazeLevel.VERY_UNHEALTHY -> "Health alert: Everyone may experience more serious health effects. Stay indoors."
                    HazeLevel.HAZARDOUS -> "Health warnings of emergency conditions. Entire population is likely to be affected."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Geometric Metric Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "PM2.5",
                    value = "${reading.pm25}",
                    unit = "µg/m³",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "PM10",
                    value = "${reading.pm10}",
                    unit = "µg/m³",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "OZONE",
                    value = "${reading.o3}",
                    unit = "µg/m³",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        content = content
    )
}
