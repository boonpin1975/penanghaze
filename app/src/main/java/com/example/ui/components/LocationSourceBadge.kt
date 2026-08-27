package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AirQualityReading
import com.example.data.model.LocationSource
import com.example.ui.theme.GeoDarkSection
import com.example.ui.theme.GeoGreen
import com.example.ui.theme.PureWhite

@Composable
fun LocationSourceBadge(
    reading: AirQualityReading,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val icon = when (reading.locationSource) {
        LocationSource.GPS -> Icons.Default.GpsFixed
        LocationSource.WIFI_POINT -> Icons.Default.Wifi
        LocationSource.CELL_5G_LTE -> Icons.Default.CellTower
        LocationSource.MANUAL_ZONE -> Icons.Default.LocationCity
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("location_source_badge"),
        shape = RoundedCornerShape(24.dp),
        color = GeoDarkSection
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular translucent icon container
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(PureWhite.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .background(GeoGreen.copy(alpha = 0.35f), CircleShape)
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = reading.locationSource.label,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "STATION TYPE • ${reading.locationSource.label.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite.copy(alpha = 0.55f),
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${reading.station.name} (${reading.networkPointDetail})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PureWhite,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Verified check icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Active Sync",
                tint = GeoGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
