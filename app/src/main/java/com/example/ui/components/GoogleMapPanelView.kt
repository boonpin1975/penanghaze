package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.webkit.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.location.PenangStationsData
import com.example.data.model.HazeLevel
import com.example.data.model.PenangStation
import com.example.ui.theme.*
import java.util.Locale

enum class GoogleMapType(val displayName: String, val code: String) {
    ROADMAP("Roadmap", "m"),
    SATELLITE("Satellite", "k"),
    TERRAIN("Terrain", "p"),
    HYBRID("Hybrid", "h")
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapPanelView(
    stations: List<PenangStation>,
    stationReadings: Map<String, Int>,
    selectedStation: PenangStation?,
    onSelectStation: (PenangStation?) -> Unit,
    onSetActiveStation: (PenangStation) -> Unit = {},
    onViewStationHistory: (PenangStation) -> Unit = {},
    isHeatmapEnabled: Boolean = false,
    districtFilter: String = "ALL",
    mapType: GoogleMapType = GoogleMapType.ROADMAP,
    onMapTypeChange: (GoogleMapType) -> Unit = {},
    onToggleHeatmap: () -> Unit = {},
    userLatitude: Double? = 5.4164,
    userLongitude: Double? = 100.3327,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showAllStationsSheet by remember { mutableStateOf(false) }
    var currentMapTypeCode by remember { mutableStateOf(mapType.code) }

    val filteredStations = remember(stations, districtFilter) {
        when (districtFilter) {
            "ISLAND" -> stations.filter { it.district.contains("Island", ignoreCase = true) }
            "MAINLAND" -> stations.filter { it.district.contains("Mainland", ignoreCase = true) }
            else -> stations
        }
    }

    // Google Maps Embed URL calculation based on active selection
    val googleMapsUrl = remember(selectedStation, currentMapTypeCode) {
        if (selectedStation != null) {
            "https://maps.google.com/maps?q=${selectedStation.latitude},${selectedStation.longitude}&hl=en&t=$currentMapTypeCode&z=15&output=embed"
        } else {
            "https://maps.google.com/maps?q=Penang+Island,+Penang,+Malaysia&hl=en&t=$currentMapTypeCode&z=11&output=embed"
        }
    }

    LaunchedEffect(googleMapsUrl) {
        webViewRef?.loadUrl(googleMapsUrl)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_map_panel_container")
    ) {
        // Direct Google Maps Interactive Engine
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    val bgColor = if (isDarkTheme) 0xFF0F172A.toInt() else 0xFFF1F5F9.toInt()
                    setBackgroundColor(bgColor)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true
                        blockNetworkImage = false
                        blockNetworkLoads = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("intent:") || url.startsWith("geo:") || url.contains("maps.app.goo.gl")) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                                return true
                            }
                            return false
                        }
                    }

                    loadUrl(googleMapsUrl)
                }
            },
            update = { view ->
                webViewRef = view
            }
        )

        // Top Horizontal Station Quick Select Carousel
        Surface(
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 58.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All / Reset Penang button
                FilterChip(
                    selected = selectedStation == null,
                    onClick = { onSelectStation(null) },
                    label = { Text("Penang View", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeoOrange,
                        selectedLabelColor = PureWhite
                    )
                )

                // Station Chips with API values
                filteredStations.forEach { st ->
                    val api = stationReadings[st.id] ?: 52
                    val level = HazeLevel.fromApi(api)
                    val chipColor = when (level) {
                        HazeLevel.GOOD -> HazeGreenGood
                        HazeLevel.MODERATE -> HazeOrangeModerate
                        HazeLevel.UNHEALTHY -> HazeOrangeUnhealthy
                        HazeLevel.VERY_UNHEALTHY -> HazeRedVeryUnhealthy
                        HazeLevel.HAZARDOUS -> HazeHazardous
                    }

                    val isSelected = selectedStation?.id == st.id

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) chipColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clickable { onSelectStation(st) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = st.name.replace("Penang", "").replace("Station", "").trim(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) PureWhite.copy(alpha = 0.25f) else chipColor
                            ) {
                                Text(
                                    text = "$api",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top-Right Quick Map Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 115.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Station Directory List
            FloatingActionButton(
                onClick = { showAllStationsSheet = true },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeoBlue,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "Stations List",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Map Layer Toggle (Roadmap vs Satellite)
            FloatingActionButton(
                onClick = {
                    currentMapTypeCode = if (currentMapTypeCode == "m") "k" else "m"
                },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (currentMapTypeCode == "k") Icons.Default.Map else Icons.Default.Satellite,
                    contentDescription = "Toggle Satellite",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Open in Google Maps App
            FloatingActionButton(
                onClick = {
                    val lat = selectedStation?.latitude ?: 5.4164
                    val lon = selectedStation?.longitude ?: 100.3327
                    val label = selectedStation?.name ?: "Penang"
                    val gmmIntentUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon"))
                        context.startActivity(browserIntent)
                    }
                },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeoGreen,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Navigation, contentDescription = "Google Maps App", modifier = Modifier.size(18.dp))
            }

            // Recenter Penang
            FloatingActionButton(
                onClick = {
                    onSelectStation(null)
                    val url = "https://maps.google.com/maps?q=Penang+Island,+Penang,+Malaysia&hl=en&t=$currentMapTypeCode&z=11&output=embed"
                    webViewRef?.loadUrl(url)
                },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeoOrange,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Penang", modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Selected Station Floating Card
        if (selectedStation != null) {
            val api = stationReadings[selectedStation.id] ?: 52
            val level = HazeLevel.fromApi(api)
            val color = when (level) {
                HazeLevel.GOOD -> HazeGreenGood
                HazeLevel.MODERATE -> HazeOrangeModerate
                HazeLevel.UNHEALTHY -> HazeOrangeUnhealthy
                HazeLevel.VERY_UNHEALTHY -> HazeRedVeryUnhealthy
                HazeLevel.HAZARDOUS -> HazeHazardous
            }

            val dist = if (userLatitude != null && userLongitude != null) {
                PenangStationsData.calculateDistanceKm(userLatitude, userLongitude, selectedStation.latitude, selectedStation.longitude)
            } else null

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.5.dp, color),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .testTag("station_map_bottom_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedStation.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (selectedStation.isOfficialCAQM) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "DOE CAQM",
                                        tint = GeoBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "📍 ${selectedStation.landmark} • ${selectedStation.district}${if (dist != null) " (%.1f km)".format(Locale.US, dist) else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = color
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$api",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureWhite
                                )
                                Text(
                                    text = "API",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSetActiveStation(selectedStation) },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Active", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=${selectedStation.latitude},${selectedStation.longitude}&mode=d")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${selectedStation.latitude},${selectedStation.longitude}"))
                                    context.startActivity(browserIntent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        IconButton(
                            onClick = { onSelectStation(null) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // All Stations Bottom Sheet
        if (showAllStationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAllStationsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Penang Air Monitoring Stations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredStations.size} continuous monitoring stations on Google Maps",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxHeight(0.6f)
                    ) {
                        items(filteredStations) { st ->
                            val stApi = stationReadings[st.id] ?: 52
                            val stLevel = HazeLevel.fromApi(stApi)
                            val stColor = when (stLevel) {
                                HazeLevel.GOOD -> HazeGreenGood
                                HazeLevel.MODERATE -> HazeOrangeModerate
                                HazeLevel.UNHEALTHY -> HazeOrangeUnhealthy
                                HazeLevel.VERY_UNHEALTHY -> HazeRedVeryUnhealthy
                                HazeLevel.HAZARDOUS -> HazeHazardous
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    if (selectedStation?.id == st.id) 2.dp else 1.dp,
                                    if (selectedStation?.id == st.id) GeoOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectStation(st)
                                        showAllStationsSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = st.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "📍 ${st.landmark} • ${st.district}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = stColor
                                    ) {
                                        Text(
                                            text = "$stApi API",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PureWhite,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
