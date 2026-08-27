package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.PenangStationsData
import com.example.data.model.AirQualityReading
import com.example.data.model.HazeLevel
import com.example.data.model.PenangStation
import com.example.ui.theme.*
import kotlin.math.sqrt

// Penang Bounding Box
private const val MIN_LAT = 5.12
private const val MAX_LAT = 5.50
private const val MIN_LON = 100.16
private const val MAX_LON = 100.54

@OptIn(ExperimentalTextApi::class)
@Composable
fun PenangInteractiveMapView(
    stations: List<PenangStation>,
    stationReadings: Map<String, Int>, // station.id -> apiValue
    selectedStation: PenangStation?,
    onSelectStation: (PenangStation) -> Unit,
    isHeatmapEnabled: Boolean,
    districtFilter: String, // ALL, ISLAND, MAINLAND
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Pulsing animation for selected station marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val textMeasurer = rememberTextMeasurer()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val waterColor = if (isDark) Color(0xFF0F1A24) else Color(0xFFE2EDF8)
    val landIslandColor = if (isDark) Color(0xFF1E2833) else Color(0xFFD6E4DB)
    val landMainlandColor = if (isDark) Color(0xFF1B242E) else Color(0xFFD0DDD6)
    val bridgeColor = if (isDark) Color(0xFF4A5568) else Color(0xFF94A3B8)

    val filteredStations = remember(stations, districtFilter) {
        when (districtFilter) {
            "ISLAND" -> stations.filter { it.district.contains("Island", ignoreCase = true) }
            "MAINLAND" -> stations.filter { it.district.contains("Mainland", ignoreCase = true) }
            else -> stations
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(waterColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 3.5f)
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-600f * scale, 600f * scale),
                        y = (offset.y + pan.y).coerceIn(-600f * scale, 600f * scale)
                    )
                }
            }
            .pointerInput(filteredStations, scale, offset) {
                detectTapGestures { tapOffset ->
                    val w = size.width
                    val h = size.height

                    // Check which station marker is closest to tap
                    var closestStation: PenangStation? = null
                    var closestDist = Float.MAX_VALUE

                    filteredStations.forEach { station ->
                        val normX = (station.longitude - MIN_LON) / (MAX_LON - MIN_LON)
                        val normY = 1.0 - (station.latitude - MIN_LAT) / (MAX_LAT - MIN_LAT)

                        val cx = w * 0.1f + normX.toFloat() * (w * 0.8f)
                        val cy = h * 0.08f + normY.toFloat() * (h * 0.84f)

                        // Apply transform
                        val screenX = (cx - w / 2) * scale + w / 2 + offset.x
                        val screenY = (cy - h / 2) * scale + h / 2 + offset.y

                        val dx = tapOffset.x - screenX
                        val dy = tapOffset.y - screenY
                        val dist = sqrt(dx * dx + dy * dy)

                        if (dist < 40.dp.toPx() && dist < closestDist) {
                            closestDist = dist
                            closestStation = station
                        }
                    }

                    closestStation?.let { onSelectStation(it) }
                }
            }
    ) {
        // Main Map Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Helper to project GPS coordinates to untransformed canvas coordinates
            fun project(lat: Double, lon: Double): Offset {
                val normX = (lon - MIN_LON) / (MAX_LON - MIN_LON)
                val normY = 1.0 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT)
                val cx = w * 0.1f + normX.toFloat() * (w * 0.8f)
                val cy = h * 0.08f + normY.toFloat() * (h * 0.84f)

                // Apply zoom and pan transform relative to canvas center
                val sx = (cx - w / 2) * scale + w / 2 + offset.x
                val sy = (cy - h / 2) * scale + h / 2 + offset.y
                return Offset(sx, sy)
            }

            // 1. Draw Penang Island Landmass
            val islandPath = Path().apply {
                val p1 = project(5.47, 100.20) // Teluk Bahang (NW)
                val p2 = project(5.47, 100.28) // Tanjung Bungah (N)
                val p3 = project(5.43, 100.32) // Gurney / George Town (NE)
                val p4 = project(5.41, 100.34) // George Town Pier
                val p5 = project(5.35, 100.31) // Gelugor (E)
                val p6 = project(5.29, 100.28) // Bayan Lepas (SE)
                val p7 = project(5.26, 100.27) // Batu Maung (S)
                val p8 = project(5.26, 100.20) // Teluk Kumbar (SW)
                val p9 = project(5.35, 100.20) // Balik Pulau (W)
                val p10 = project(5.43, 100.19) // Pantai Acheh

                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                lineTo(p5.x, p5.y)
                lineTo(p6.x, p6.y)
                lineTo(p7.x, p7.y)
                lineTo(p8.x, p8.y)
                lineTo(p9.x, p9.y)
                lineTo(p10.x, p10.y)
                close()
            }
            drawPath(path = islandPath, color = landIslandColor)
            drawPath(
                path = islandPath,
                color = if (isDark) Color(0xFF2D3B48) else Color(0xFFB5C9BE),
                style = Stroke(width = 2.dp.toPx() * scale.coerceIn(0.8f, 1.5f))
            )

            // 2. Draw Penang Mainland (Seberang Perai) Landmass
            val mainlandPath = Path().apply {
                val m1 = project(5.50, 100.36) // Penaga / Kepala Batas (N)
                val m2 = project(5.40, 100.36) // Butterworth Bagan
                val m3 = project(5.37, 100.38) // Prai Industrial
                val m4 = project(5.34, 100.41) // Juru
                val m5 = project(5.26, 100.43) // Batu Kawan
                val m6 = project(5.14, 100.48) // Nibong Tebal (S)
                val m7 = project(5.14, 100.54) // East Border S
                val m8 = project(5.50, 100.54) // East Border N

                moveTo(m1.x, m1.y)
                lineTo(m2.x, m2.y)
                lineTo(m3.x, m3.y)
                lineTo(m4.x, m4.y)
                lineTo(m5.x, m5.y)
                lineTo(m6.x, m6.y)
                lineTo(m7.x, m7.y)
                lineTo(m8.x, m8.y)
                close()
            }
            drawPath(path = mainlandPath, color = landMainlandColor)
            drawPath(
                path = mainlandPath,
                color = if (isDark) Color(0xFF2A3644) else Color(0xFFB0C4B8),
                style = Stroke(width = 2.dp.toPx() * scale.coerceIn(0.8f, 1.5f))
            )

            // 3. Draw Penang Bridges
            // First Bridge (Gelugor <-> Prai)
            val b1Start = project(5.353, 100.310)
            val b1End = project(5.362, 100.385)
            drawLine(
                color = bridgeColor,
                start = b1Start,
                end = b1End,
                strokeWidth = 3.5.dp.toPx() * scale.coerceIn(0.8f, 1.5f),
                cap = StrokeCap.Round
            )

            // Second Bridge (Sultan Abdul Halim Muadzam Shah Bridge: Batu Maung <-> Batu Kawan)
            val b2Start = project(5.275, 100.275)
            val b2End = project(5.265, 100.430)
            drawLine(
                color = bridgeColor,
                start = b2Start,
                end = b2End,
                strokeWidth = 3.5.dp.toPx() * scale.coerceIn(0.8f, 1.5f),
                cap = StrokeCap.Round
            )

            // 4. Heatmap dispersion overlays (if enabled)
            if (isHeatmapEnabled) {
                filteredStations.forEach { station ->
                    val pos = project(station.latitude, station.longitude)
                    val api = stationReadings[station.id] ?: 52
                    val level = HazeLevel.fromApi(api)
                    val heatColor = level.color

                    val heatRadius = (45.dp.toPx() * scale).coerceIn(30f, 140f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                heatColor.copy(alpha = 0.38f),
                                heatColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = pos,
                            radius = heatRadius
                        ),
                        radius = heatRadius,
                        center = pos
                    )
                }
            }

            // 5. Draw Station Markers
            filteredStations.forEach { station ->
                val pos = project(station.latitude, station.longitude)
                val isSelected = selectedStation?.id == station.id
                val api = stationReadings[station.id] ?: 52
                val level = HazeLevel.fromApi(api)
                val markerColor = level.color

                // Pulse ring for selected station
                if (isSelected) {
                    drawCircle(
                        color = markerColor.copy(alpha = pulseAlpha),
                        radius = (pulseRadius.dp.toPx() * scale).coerceIn(16f, 60f),
                        center = pos,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Outer pin base shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = (11.dp.toPx() * scale.coerceIn(0.9f, 1.4f)),
                    center = Offset(pos.x, pos.y + 2.dp.toPx())
                )

                // White border
                drawCircle(
                    color = PureWhite,
                    radius = (10.dp.toPx() * scale.coerceIn(0.9f, 1.4f)),
                    center = pos
                )

                // Color inner core
                drawCircle(
                    color = markerColor,
                    radius = (8.dp.toPx() * scale.coerceIn(0.9f, 1.4f)),
                    center = pos
                )

                // API Value Badge on top
                val labelText = "$api"
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(labelText),
                    style = TextStyle(
                        fontSize = (10 * scale.coerceIn(0.85f, 1.3f)).sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )
                )

                val badgeWidth = (textLayout.size.width + 16.dp.toPx())
                val badgeHeight = (textLayout.size.height + 6.dp.toPx())
                val badgeTopLeft = Offset(
                    x = pos.x - badgeWidth / 2,
                    y = pos.y - (18.dp.toPx() * scale.coerceIn(0.9f, 1.4f)) - badgeHeight
                )

                // Badge Container
                drawRoundRect(
                    color = if (isDark) Color(0xFF111315) else Color(0xFF22262B),
                    topLeft = badgeTopLeft,
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = markerColor,
                    topLeft = badgeTopLeft,
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        x = badgeTopLeft.x + (badgeWidth - textLayout.size.width) / 2,
                        y = badgeTopLeft.y + (badgeHeight - textLayout.size.height) / 2
                    )
                )

                // Station name text below marker if zoomed in
                if (scale >= 1.2f || isSelected) {
                    val nameText = station.name.substringBefore("(")
                    val nameLayout = textMeasurer.measure(
                        text = AnnotatedString(nameText),
                        style = TextStyle(
                            fontSize = (9 * scale.coerceIn(0.85f, 1.2f)).sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDark) PureWhite else Color(0xFF111827)
                        )
                    )
                    drawText(
                        textLayoutResult = nameLayout,
                        topLeft = Offset(
                            x = pos.x - nameLayout.size.width / 2,
                            y = pos.y + (12.dp.toPx() * scale.coerceIn(0.9f, 1.4f))
                        )
                    )
                }
            }
        }

        // Top Floating Legend & Overlay Controls
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendPill(label = "Good", color = HazeGreenGood)
                LegendPill(label = "Moderate", color = HazeOrangeModerate)
                LegendPill(label = "Unhealthy", color = HazeOrangeUnhealthy)
            }
        }

        // Zoom & Reset Floating Buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale * 1.3f).coerceAtMost(3.5f) },
                modifier = Modifier.size(38.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
            }

            FloatingActionButton(
                onClick = { scale = (scale / 1.3f).coerceAtLeast(0.8f) },
                modifier = Modifier.size(38.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
            }

            FloatingActionButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier.size(38.dp),
                containerColor = GeoOrangeBg,
                contentColor = GeoOrange,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
