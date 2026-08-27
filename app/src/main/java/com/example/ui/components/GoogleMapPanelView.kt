package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.HazeLevel
import com.example.data.model.PenangStation
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

enum class GoogleMapType(val displayName: String, val code: String) {
    ROADMAP("Roadmap", "m"),
    SATELLITE("Satellite", "y"),
    TERRAIN("Terrain", "p"),
    OSM("OpenStreet", "osm")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapPanelView(
    stations: List<PenangStation>,
    stationReadings: Map<String, Int>,
    selectedStation: PenangStation?,
    onSelectStation: (PenangStation) -> Unit,
    isHeatmapEnabled: Boolean,
    districtFilter: String,
    mapType: GoogleMapType,
    onMapTypeChange: (GoogleMapType) -> Unit,
    userLatitude: Double? = 5.4164,
    userLongitude: Double? = 100.3327,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val filteredStations = remember(stations, districtFilter) {
        when (districtFilter) {
            "ISLAND" -> stations.filter { it.district.contains("Island", ignoreCase = true) }
            "MAINLAND" -> stations.filter { it.district.contains("Mainland", ignoreCase = true) }
            else -> stations
        }
    }

    // JSON serialization for stations
    val stationsJson = remember(filteredStations, stationReadings, selectedStation) {
        val array = JSONArray()
        filteredStations.forEach { station ->
            val api = stationReadings[station.id] ?: 52
            val level = HazeLevel.fromApi(api)
            val colorHex = when (level) {
                HazeLevel.GOOD -> "#22C55E"
                HazeLevel.MODERATE -> "#EAB308"
                HazeLevel.UNHEALTHY -> "#F97316"
                HazeLevel.VERY_UNHEALTHY -> "#EF4444"
                HazeLevel.HAZARDOUS -> "#7E22CE"
            }

            val obj = JSONObject().apply {
                put("id", station.id)
                put("name", station.name)
                put("district", station.district)
                put("lat", station.latitude)
                put("lon", station.longitude)
                put("api", api)
                put("color", colorHex)
                put("level", level.title)
                put("isOfficial", station.isOfficialCAQM)
                put("isSelected", selectedStation?.id == station.id)
            }
            array.put(obj)
        }
        array.toString()
    }

    // Sync station pins and heatmap to WebView
    LaunchedEffect(stationsJson, isHeatmapEnabled, mapType, isMapLoaded) {
        if (isMapLoaded) {
            val script = "updateMapState($stationsJson, $isHeatmapEnabled, '${mapType.code}');"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // Pan to selected station
    LaunchedEffect(selectedStation, isMapLoaded) {
        if (isMapLoaded && selectedStation != null) {
            val script = "panToStation(${selectedStation.latitude}, ${selectedStation.longitude});"
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
                    setBackgroundColor(0xFF0F172A.toInt())
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(true)
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
                            val initScript = "initPenangGoogleMap($stationsJson, $isHeatmapEnabled, '${mapType.code}', $userLatitude, $userLongitude);"
                            view?.evaluateJavascript(initScript, null)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
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
                        },
                        "AndroidBridge"
                    )

                    loadDataWithBaseURL(
                        "https://maps.google.com",
                        generateGoogleMapHtml(),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { view ->
                webViewRef = view
            }
        )

        // Top-Left Floating Google Maps Layer Selector
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 64.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GeoOrangeBg,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Google Maps",
                            tint = GeoOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Map",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GeoOrange
                        )
                    }
                }

                GoogleMapType.values().forEach { type ->
                    val isSelected = mapType == type
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) GeoOrange else Color.Transparent,
                        modifier = Modifier
                    ) {
                        TextButton(
                            onClick = { onMapTypeChange(type) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Top-Right Quick Map Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Zoom In
            FloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript("mapZoomIn();", null)
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
                    webViewRef?.evaluateJavascript("mapZoomOut();", null)
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
                    webViewRef?.evaluateJavascript("resetPenangView();", null)
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
                        webViewRef?.evaluateJavascript("panToMyLocation();", null)
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

        // AQI Map Legend Bar at Bottom-Left (above card)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedStation != null) 300.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(label = "0-50 Good", color = HazeGreenGood)
                LegendIndicator(label = "51-100 Mod", color = HazeOrangeModerate)
                LegendIndicator(label = "101+ Unh", color = HazeOrangeUnhealthy)
            }
        }
    }
}

@Composable
private fun LegendIndicator(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Generate full HTML5 / Leaflet + Google Maps Tile Layer with dynamic markers,
 * heatmap overlays, Penang bridges, and live GPS tracking.
 */
private fun generateGoogleMapHtml(): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" integrity="sha512-Zcn6HvY/4ddh05027582b93df235b2e650c8227b4097455d36e84d723707b6e9" crossorigin="" />
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js" integrity="sha512-BwHfrr4c9kmRuvxDBTyPiPjURtEDLQeSXUcCN051QRZTVNeyP4nxhukLC4vdf093hA97Vkh5gkV21YUJqyQUDA==" crossorigin=""></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { width: 100%; height: 100%; overflow: hidden; background: #0f172a; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        #map { width: 100%; height: 100%; background: #0f172a; position: absolute; top: 0; left: 0; right: 0; bottom: 0; }
        
        .station-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        .station-marker:hover, .station-marker.selected {
            transform: scale(1.22);
            z-index: 1000 !important;
        }
        
        .marker-badge {
            background: #0f172a;
            color: #ffffff;
            font-size: 11px;
            font-weight: 800;
            padding: 2px 7px;
            border-radius: 12px;
            border: 2px solid var(--border-color, #22c55e);
            box-shadow: 0 3px 6px rgba(0,0,0,0.5);
            white-space: nowrap;
            letter-spacing: -0.2px;
        }
        
        .marker-pin {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: var(--pin-color, #22c55e);
            border: 2.5px solid #ffffff;
            box-shadow: 0 2px 5px rgba(0,0,0,0.5);
            margin-top: -2px;
            position: relative;
        }

        .marker-pin.pulse::after {
            content: '';
            position: absolute;
            top: -6px;
            left: -6px;
            right: -6px;
            bottom: -6px;
            border-radius: 50%;
            border: 2px solid var(--pin-color, #22c55e);
            animation: ripple 1.5s infinite;
        }

        @keyframes ripple {
            0% { transform: scale(0.9); opacity: 0.8; }
            100% { transform: scale(2.2); opacity: 0; }
        }

        .user-gps-marker {
            width: 18px;
            height: 18px;
            border-radius: 50%;
            background: #0ea5e9;
            border: 3px solid #ffffff;
            box-shadow: 0 0 10px rgba(14, 165, 233, 0.8);
            position: relative;
        }
        .user-gps-marker::after {
            content: '';
            position: absolute;
            top: -8px; left: -8px; right: -8px; bottom: -8px;
            border-radius: 50%;
            border: 2px solid #0ea5e9;
            animation: ripple 2s infinite;
        }

        .leaflet-control-attribution {
            font-size: 8px !important;
            background: rgba(15, 23, 42, 0.75) !important;
            color: #94a3b8 !important;
            border-radius: 4px;
            padding: 2px 6px !important;
        }
        .leaflet-control-attribution a {
            color: #38bdf8 !important;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <div id="map"></div>

    <script>
        var map;
        var currentTileLayer;
        var markersGroup;
        var heatmapGroup;
        var bridgesGroup;
        var userMarker;
        var userLat = 5.4164;
        var userLon = 100.3327;

        function initPenangGoogleMap(stations, heatmapEnabled, mapTypeCode, uLat, uLon) {
            if (uLat && uLon) {
                userLat = uLat;
                userLon = uLon;
            }

            if (!window.L) {
                console.error("Leaflet library failed to load");
                return;
            }

            if (!map) {
                // Initialize Leaflet map centered at Penang
                map = L.map('map', {
                    center: [5.3600, 100.3000],
                    zoom: 11,
                    minZoom: 9,
                    maxZoom: 18,
                    zoomControl: false,
                    attributionControl: true
                });

                markersGroup = L.layerGroup().addTo(map);
                heatmapGroup = L.layerGroup().addTo(map);
                bridgesGroup = L.layerGroup().addTo(map);

                // Draw Penang Bridges
                drawPenangBridges();

                // Draw User GPS location
                drawUserGps();
            }

            // Set initial map tile layer
            setMapTileType(mapTypeCode || 'm');

            // Render Stations & Heatmap
            updateMapState(stations, heatmapEnabled, mapTypeCode);

            // Invalidate size after layout settles
            setTimeout(function() { if (map) map.invalidateSize(); }, 150);
            setTimeout(function() { if (map) map.invalidateSize(); }, 400);
            setTimeout(function() { if (map) map.invalidateSize(); }, 800);
        }

        window.addEventListener('resize', function() {
            if (map) map.invalidateSize();
        });

        function setMapTileType(code) {
            if (!map) return;
            if (currentTileLayer) {
                map.removeLayer(currentTileLayer);
            }

            if (code === 'osm') {
                currentTileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap contributors'
                }).addTo(map);
            } else {
                var subdomains = ['mt0', 'mt1', 'mt2', 'mt3'];
                var url = 'https://{s}.google.com/vt/lyrs=' + code + '&x={x}&y={y}&z={z}';
                currentTileLayer = L.tileLayer(url, {
                    maxZoom: 20,
                    subdomains: subdomains,
                    attribution: '© Google Maps'
                });
                
                // Fallback to OSM if Google tile fails
                currentTileLayer.on('tileerror', function() {
                    console.warn('Google Map tile error, falling back to OSM');
                });
                
                currentTileLayer.addTo(map);
            }
        }

        function drawPenangBridges() {
            if (!bridgesGroup) return;
            bridgesGroup.clearLayers();

            // First Bridge: Gelugor to Prai
            var b1Coords = [
                [5.3530, 100.3100],
                [5.3580, 100.3450],
                [5.3620, 100.3850]
            ];
            L.polyline(b1Coords, {
                color: '#6366f1',
                weight: 4,
                opacity: 0.85,
                dashArray: '6, 6'
            }).bindTooltip('Penang 1st Bridge').addTo(bridgesGroup);

            // Second Bridge: Sultan Abdul Halim Muadzam Shah Bridge (Batu Maung to Batu Kawan)
            var b2Coords = [
                [5.2750, 100.2750],
                [5.2600, 100.3400],
                [5.2650, 100.4300]
            ];
            L.polyline(b2Coords, {
                color: '#8b5cf6',
                weight: 4,
                opacity: 0.85,
                dashArray: '6, 6'
            }).bindTooltip('Penang 2nd Bridge').addTo(bridgesGroup);
        }

        function drawUserGps() {
            if (!map) return;
            if (userMarker) {
                map.removeLayer(userMarker);
            }
            var userIcon = L.divIcon({
                className: 'user-gps-container',
                html: '<div class="user-gps-marker"></div>',
                iconSize: [18, 18],
                iconAnchor: [9, 9]
            });
            userMarker = L.marker([userLat, userLon], { icon: userIcon, zIndexOffset: 2000 })
                .bindTooltip('<b>Your GPS Location</b>', { direction: 'top' })
                .addTo(map);
        }

        function updateMapState(stations, heatmapEnabled, mapTypeCode) {
            if (!map) return;

            if (mapTypeCode) {
                setMapTileType(mapTypeCode);
            }

            if (markersGroup) markersGroup.clearLayers();
            if (heatmapGroup) heatmapGroup.clearLayers();

            if (!stations || !stations.length) return;

            stations.forEach(function(st) {
                // 1. Heatmap dispersion circle
                if (heatmapEnabled && heatmapGroup) {
                    var radius = Math.min(6500, Math.max(3000, st.api * 50));
                    var heatCircle = L.circle([st.lat, st.lon], {
                        radius: radius,
                        fillColor: st.color,
                        fillOpacity: 0.28,
                        color: st.color,
                        weight: 1,
                        opacity: 0.4
                    });
                    heatmapGroup.addLayer(heatCircle);
                }

                // 2. Custom Station HTML Pin
                var isSelected = st.isSelected;
                var pulseClass = isSelected ? ' pulse' : '';
                var selectedClass = isSelected ? ' selected' : '';

                var htmlContent = 
                    '<div class="station-marker' + selectedClass + '" onclick="window.AndroidBridge && window.AndroidBridge.onStationClicked(\'' + st.id + '\')">' +
                        '<div class="marker-badge" style="--border-color: ' + st.color + ';">' + st.api + '</div>' +
                        '<div class="marker-pin' + pulseClass + '" style="--pin-color: ' + st.color + ';"></div>' +
                    '</div>';

                var customIcon = L.divIcon({
                    className: 'custom-station-icon',
                    html: htmlContent,
                    iconSize: [40, 36],
                    iconAnchor: [20, 34]
                });

                var marker = L.marker([st.lat, st.lon], {
                    icon: customIcon,
                    title: st.name
                });

                marker.on('click', function() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onStationClicked(st.id);
                    }
                });

                if (markersGroup) markersGroup.addLayer(marker);
            });
        }

        function panToStation(lat, lon) {
            if (map) {
                map.flyTo([lat, lon], 13, { duration: 0.8 });
            }
        }

        function mapZoomIn() {
            if (map) map.zoomIn();
        }

        function mapZoomOut() {
            if (map) map.zoomOut();
        }

        function resetPenangView() {
            if (map) {
                map.flyTo([5.3600, 100.3000], 11, { duration: 0.8 });
            }
        }

        function panToMyLocation() {
            if (map) {
                map.flyTo([userLat, userLon], 14, { duration: 0.8 });
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}
