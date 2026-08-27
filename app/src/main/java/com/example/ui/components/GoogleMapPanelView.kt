package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

enum class GoogleMapType(val displayName: String, val code: String) {
    ROADMAP("Street", "m"),
    SATELLITE("Satellite", "y"),
    TERRAIN("Terrain", "p"),
    VOYAGER("Clean", "v"),
    OSM("OpenStreet", "osm")
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
    isHeatmapEnabled: Boolean,
    districtFilter: String,
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
    var isMapLoaded by remember { mutableStateOf(false) }
    var isDetailedLabels by remember { mutableStateOf(true) }
    var showAllStationsSheet by remember { mutableStateOf(false) }

    val filteredStations = remember(stations, districtFilter) {
        when (districtFilter) {
            "ISLAND" -> stations.filter { it.district.contains("Island", ignoreCase = true) }
            "MAINLAND" -> stations.filter { it.district.contains("Mainland", ignoreCase = true) }
            else -> stations
        }
    }

    // JSON serialization for stations with color coding, names, pollutants, coordinates and landmark
    val stationsJson = remember(filteredStations, stationReadings, selectedStation, userLatitude, userLongitude) {
        val array = JSONArray()
        filteredStations.forEach { station ->
            val api = stationReadings[station.id] ?: 52
            val level = HazeLevel.fromApi(api)
            val colorHex = when (level) {
                HazeLevel.GOOD -> "#16A34A"
                HazeLevel.MODERATE -> "#D97706"
                HazeLevel.UNHEALTHY -> "#EA580C"
                HazeLevel.VERY_UNHEALTHY -> "#DC2626"
                HazeLevel.HAZARDOUS -> "#7E22CE"
            }

            val dist = if (userLatitude != null && userLongitude != null) {
                PenangStationsData.calculateDistanceKm(userLatitude, userLongitude, station.latitude, station.longitude)
            } else 0.0

            val obj = JSONObject().apply {
                put("id", station.id)
                put("name", station.name)
                put("shortName", station.name.replace("Penang", "").replace("Station", "").trim())
                put("district", station.district)
                put("landmark", station.landmark)
                put("lat", station.latitude)
                put("lon", station.longitude)
                put("api", api)
                put("color", colorHex)
                put("level", level.title)
                put("levelMalay", level.titleMalay)
                put("isOfficial", station.isOfficialCAQM)
                put("isSelected", selectedStation?.id == station.id)
                put("distanceKm", String.format(Locale.US, "%.1f", dist))
                put("pm25", (api * 0.72 + 5.0).roundToInt())
                put("pm10", (api * 1.15 + 12.0).roundToInt())
                put("o3", (42.0 + (api % 15)).roundToInt())
                put("no2", (18.0 + (api % 10)).roundToInt())
            }
            array.put(obj)
        }
        array.toString()
    }

    // Sync station pins and atmospheric heatmap to WebView
    LaunchedEffect(stationsJson, isHeatmapEnabled, isMapLoaded, isDarkTheme, isDetailedLabels) {
        if (isMapLoaded) {
            val script = "if(window.updateMapState){ window.updateMapState($stationsJson, $isHeatmapEnabled, 'm', $isDarkTheme, $isDetailedLabels); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // Pan to selected station
    LaunchedEffect(selectedStation, isMapLoaded) {
        if (isMapLoaded && selectedStation != null) {
            val script = "if(window.panToStation){ window.panToStation(${selectedStation.latitude}, ${selectedStation.longitude}, '${selectedStation.id}'); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_map_panel_container")
    ) {
        // Embed Interactive Map WebView with touch handling
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    val bgColor = if (isDarkTheme) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt()
                    setBackgroundColor(bgColor)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                    }

                    // Prevent parent Compose scroll containers from intercepting map gestures
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

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            val initScript = "if(window.initPenangGoogleMap){ window.initPenangGoogleMap($stationsJson, $isHeatmapEnabled, 'm', $userLatitude, $userLongitude, $isDarkTheme, $isDetailedLabels); }"
                            view?.evaluateJavascript(initScript, null)
                        }
                    }

                    // JavaScript Bridge to Jetpack Compose
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onStationClicked(stationId: String) {
                                Handler(Looper.getMainLooper()).post {
                                    val st = stations.find { it.id == stationId }
                                    if (st != null) {
                                        onSelectStation(st)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onSetActiveZone(stationId: String) {
                                Handler(Looper.getMainLooper()).post {
                                    val st = stations.find { it.id == stationId }
                                    if (st != null) {
                                        onSetActiveStation(st)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onViewHistory(stationId: String) {
                                Handler(Looper.getMainLooper()).post {
                                    val st = stations.find { it.id == stationId }
                                    if (st != null) {
                                        onViewStationHistory(st)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onOpenExternalMaps(lat: Double, lon: Double, name: String) {
                                Handler(Looper.getMainLooper()).post {
                                    try {
                                        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(name)})")
                                        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                }
                            }
                        },
                        "AndroidBridge"
                    )

                    val htmlContent = generateSelfContainedGoogleMapHtml(isDarkTheme)
                    loadDataWithBaseURL(
                        "https://maps.google.com/",
                        htmlContent,
                        "text/html; charset=UTF-8",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { view ->
                webViewRef = view
            }
        )

        // Top-Left Floating Live Stations Badge Overlay
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 64.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = GeoOrange,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Street Map",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• ${filteredStations.size} Stations Overlay",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GeoOrange
                )
            }
        }

        // Top-Right Quick Map Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // All Stations Full Directory Modal Button
            FloatingActionButton(
                onClick = { showAllStationsSheet = true },
                modifier = Modifier.size(38.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeoBlue,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "Show All Stations Information",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Toggle Detailed Labels Mode on Map Pins
            FloatingActionButton(
                onClick = {
                    isDetailedLabels = !isDetailedLabels
                },
                modifier = Modifier.size(38.dp),
                containerColor = if (isDetailedLabels) GeoBlue else MaterialTheme.colorScheme.surface,
                contentColor = if (isDetailedLabels) PureWhite else MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isDetailedLabels) Icons.Default.PinDrop else Icons.Default.LocationOff,
                    contentDescription = "Toggle Detailed Station Labels",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Heatmap Toggle Button (Large, prominent)
            FloatingActionButton(
                onClick = {
                    onToggleHeatmap()
                },
                modifier = Modifier.size(38.dp),
                containerColor = if (isHeatmapEnabled) GeoOrange else MaterialTheme.colorScheme.surface,
                contentColor = if (isHeatmapEnabled) PureWhite else MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isHeatmapEnabled) Icons.Default.Layers else Icons.Default.LayersClear,
                    contentDescription = "Toggle Atmospheric Heatmap",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Zoom In
            FloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript("if(window.mapZoomIn) window.mapZoomIn();", null)
                },
                modifier = Modifier.size(36.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
            }

            // Zoom Out
            FloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript("if(window.mapZoomOut) window.mapZoomOut();", null)
                },
                modifier = Modifier.size(36.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
            }

            // Recenter Penang
            FloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript("if(window.resetPenangView) window.resetPenangView();", null)
                },
                modifier = Modifier.size(36.dp),
                containerColor = GeoOrangeBg,
                contentColor = GeoOrange,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Penang", modifier = Modifier.size(16.dp))
            }

            // Center My Location
            if (userLatitude != null && userLongitude != null) {
                FloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if(window.panToMyLocation) window.panToMyLocation();", null)
                    },
                    modifier = Modifier.size(36.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GeoBlue,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My GPS Position", modifier = Modifier.size(16.dp))
                }
            }

            // Open in Google Maps App
            FloatingActionButton(
                onClick = {
                    try {
                        val geoUri = Uri.parse("geo:5.3600,100.3000?q=5.3600,100.3000(Penang+Air+Quality+Stations)")
                        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val webUri = Uri.parse("https://www.google.com/maps/@5.3600,100.3000,11z")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.size(36.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeoGreen,
                shape = CircleShape
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open in Google Maps", modifier = Modifier.size(16.dp))
            }
        }

        // Atmospheric Heatmap & AQI Gradient Legend Bar at Bottom-Left (above card)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedStation != null) 300.dp else 84.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Live Atmospheric Heatmap Active Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isHeatmapEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isHeatmapEnabled) GeoOrange.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { onToggleHeatmap() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isHeatmapEnabled) GeoOrange else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHeatmapEnabled) "ATMOSPHERIC HEATMAP: ON" else "ATMOSPHERIC HEATMAP: OFF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = if (isHeatmapEnabled) GeoOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isHeatmapEnabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SW 14 km/h ↗",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Continuous Spectrum Legend Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Continuous AQI Gradient Strip
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        GeoGreen,
                                        GeoOrange,
                                        GeoOrangeDark,
                                        HazeRedVeryUnhealthy,
                                        HazeHazardous
                                    )
                                ),
                                RoundedCornerShape(3.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.width(180.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "0 Good", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = GeoGreen)
                        Text(text = "50 Mod", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = GeoOrange)
                        Text(text = "100 Unh", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = GeoOrangeDark)
                        Text(text = "200+ Haz", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = HazeRedVeryUnhealthy)
                    }
                }
            }
        }

        // All 11 Stations Full Information Bottom Sheet
        if (showAllStationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAllStationsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "All Penang Stations Directory",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${stations.size} Official CAQM & Sensor Stations across Penang",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showAllStationsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(stations, key = { it.id }) { st ->
                            val api = stationReadings[st.id] ?: 52
                            val level = HazeLevel.fromApi(api)
                            val levelColor = when (level) {
                                HazeLevel.GOOD -> GeoGreen
                                HazeLevel.MODERATE -> GeoOrange
                                HazeLevel.UNHEALTHY -> GeoOrangeDark
                                HazeLevel.VERY_UNHEALTHY -> HazeRedVeryUnhealthy
                                HazeLevel.HAZARDOUS -> HazeHazardous
                            }
                            val dist = if (userLatitude != null && userLongitude != null) {
                                PenangStationsData.calculateDistanceKm(userLatitude, userLongitude, st.latitude, st.longitude)
                            } else 0.0

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, levelColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectStation(st)
                                        showAllStationsSheet = false
                                        webViewRef?.evaluateJavascript("if(window.panToStation){ window.panToStation(${st.latitude}, ${st.longitude}, '${st.id}'); }", null)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = levelColor.copy(alpha = 0.15f),
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "$api",
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = levelColor
                                                    )
                                                    Text(
                                                        text = "API",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = levelColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = st.name,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (st.isOfficialCAQM) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = GeoBlue.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "DOE CAQM",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = GeoBlue,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = st.district,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = levelColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = level.title,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = levelColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Landmark and coordinates
                                    Text(
                                        text = "📍 ${st.landmark}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🌐 ${st.latitude}°N, ${st.longitude}°E  •  ${String.format(Locale.US, "%.1f", dist)} km away",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilledTonalButton(
                                                onClick = {
                                                    onSelectStation(st)
                                                    showAllStationsSheet = false
                                                    webViewRef?.evaluateJavascript("if(window.panToStation){ window.panToStation(${st.latitude}, ${st.longitude}, '${st.id}'); }", null)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View on Map", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    }
}

/**
 * Slippy Map Engine with Full Station Information System,
 * Google Street / Satellite / Terrain / Voyager / OSM tiles,
 * SVG geographic fallback, and high-visibility Gaussian atmospheric dispersion heatmap plumes.
 */
private fun generateSelfContainedGoogleMapHtml(isDarkTheme: Boolean): String {
    val bgHex = if (isDarkTheme) "#0f172a" else "#e2edf8"
    val cardBg = if (isDarkTheme) "#1e293b" else "#ffffff"
    val cardText = if (isDarkTheme) "#f8fafc" else "#0f172a"
    val subText = if (isDarkTheme) "#94a3b8" else "#64748b"
    val borderHex = if (isDarkTheme) "rgba(255,255,255,0.12)" else "rgba(0,0,0,0.12)"
    val shadowHex = if (isDarkTheme) "rgba(0,0,0,0.65)" else "rgba(0,0,0,0.20)"

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; -webkit-user-select: none; user-select: none; }
        html, body { width: 100%; height: 100%; overflow: hidden; background: $bgHex; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        
        #map-container {
            width: 100%;
            height: 100%;
            position: absolute;
            top: 0; left: 0;
            overflow: hidden;
            background: $bgHex;
            touch-action: none;
            cursor: grab;
        }

        #basemap-fallback {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 1;
            pointer-events: none;
        }

        #tiles-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 5;
            pointer-events: none;
        }

        .map-tile {
            position: absolute;
            width: 256px;
            height: 256px;
            pointer-events: none;
            display: block;
        }

        /* Atmospheric Heatmap Canvas Layer */
        #heatmap-canvas {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 15;
            pointer-events: none;
        }

        /* Animated Wind Particles Canvas Layer */
        #wind-canvas {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 25;
            pointer-events: none;
        }

        #vectors-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 30;
            pointer-events: none;
        }

        #markers-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 100;
            pointer-events: none;
        }

        #infowindow-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            z-index: 300;
            pointer-events: none;
        }

        /* Station Marker Styles */
        .station-marker {
            position: absolute;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            pointer-events: auto;
            transform: translate(-50%, -100%);
            transition: transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
            z-index: 100;
        }
        
        .station-marker:hover, .station-marker.selected {
            transform: translate(-50%, -100%) scale(1.15);
            z-index: 200;
        }

        /* Detailed Station Card on Map */
        .marker-card-detailed {
            background: $cardBg;
            color: $cardText;
            padding: 5px 8px;
            border-radius: 12px;
            border: 2px solid var(--border-color, #16a34a);
            box-shadow: 0 4px 12px $shadowHex;
            display: flex;
            flex-direction: column;
            gap: 2px;
            min-width: 110px;
            max-width: 160px;
            backdrop-filter: blur(8px);
        }

        .marker-card-header {
            display: flex;
            align-items: center;
            gap: 5px;
        }

        .marker-api-badge {
            background: var(--border-color, #16a34a);
            color: #ffffff;
            font-size: 11px;
            font-weight: 900;
            padding: 1px 5px;
            border-radius: 6px;
            letter-spacing: -0.2px;
            display: inline-block;
        }

        .marker-station-title {
            font-size: 10px;
            font-weight: 800;
            color: $cardText;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 95px;
        }

        .marker-sub-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 8.5px;
            color: $subText;
            font-weight: 600;
        }

        .marker-landmark-tag {
            font-size: 8px;
            color: $subText;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 145px;
            border-top: 1px solid $borderHex;
            padding-top: 2px;
            margin-top: 1px;
        }

        /* Compact Marker Badge */
        .marker-badge-compact {
            background: $cardBg;
            color: $cardText;
            font-size: 11px;
            font-weight: 800;
            padding: 3px 8px;
            border-radius: 12px;
            border: 2px solid var(--border-color, #16a34a);
            box-shadow: 0 3px 8px $shadowHex;
            white-space: nowrap;
            letter-spacing: -0.2px;
            display: flex;
            align-items: center;
            gap: 4px;
        }
        
        .marker-pin-stem {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: var(--pin-color, #16a34a);
            border: 2.5px solid #ffffff;
            box-shadow: 0 2px 6px rgba(0,0,0,0.4);
            margin-top: -2px;
            position: relative;
        }

        .marker-pin-stem.pulse::after {
            content: '';
            position: absolute;
            top: -7px; left: -7px; right: -7px; bottom: -7px;
            border-radius: 50%;
            border: 2.5px solid var(--pin-color, #16a34a);
            animation: ripple 1.4s infinite;
        }

        @keyframes ripple {
            0% { transform: scale(0.9); opacity: 0.95; }
            100% { transform: scale(2.4); opacity: 0; }
        }

        /* Google Maps Style InfoWindow Popup */
        .info-window {
            position: absolute;
            transform: translate(-50%, -100%);
            margin-top: -24px;
            background: $cardBg;
            color: $cardText;
            border-radius: 16px;
            padding: 12px 14px;
            box-shadow: 0 8px 24px $shadowHex;
            border: 1.5px solid var(--station-color, #16a34a);
            width: 250px;
            pointer-events: auto;
            z-index: 400;
            animation: popIn 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
        }

        @keyframes popIn {
            0% { opacity: 0; transform: translate(-50%, -90%) scale(0.9); }
            100% { opacity: 1; transform: translate(-50%, -100%) scale(1); }
        }

        .info-window::after {
            content: '';
            position: absolute;
            bottom: -8px;
            left: 50%;
            transform: translateX(-50%);
            border-width: 8px 8px 0;
            border-style: solid;
            border-color: $cardBg transparent transparent;
            display: block;
            width: 0;
        }

        .iw-header {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            margin-bottom: 6px;
        }

        .iw-title {
            font-size: 13px;
            font-weight: 800;
            color: $cardText;
            line-height: 1.2;
        }

        .iw-badge-official {
            font-size: 8px;
            font-weight: 800;
            color: #0284c7;
            background: rgba(2, 132, 199, 0.15);
            padding: 2px 5px;
            border-radius: 4px;
            display: inline-block;
            margin-top: 2px;
        }

        .iw-close-btn {
            background: none;
            border: none;
            font-size: 16px;
            color: $subText;
            cursor: pointer;
            padding: 0 4px;
            line-height: 1;
        }

        .iw-location-row {
            font-size: 10px;
            color: $subText;
            margin-bottom: 8px;
            line-height: 1.3;
        }

        .iw-api-card {
            background: var(--bg-tint, rgba(22, 163, 74, 0.12));
            border-radius: 10px;
            padding: 8px 10px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 8px;
        }

        .iw-api-num {
            font-size: 24px;
            font-weight: 900;
            color: var(--station-color, #16a34a);
            line-height: 1;
        }

        .iw-api-desc {
            text-align: right;
        }

        .iw-level-title {
            font-size: 11px;
            font-weight: 800;
            color: var(--station-color, #16a34a);
        }

        .iw-level-sub {
            font-size: 9px;
            color: $subText;
        }

        .iw-pollutants-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 4px;
            margin-bottom: 10px;
            background: rgba(0,0,0,0.04);
            padding: 6px;
            border-radius: 8px;
        }

        .iw-pollutant-item {
            font-size: 9.5px;
            color: $subText;
        }

        .iw-pollutant-item strong {
            color: $cardText;
            font-weight: 800;
        }

        .iw-actions {
            display: flex;
            gap: 6px;
        }

        .iw-btn {
            flex: 1;
            padding: 6px 4px;
            border-radius: 8px;
            font-size: 10px;
            font-weight: 700;
            border: none;
            cursor: pointer;
            text-align: center;
        }

        .iw-btn-primary {
            background: #ea580c;
            color: #ffffff;
        }

        .iw-btn-secondary {
            background: rgba(0,0,0,0.08);
            color: $cardText;
        }

        .user-gps-marker {
            position: absolute;
            width: 18px;
            height: 18px;
            border-radius: 50%;
            background: #0ea5e9;
            border: 3px solid #ffffff;
            box-shadow: 0 0 12px rgba(14, 165, 233, 0.9);
            transform: translate(-50%, -50%);
            pointer-events: none;
            z-index: 90;
        }
        .user-gps-marker::after {
            content: '';
            position: absolute;
            top: -8px; left: -8px; right: -8px; bottom: -8px;
            border-radius: 50%;
            border: 2px solid #0ea5e9;
            animation: ripple 2s infinite;
        }

        #attribution {
            position: absolute;
            bottom: 4px;
            right: 6px;
            font-size: 8px;
            color: #94a3b8;
            background: rgba(0,0,0,0.35);
            padding: 2px 6px;
            border-radius: 4px;
            pointer-events: none;
            z-index: 50;
        }
    </style>
</head>
<body>
    <div id="map-container">
        <!-- SVG Geographic Background (Instant crisp geometry fallback) -->
        <svg id="basemap-fallback"></svg>
        <div id="tiles-layer"></div>
        <canvas id="heatmap-canvas"></canvas>
        <canvas id="wind-canvas"></canvas>
        <svg id="vectors-layer"></svg>
        <div id="markers-layer"></div>
        <div id="infowindow-layer"></div>
        <div id="attribution">Google Maps • DOE Air Quality</div>
    </div>

    <script>
        var container = document.getElementById('map-container');
        var basemapFallback = document.getElementById('basemap-fallback');
        var tilesLayer = document.getElementById('tiles-layer');
        var heatmapCanvas = document.getElementById('heatmap-canvas');
        var heatmapCtx = heatmapCanvas.getContext('2d');
        var windCanvas = document.getElementById('wind-canvas');
        var windCtx = windCanvas.getContext('2d');
        var vectorsLayer = document.getElementById('vectors-layer');
        var markersLayer = document.getElementById('markers-layer');
        var infoWindowLayer = document.getElementById('infowindow-layer');

        // Map State
        var centerLat = 5.3700;
        var centerLon = 100.3000;
        var zoom = 11;
        var mapType = 'm'; // m=Roadmap, y=Satellite, p=Terrain, v=Voyager, osm=OSM
        var isHeatmap = true;
        var isDark = $isDarkTheme;
        var isDetailedMode = true;
        var stationsData = [];
        var activeStationId = null;
        var userLat = 5.4164;
        var userLon = 100.3327;

        var isDragging = false;
        var startX, startY;
        var lastTouchDist = 0;

        // Animated Wind Streamlines State
        var animFrameId = null;
        var windTime = 0;
        var windParticles = [];
        for (var i = 0; i < 50; i++) {
            windParticles.push({
                xRatio: Math.random(),
                yRatio: Math.random(),
                speed: 0.0012 + Math.random() * 0.0020,
                length: 22 + Math.random() * 30,
                alpha: 0.40 + Math.random() * 0.45
            });
        }

        // Mercator Projection Math
        function latLonToWorldPixels(lat, lon, z) {
            var scale = 256 * Math.pow(2, z);
            var x = (lon + 180) / 360 * scale;
            var sinLat = Math.sin(lat * Math.PI / 180);
            sinLat = Math.min(Math.max(sinLat, -0.9999), 0.9999);
            var y = (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * scale;
            return { x: x, y: y };
        }

        function worldPixelsToLatLon(x, y, z) {
            var scale = 256 * Math.pow(2, z);
            var lon = (x / scale) * 360 - 180;
            var n = Math.PI - 2 * Math.PI * y / scale;
            var lat = (180 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
            return { lat: lat, lon: lon };
        }

        function getTileUrl(x, y, z, type) {
            if (type === 'osm') {
                return 'https://tile.openstreetmap.org/' + z + '/' + x + '/' + y + '.png';
            }
            if (type === 'v') {
                var cSubs = ['a', 'b', 'c', 'd'];
                var cs = cSubs[(x + y) % 4];
                return 'https://' + cs + '.basemaps.cartocdn.com/rastertiles/voyager/' + z + '/' + x + '/' + y + '.png';
            }
            var subdomains = ['mt0', 'mt1', 'mt2', 'mt3'];
            var sub = subdomains[(x + y) % 4];
            return 'https://' + sub + '.google.com/vt/lyrs=' + type + '&x=' + x + '&y=' + y + '&z=' + z;
        }

        function hexToRgb(hex) {
            var clean = (hex || '#16a34a').replace('#', '');
            if (clean.length === 3) {
                clean = clean.split('').map(function(c) { return c + c; }).join('');
            }
            var num = parseInt(clean, 16);
            return {
                r: (num >> 16) & 255,
                g: (num >> 8) & 255,
                b: num & 255
            };
        }

        // Draw High-Visibility Canvas Heatmap (Gaussian Plumes + Isopleths + Penang Strait Channel)
        function renderHeatmapLayer(width, height, viewLeft, viewTop) {
            heatmapCanvas.width = width;
            heatmapCanvas.height = height;
            heatmapCanvas.style.display = isHeatmap ? 'block' : 'none';
            heatmapCtx.clearRect(0, 0, width, height);

            if (!isHeatmap || stationsData.length === 0) return;

            heatmapCtx.save();

            // 1. Draw Connecting Diffusion Plume across Penang Strait (George Town -> Prai)
            var usmSt = stationsData.find(function(s) { return s.id.indexOf('usm') >= 0 || s.id.indexOf('minden') >= 0 || s.id.indexOf('george') >= 0; });
            var praiSt = stationsData.find(function(s) { return s.id.indexOf('prai') >= 0 || s.id.indexOf('seberang') >= 0; });
            if (usmSt && praiSt) {
                var p1 = latLonToWorldPixels(usmSt.lat, usmSt.lon, zoom);
                var p2 = latLonToWorldPixels(praiSt.lat, praiSt.lon, zoom);
                var cx = ((p1.x + p2.x) / 2) - viewLeft;
                var cy = ((p1.y + p2.y) / 2) - viewTop;
                var sRadius = 140 * Math.pow(1.25, zoom - 11);

                var straitGrad = heatmapCtx.createRadialGradient(cx, cy, 0, cx, cy, sRadius);
                straitGrad.addColorStop(0.0, 'rgba(234, 88, 12, 0.48)');
                straitGrad.addColorStop(0.5, 'rgba(217, 119, 6, 0.28)');
                straitGrad.addColorStop(1.0, 'rgba(217, 119, 6, 0)');

                heatmapCtx.fillStyle = straitGrad;
                heatmapCtx.beginPath();
                heatmapCtx.arc(cx, cy, sRadius, 0, Math.PI * 2);
                heatmapCtx.fill();
            }

            // 2. Draw Multi-Source Gaussian Radial Plumes
            stationsData.forEach(function(st) {
                var pt = latLonToWorldPixels(st.lat, st.lon, zoom);
                var sx = pt.x - viewLeft;
                var sy = pt.y - viewTop;

                var zoomFactor = Math.pow(1.30, zoom - 11);
                var baseRadius = 90 + (st.api * 1.05);
                var radius = Math.min(340, Math.max(65, baseRadius * zoomFactor));

                var rgb = hexToRgb(st.color);

                // Multi-stop Gaussian atmospheric concentration gradient
                var grad = heatmapCtx.createRadialGradient(sx, sy, 0, sx, sy, radius);
                grad.addColorStop(0.00, 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ', 0.70)');
                grad.addColorStop(0.35, 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ', 0.45)');
                grad.addColorStop(0.70, 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ', 0.22)');
                grad.addColorStop(1.00, 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ', 0.00)');

                heatmapCtx.fillStyle = grad;
                heatmapCtx.beginPath();
                heatmapCtx.arc(sx, sy, radius, 0, Math.PI * 2);
                heatmapCtx.fill();

                // Isopleth boundary contour rings
                heatmapCtx.save();
                heatmapCtx.strokeStyle = 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ', 0.65)';
                heatmapCtx.lineWidth = 1.8;
                heatmapCtx.setLineDash([5, 4]);
                heatmapCtx.beginPath();
                heatmapCtx.arc(sx, sy, radius * 0.48, 0, Math.PI * 2);
                heatmapCtx.stroke();

                heatmapCtx.strokeStyle = 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ', 0.38)';
                heatmapCtx.lineWidth = 1.4;
                heatmapCtx.setLineDash([7, 5]);
                heatmapCtx.beginPath();
                heatmapCtx.arc(sx, sy, radius * 0.80, 0, Math.PI * 2);
                heatmapCtx.stroke();
                heatmapCtx.restore();
            });

            heatmapCtx.restore();
        }

        // Draw Wind Streamlines on Separate Canvas Layer
        function renderWindLayer(width, height) {
            windCanvas.style.display = isHeatmap ? 'block' : 'none';
            if (!isHeatmap) return;

            if (windCanvas.width !== width || windCanvas.height !== height) {
                windCanvas.width = width;
                windCanvas.height = height;
            }

            windCtx.clearRect(0, 0, width, height);

            windCtx.save();
            windCtx.lineWidth = 2.4;
            windCtx.lineCap = 'round';

            // Wind direction: 220° SW blowing towards ~40° NE
            var angle = -Math.PI / 4; // 45 deg upwards right
            var cosA = Math.cos(angle);
            var sinA = Math.sin(angle);

            windParticles.forEach(function(p) {
                var px = (p.xRatio * width + windTime * p.speed * width) % width;
                var py = (p.yRatio * height + windTime * p.speed * height * 0.95) % height;
                py = height - py; // invert for upward flow

                var strokeAlpha = isDark ? (p.alpha * 0.90) : (p.alpha * 0.75);
                windCtx.strokeStyle = 'rgba(234, 88, 12, ' + strokeAlpha + ')';

                windCtx.beginPath();
                windCtx.moveTo(px, py);
                windCtx.lineTo(px + cosA * p.length, py + sinA * p.length);
                windCtx.stroke();

                // Particle head dot
                windCtx.fillStyle = 'rgba(251, 146, 60, ' + Math.min(1.0, strokeAlpha + 0.30) + ')';
                windCtx.beginPath();
                windCtx.arc(px + cosA * p.length, py + sinA * p.length, 2.2, 0, Math.PI * 2);
                windCtx.fill();
            });

            windCtx.restore();
        }

        // Start Smooth Wind Animation Loop
        function startWindLoop() {
            if (animFrameId) cancelAnimationFrame(animFrameId);
            function loop() {
                if (isHeatmap) {
                    windTime += 1;
                    var width = container.clientWidth || window.innerWidth || 390;
                    var height = container.clientHeight || window.innerHeight || 700;
                    renderWindLayer(width, height);
                }
                animFrameId = requestAnimationFrame(loop);
            }
            loop();
        }

        // Draw Instant Geographic SVG Fallback
        function renderBasemapFallback(width, height, viewLeft, viewTop) {
            basemapFallback.setAttribute('width', width);
            basemapFallback.setAttribute('height', height);
            
            var islandLand = isDark ? '#1e293b' : '#d6e4db';
            var mainlandLand = isDark ? '#1b242e' : '#d0ddd6';
            var strokeCol = isDark ? '#334155' : '#b5c9be';
            var seaCol = isDark ? '#0f172a' : '#e2edf8';

            // Penang Island polygon
            var islandCoords = [
                { lat: 5.47, lon: 100.20 },
                { lat: 5.47, lon: 100.28 },
                { lat: 5.43, lon: 100.32 },
                { lat: 5.41, lon: 100.34 },
                { lat: 5.35, lon: 100.31 },
                { lat: 5.29, lon: 100.28 },
                { lat: 5.26, lon: 100.27 },
                { lat: 5.26, lon: 100.20 },
                { lat: 5.35, lon: 100.20 },
                { lat: 5.43, lon: 100.19 }
            ];

            var islandPts = islandCoords.map(function(c) {
                var p = latLonToWorldPixels(c.lat, c.lon, zoom);
                return (p.x - viewLeft) + ',' + (p.y - viewTop);
            }).join(' ');

            // Mainland polygon
            var mainlandCoords = [
                { lat: 5.52, lon: 100.36 },
                { lat: 5.40, lon: 100.36 },
                { lat: 5.37, lon: 100.38 },
                { lat: 5.34, lon: 100.41 },
                { lat: 5.26, lon: 100.43 },
                { lat: 5.12, lon: 100.48 },
                { lat: 5.12, lon: 100.58 },
                { lat: 5.52, lon: 100.58 }
            ];

            var mainlandPts = mainlandCoords.map(function(c) {
                var p = latLonToWorldPixels(c.lat, c.lon, zoom);
                return (p.x - viewLeft) + ',' + (p.y - viewTop);
            }).join(' ');

            basemapFallback.innerHTML = 
                '<rect width="' + width + '" height="' + height + '" fill="' + seaCol + '"/>' +
                '<polygon points="' + islandPts + '" fill="' + islandLand + '" stroke="' + strokeCol + '" stroke-width="2"/>' +
                '<polygon points="' + mainlandPts + '" fill="' + mainlandLand + '" stroke="' + strokeCol + '" stroke-width="2"/>';
        }

        function renderMap() {
            var width = container.clientWidth || window.innerWidth || document.documentElement.clientWidth || 390;
            var height = container.clientHeight || window.innerHeight || document.documentElement.clientHeight || 700;
            if (width <= 0) width = 390;
            if (height <= 0) height = 700;

            var centerWorld = latLonToWorldPixels(centerLat, centerLon, zoom);
            var viewLeft = centerWorld.x - width / 2;
            var viewTop = centerWorld.y - height / 2;

            // 0. Render Instant Geographic Fallback
            renderBasemapFallback(width, height, viewLeft, viewTop);

            // 1. Render Google Street Map / Roadmap / Voyager Tiles
            var minTileX = Math.floor(viewLeft / 256);
            var maxTileX = Math.floor((viewLeft + width) / 256);
            var minTileY = Math.floor(viewTop / 256);
            var maxTileY = Math.floor((viewTop + height) / 256);

            tilesLayer.innerHTML = '';
            var maxTile = Math.pow(2, zoom);

            for (var tx = minTileX; tx <= maxTileX; tx++) {
                for (var ty = minTileY; ty <= maxTileY; ty++) {
                    if (ty < 0 || ty >= maxTile) continue;
                    var wrappedTx = (tx % maxTile + maxTile) % maxTile;

                    var tileImg = document.createElement('img');
                    tileImg.className = 'map-tile';
                    tileImg.crossOrigin = 'anonymous';
                    tileImg.src = getTileUrl(wrappedTx, ty, zoom, mapType);
                    tileImg.style.left = (tx * 256 - viewLeft) + 'px';
                    tileImg.style.top = (ty * 256 - viewTop) + 'px';

                    // Fallback to CartoDB Voyager or OSM if Google Map tile fails
                    tileImg.onerror = function() {
                        if (!this.dataset.fallback) {
                            this.dataset.fallback = "1";
                            var cSubs = ['a', 'b', 'c', 'd'];
                            var cs = cSubs[Math.floor(Math.random() * 4)];
                            this.src = 'https://' + cs + '.basemaps.cartocdn.com/rastertiles/voyager/' + zoom + '/' + wrappedTx + '/' + ty + '.png';
                        }
                    };

                    tilesLayer.appendChild(tileImg);
                }
            }

            // 2. Render Atmospheric Heatmap Canvas Layer
            renderHeatmapLayer(width, height, viewLeft, viewTop);

            // 3. Render Vectors (Penang 1st & 2nd Bridges)
            vectorsLayer.setAttribute('width', width);
            vectorsLayer.setAttribute('height', height);
            vectorsLayer.innerHTML = '';

            // Penang 1st Bridge Vector (Gelugor to Prai)
            var b1Points = [
                latLonToWorldPixels(5.3530, 100.3100, zoom),
                latLonToWorldPixels(5.3580, 100.3450, zoom),
                latLonToWorldPixels(5.3620, 100.3850, zoom)
            ];
            var b1Path = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
            var b1PtsStr = b1Points.map(function(p) { return (p.x - viewLeft) + ',' + (p.y - viewTop); }).join(' ');
            b1Path.setAttribute('points', b1PtsStr);
            b1Path.setAttribute('stroke', '#4f46e5');
            b1Path.setAttribute('stroke-width', '3.5');
            b1Path.setAttribute('stroke-dasharray', '5,5');
            b1Path.setAttribute('fill', 'none');
            vectorsLayer.appendChild(b1Path);

            // Penang 2nd Bridge Vector (Batu Maung to Batu Kawan)
            var b2Points = [
                latLonToWorldPixels(5.2750, 100.2750, zoom),
                latLonToWorldPixels(5.2600, 100.3400, zoom),
                latLonToWorldPixels(5.2650, 100.4300, zoom)
            ];
            var b2Path = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
            var b2PtsStr = b2Points.map(function(p) { return (p.x - viewLeft) + ',' + (p.y - viewTop); }).join(' ');
            b2Path.setAttribute('points', b2PtsStr);
            b2Path.setAttribute('stroke', '#7c3aed');
            b2Path.setAttribute('stroke-width', '3.5');
            b2Path.setAttribute('stroke-dasharray', '5,5');
            b2Path.setAttribute('fill', 'none');
            vectorsLayer.appendChild(b2Path);

            // 4. Render Station Pins & User GPS Marker
            markersLayer.innerHTML = '';
            infoWindowLayer.innerHTML = '';

            // User GPS Marker
            if (userLat && userLon) {
                var userPt = latLonToWorldPixels(userLat, userLon, zoom);
                var userDiv = document.createElement('div');
                userDiv.className = 'user-gps-marker';
                userDiv.style.left = (userPt.x - viewLeft) + 'px';
                userDiv.style.top = (userPt.y - viewTop) + 'px';
                markersLayer.appendChild(userDiv);
            }

            // Render all stations with comprehensive info cards/pins
            stationsData.forEach(function(st) {
                var pt = latLonToWorldPixels(st.lat, st.lon, zoom);
                var sx = pt.x - viewLeft;
                var sy = pt.y - viewTop;

                var isSel = (st.isSelected || activeStationId === st.id);

                var marker = document.createElement('div');
                marker.className = 'station-marker' + (isSel ? ' selected' : '');
                marker.style.left = sx + 'px';
                marker.style.top = sy + 'px';

                var pulseClass = isSel ? ' pulse' : '';
                var officialTag = st.isOfficial ? ' <span style="color:#0284c7;font-weight:900;">✓</span>' : '';

                if (isDetailedMode) {
                    // Full Detailed Station Card floating right on the Google Street Map
                    marker.innerHTML = 
                        '<div class="marker-card-detailed" style="--border-color: ' + st.color + ';">' +
                            '<div class="marker-card-header">' +
                                '<span class="marker-api-badge" style="background:' + st.color + ';">' + st.api + '</span>' +
                                '<span class="marker-station-title">' + (st.shortName || st.name) + officialTag + '</span>' +
                            '</div>' +
                            '<div class="marker-sub-row">' +
                                '<span>' + st.level + '</span>' +
                                '<span>PM2.5: ' + (st.pm25 || '--') + '</span>' +
                            '</div>' +
                            '<div class="marker-landmark-tag">📍 ' + (st.landmark || st.district) + '</div>' +
                        '</div>' +
                        '<div class="marker-pin-stem' + pulseClass + '" style="--pin-color: ' + st.color + ';"></div>';
                } else {
                    // Compact Badge Mode
                    marker.innerHTML = 
                        '<div class="marker-badge-compact" style="--border-color: ' + st.color + ';">' +
                            '<span style="font-weight:900;color:' + st.color + ';">' + st.api + '</span>' +
                            '<span style="font-size:9.5px;font-weight:700;">' + (st.shortName || st.name) + '</span>' +
                        '</div>' +
                        '<div class="marker-pin-stem' + pulseClass + '" style="--pin-color: ' + st.color + ';"></div>';
                }

                marker.addEventListener('click', function(e) {
                    e.stopPropagation();
                    activeStationId = st.id;
                    renderMap();
                    if (window.AndroidBridge && window.AndroidBridge.onStationClicked) {
                        window.AndroidBridge.onStationClicked(st.id);
                    }
                });

                markersLayer.appendChild(marker);

                // 5. If station is active/selected, render rich Google Maps-style InfoWindow Popup
                if (isSel) {
                    var infoWin = document.createElement('div');
                    infoWin.className = 'info-window';
                    infoWin.style.setProperty('--station-color', st.color);
                    var rgbVal = hexToRgb(st.color);
                    infoWin.style.setProperty('--bg-tint', 'rgba(' + rgbVal.r + ',' + rgbVal.g + ',' + rgbVal.b + ', 0.14)');
                    infoWin.style.left = sx + 'px';
                    infoWin.style.top = (sy - 22) + 'px';

                    infoWin.innerHTML = 
                        '<div class="iw-header">' +
                            '<div>' +
                                '<div class="iw-title">' + st.name + '</div>' +
                                (st.isOfficial ? '<div class="iw-badge-official">✓ DOE Official CAQM Station</div>' : '<div class="iw-badge-official" style="color:#d97706;background:rgba(217,119,6,0.15)">Community Air Sensor</div>') +
                            '</div>' +
                            '<button class="iw-close-btn" id="iw-close">✕</button>' +
                        '</div>' +
                        '<div class="iw-location-row">📍 ' + (st.landmark || st.district) + '<br>🌐 ' + st.lat.toFixed(4) + '°N, ' + st.lon.toFixed(4) + '°E • ' + (st.distanceKm || '0.0') + ' km away</div>' +
                        '<div class="iw-api-card">' +
                            '<div class="iw-api-num">' + st.api + ' <span style="font-size:12px;font-weight:700;">API</span></div>' +
                            '<div class="iw-api-desc">' +
                                '<div class="iw-level-title">' + st.level + '</div>' +
                                '<div class="iw-level-sub">' + (st.levelMalay || '') + '</div>' +
                            '</div>' +
                        '</div>' +
                        '<div class="iw-pollutants-grid">' +
                            '<div class="iw-pollutant-item">PM2.5: <strong>' + (st.pm25 || 0) + ' µg/m³</strong></div>' +
                            '<div class="iw-pollutant-item">PM10: <strong>' + (st.pm10 || 0) + ' µg/m³</strong></div>' +
                            '<div class="iw-pollutant-item">O3: <strong>' + (st.o3 || 0) + ' µg/m³</strong></div>' +
                            '<div class="iw-pollutant-item">NO2: <strong>' + (st.no2 || 0) + ' µg/m³</strong></div>' +
                        '</div>' +
                        '<div class="iw-actions">' +
                            '<button class="iw-btn iw-btn-primary" id="iw-set-active">📍 Set Active</button>' +
                            '<button class="iw-btn iw-btn-secondary" id="iw-maps">🧭 Directions</button>' +
                        '</div>';

                    infoWin.querySelector('#iw-close').addEventListener('click', function(ev) {
                        ev.stopPropagation();
                        activeStationId = null;
                        renderMap();
                    });

                    infoWin.querySelector('#iw-set-active').addEventListener('click', function(ev) {
                        ev.stopPropagation();
                        if (window.AndroidBridge && window.AndroidBridge.onSetActiveZone) {
                            window.AndroidBridge.onSetActiveZone(st.id);
                        }
                    });

                    infoWin.querySelector('#iw-maps').addEventListener('click', function(ev) {
                        ev.stopPropagation();
                        if (window.AndroidBridge && window.AndroidBridge.onOpenExternalMaps) {
                            window.AndroidBridge.onOpenExternalMaps(st.lat, st.lon, st.name);
                        }
                    });

                    infoWindowLayer.appendChild(infoWin);
                }
            });
        }

        // Gesture Listeners (Mouse & Touch)
        container.addEventListener('pointerdown', function(e) {
            isDragging = true;
            startX = e.clientX;
            startY = e.clientY;
            container.style.cursor = 'grabbing';
        });

        window.addEventListener('pointermove', function(e) {
            if (!isDragging) return;
            var dx = e.clientX - startX;
            var dy = e.clientY - startY;
            startX = e.clientX;
            startY = e.clientY;

            var cur = latLonToWorldPixels(centerLat, centerLon, zoom);
            var next = worldPixelsToLatLon(cur.x - dx, cur.y - dy, zoom);
            centerLat = Math.min(5.65, Math.max(5.10, next.lat));
            centerLon = Math.min(100.65, Math.max(100.05, next.lon));
            renderMap();
        });

        window.addEventListener('pointerup', function() {
            isDragging = false;
            container.style.cursor = 'grab';
        });

        // Touch Pinch-to-Zoom
        container.addEventListener('touchstart', function(e) {
            if (e.touches.length === 2) {
                var dx = e.touches[0].clientX - e.touches[1].clientX;
                var dy = e.touches[0].clientY - e.touches[1].clientY;
                lastTouchDist = Math.sqrt(dx * dx + dy * dy);
            }
        });

        container.addEventListener('touchmove', function(e) {
            if (e.touches.length === 2) {
                var dx = e.touches[0].clientX - e.touches[1].clientX;
                var dy = e.touches[0].clientY - e.touches[1].clientY;
                var dist = Math.sqrt(dx * dx + dy * dy);
                if (lastTouchDist > 0) {
                    if (dist - lastTouchDist > 40 && zoom < 17) {
                        zoom++;
                        lastTouchDist = dist;
                        renderMap();
                    } else if (lastTouchDist - dist > 40 && zoom > 9) {
                        zoom--;
                        lastTouchDist = dist;
                        renderMap();
                    }
                }
            }
        });

        window.addEventListener('resize', function() {
            renderMap();
        });

        // Public API for Android Bridge
        window.initPenangGoogleMap = function(stations, heatmapEnabled, mapTypeCode, uLat, uLon, isDarkTheme, detailedLabels) {
            stationsData = stations || [];
            isHeatmap = typeof heatmapEnabled !== 'undefined' ? heatmapEnabled : true;
            mapType = mapTypeCode || 'm';
            isDark = !!isDarkTheme;
            if (typeof detailedLabels !== 'undefined') isDetailedMode = !!detailedLabels;
            if (uLat && uLon) {
                userLat = uLat;
                userLon = uLon;
            }
            renderMap();
            startWindLoop();
        };

        window.updateMapState = function(stations, heatmapEnabled, mapTypeCode, isDarkTheme, detailedLabels) {
            stationsData = stations || [];
            isHeatmap = typeof heatmapEnabled !== 'undefined' ? heatmapEnabled : true;
            if (mapTypeCode) mapType = mapTypeCode;
            if (typeof isDarkTheme !== 'undefined') isDark = !!isDarkTheme;
            if (typeof detailedLabels !== 'undefined') isDetailedMode = !!detailedLabels;
            renderMap();
        };

        window.panToStation = function(lat, lon, stationId) {
            centerLat = lat;
            centerLon = lon;
            activeStationId = stationId || null;
            zoom = Math.max(zoom, 13);
            renderMap();
        };

        window.mapZoomIn = function() {
            if (zoom < 18) {
                zoom++;
                renderMap();
            }
        };

        window.mapZoomOut = function() {
            if (zoom > 9) {
                zoom--;
                renderMap();
            }
        };

        window.resetPenangView = function() {
            centerLat = 5.3700;
            centerLon = 100.3000;
            zoom = 11;
            renderMap();
        };

        window.panToMyLocation = function() {
            if (userLat && userLon) {
                centerLat = userLat;
                centerLon = userLon;
                zoom = 14;
                renderMap();
            }
        };

        // Guaranteed initial and interval layout triggers
        renderMap();
        startWindLoop();
        setTimeout(renderMap, 150);
        setTimeout(renderMap, 500);
        setTimeout(renderMap, 1200);
    </script>
</body>
</html>
    """.trimIndent()
}
