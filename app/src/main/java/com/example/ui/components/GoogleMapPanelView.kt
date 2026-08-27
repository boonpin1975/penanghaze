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
    isDarkTheme: Boolean = false,
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

    // JSON serialization for stations with color coding and names
    val stationsJson = remember(filteredStations, stationReadings, selectedStation) {
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

            val obj = JSONObject().apply {
                put("id", station.id)
                put("name", station.name)
                put("shortName", station.name.replace("Penang", "").replace("Station", "").trim())
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
    LaunchedEffect(stationsJson, isHeatmapEnabled, mapType, isMapLoaded, isDarkTheme) {
        if (isMapLoaded) {
            val script = "if(window.updateMapState){ window.updateMapState($stationsJson, $isHeatmapEnabled, '${mapType.code}', $isDarkTheme); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // Pan to selected station
    LaunchedEffect(selectedStation, isMapLoaded) {
        if (isMapLoaded && selectedStation != null) {
            val script = "if(window.panToStation){ window.panToStation(${selectedStation.latitude}, ${selectedStation.longitude}); }"
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
                    val bgColor = if (isDarkTheme) 0xFF0F172A.toInt() else 0xFFF1F5F9.toInt()
                    setBackgroundColor(bgColor)

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
                            val initScript = "if(window.initPenangGoogleMap){ window.initPenangGoogleMap($stationsJson, $isHeatmapEnabled, '${mapType.code}', $userLatitude, $userLongitude, $isDarkTheme); }"
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
                        },
                        "AndroidBridge"
                    )

                    val htmlContent = generateSelfContainedGoogleMapHtml(isDarkTheme)
                    loadDataWithBaseURL(
                        null,
                        htmlContent,
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                        color = if (isSelected) GeoOrange else Color.Transparent
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

        // AQI Map Legend Bar at Bottom-Left (above card)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedStation != null) 300.dp else 84.dp)
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
 * 100% Self-Contained Slippy Map Engine (Zero CDN Dependency)
 * Renders Google Maps Roadmap, Satellite Hybrid, Terrain, and OSM tiles directly,
 * with touch pan/zoom, dynamic air quality station pins, pulse animations, bridges, and GPS radar.
 */
private fun generateSelfContainedGoogleMapHtml(isDarkTheme: Boolean): String {
    val bgHex = if (isDarkTheme) "#0f172a" else "#f8fafc"
    val badgeBg = if (isDarkTheme) "#0f172a" else "#ffffff"
    val badgeText = if (isDarkTheme) "#ffffff" else "#0f172a"
    val badgeShadow = if (isDarkTheme) "rgba(0,0,0,0.6)" else "rgba(0,0,0,0.18)"
    val subText = if (isDarkTheme) "#94a3b8" else "#64748b"

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

        #tiles-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            pointer-events: none;
        }

        .map-tile {
            position: absolute;
            width: 256px;
            height: 256px;
            transition: opacity 0.15s ease-in;
            pointer-events: none;
        }

        #vectors-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            pointer-events: none;
        }

        #markers-layer {
            position: absolute;
            top: 0; left: 0;
            width: 100%; height: 100%;
            pointer-events: none;
        }

        .station-marker {
            position: absolute;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            pointer-events: auto;
            transform: translate(-50%, -100%);
            transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1);
            z-index: 100;
        }
        
        .station-marker:hover, .station-marker.selected {
            transform: translate(-50%, -100%) scale(1.22);
            z-index: 500;
        }
        
        .marker-badge {
            background: $badgeBg;
            color: $badgeText;
            font-size: 11px;
            font-weight: 800;
            padding: 3px 8px;
            border-radius: 12px;
            border: 2px solid var(--border-color, #16a34a);
            box-shadow: 0 3px 8px $badgeShadow;
            white-space: nowrap;
            letter-spacing: -0.2px;
            display: flex;
            align-items: center;
            gap: 4px;
        }
        
        .marker-name {
            font-size: 9px;
            font-weight: 600;
            color: $subText;
            max-width: 90px;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        
        .marker-pin {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: var(--pin-color, #16a34a);
            border: 2.5px solid #ffffff;
            box-shadow: 0 2px 6px rgba(0,0,0,0.4);
            margin-top: -2px;
            position: relative;
        }

        .marker-pin.pulse::after {
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
            background: rgba(15, 23, 42, 0.7);
            padding: 2px 6px;
            border-radius: 4px;
            pointer-events: none;
            z-index: 1000;
        }
    </style>
</head>
<body>
    <div id="map-container">
        <div id="tiles-layer"></div>
        <svg id="vectors-layer"></svg>
        <div id="markers-layer"></div>
        <div id="attribution">© Google Maps / OpenStreetMap</div>
    </div>

    <script>
        // Map State
        var centerLat = 5.3600;
        var centerLon = 100.3000;
        var zoom = 11;
        var mapType = 'm'; // 'm' roadmap, 'y' satellite, 'p' terrain, 'osm'
        var isHeatmap = true;
        var stationsData = [];
        var userLat = 5.4164;
        var userLon = 100.3327;

        var container = document.getElementById('map-container');
        var tilesLayer = document.getElementById('tiles-layer');
        var vectorsLayer = document.getElementById('vectors-layer');
        var markersLayer = document.getElementById('markers-layer');

        // Drag & Touch Gestures State
        var isDragging = false;
        var startX, startY;
        var lastTouchDist = 0;

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
            var subdomains = ['mt0', 'mt1', 'mt2', 'mt3'];
            var sub = subdomains[(x + y) % 4];
            return 'https://' + sub + '.google.com/vt/lyrs=' + type + '&x=' + x + '&y=' + y + '&z=' + z;
        }

        function renderMap() {
            var width = container.clientWidth || window.innerWidth;
            var height = container.clientHeight || window.innerHeight;
            if (width === 0 || height === 0) return;

            var centerWorld = latLonToWorldPixels(centerLat, centerLon, zoom);
            var viewLeft = centerWorld.x - width / 2;
            var viewTop = centerWorld.y - height / 2;

            // 1. Render Map Tiles
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
                    tileImg.src = getTileUrl(wrappedTx, ty, zoom, mapType);
                    tileImg.style.left = (tx * 256 - viewLeft) + 'px';
                    tileImg.style.top = (ty * 256 - viewTop) + 'px';

                    // Fallback to OSM if Google Map tile fails
                    tileImg.onerror = function() {
                        if (!this.dataset.fallback) {
                            this.dataset.fallback = "1";
                            this.src = 'https://tile.openstreetmap.org/' + zoom + '/' + wrappedTx + '/' + ty + '.png';
                        }
                    };

                    tilesLayer.appendChild(tileImg);
                }
            }

            // 2. Render Vectors (Bridges & Heatmap)
            vectorsLayer.setAttribute('width', width);
            vectorsLayer.setAttribute('height', height);
            vectorsLayer.innerHTML = '';

            // Heatmap dispersion circles
            if (isHeatmap && stationsData.length) {
                stationsData.forEach(function(st) {
                    var pt = latLonToWorldPixels(st.lat, st.lon, zoom);
                    var sx = pt.x - viewLeft;
                    var sy = pt.y - viewTop;
                    var radius = Math.min(110, Math.max(38, (st.api * 0.75) * (zoom / 11)));

                    var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                    circle.setAttribute('cx', sx);
                    circle.setAttribute('cy', sy);
                    circle.setAttribute('r', radius);
                    circle.setAttribute('fill', st.color);
                    circle.setAttribute('fill-opacity', '0.25');
                    circle.setAttribute('stroke', st.color);
                    circle.setAttribute('stroke-opacity', '0.4');
                    circle.setAttribute('stroke-width', '1.5');
                    vectorsLayer.appendChild(circle);
                });
            }

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
            b1Path.setAttribute('stroke-width', '3');
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
            b2Path.setAttribute('stroke-width', '3');
            b2Path.setAttribute('stroke-dasharray', '5,5');
            b2Path.setAttribute('fill', 'none');
            vectorsLayer.appendChild(b2Path);

            // 3. Render Station Pins & User GPS Marker
            markersLayer.innerHTML = '';

            // User GPS Marker
            if (userLat && userLon) {
                var userPt = latLonToWorldPixels(userLat, userLon, zoom);
                var userDiv = document.createElement('div');
                userDiv.className = 'user-gps-marker';
                userDiv.style.left = (userPt.x - viewLeft) + 'px';
                userDiv.style.top = (userPt.y - viewTop) + 'px';
                markersLayer.appendChild(userDiv);
            }

            // Stations Markers (Showing all 11 stations clearly)
            stationsData.forEach(function(st) {
                var pt = latLonToWorldPixels(st.lat, st.lon, zoom);
                var sx = pt.x - viewLeft;
                var sy = pt.y - viewTop;

                var marker = document.createElement('div');
                marker.className = 'station-marker' + (st.isSelected ? ' selected' : '');
                marker.style.left = sx + 'px';
                marker.style.top = sy + 'px';

                var pulseClass = st.isSelected ? ' pulse' : '';
                marker.innerHTML = 
                    '<div class="marker-badge" style="--border-color: ' + st.color + ';">' +
                        '<span>' + st.api + '</span>' +
                        '<span class="marker-name">' + (st.shortName || st.name) + '</span>' +
                    '</div>' +
                    '<div class="marker-pin' + pulseClass + '" style="--pin-color: ' + st.color + ';"></div>';

                marker.addEventListener('click', function(e) {
                    e.stopPropagation();
                    if (window.AndroidBridge && window.AndroidBridge.onStationClicked) {
                        window.AndroidBridge.onStationClicked(st.id);
                    }
                });

                markersLayer.appendChild(marker);
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
        window.initPenangGoogleMap = function(stations, heatmapEnabled, mapTypeCode, uLat, uLon) {
            stationsData = stations || [];
            isHeatmap = heatmapEnabled;
            mapType = mapTypeCode || 'm';
            if (uLat && uLon) {
                userLat = uLat;
                userLon = uLon;
            }
            renderMap();
        };

        window.updateMapState = function(stations, heatmapEnabled, mapTypeCode) {
            stationsData = stations || [];
            isHeatmap = heatmapEnabled;
            if (mapTypeCode) mapType = mapTypeCode;
            renderMap();
        };

        window.panToStation = function(lat, lon) {
            centerLat = lat;
            centerLon = lon;
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
            centerLat = 5.3600;
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

        // Initial trigger
        renderMap();
    </script>
</body>
</html>
    """.trimIndent()
}
