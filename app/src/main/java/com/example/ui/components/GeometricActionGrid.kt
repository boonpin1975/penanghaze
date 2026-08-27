package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AirQualityReading
import com.example.ui.theme.*

@Composable
fun GeometricActionGrid(
    reading: AirQualityReading,
    modifier: Modifier = Modifier
) {
    val api = reading.apiValue

    val outdoorStatus = if (api <= 50) "Safe for all activities" else if (api <= 100) "Safe for most activities" else "Limit outdoor exercise"
    val outdoorColor = if (api <= 100) GeoGreen else GeoOrange
    val outdoorBg = if (api <= 100) GeoGreenLight else GeoOrangeLight

    val maskStatus = if (api <= 50) "No mask required" else if (api <= 100) "Mask recommended" else "N95 / KF94 required"
    val maskColor = if (api <= 50) GeoGreen else GeoOrange
    val maskBg = if (api <= 50) GeoGreenLight else GeoOrangeLight

    val healthStatus = if (api <= 50) "Normal hydration" else if (api <= 100) "Drink more water" else "Stay well hydrated"
    val healthColor = GeoBlue
    val healthBg = GeoBlueLight

    val homeStatus = if (api <= 100) "Normal ventilation" else "Keep windows closed"
    val homeColor = if (api <= 100) Color(0xFF64748B) else GeoOrangeDark
    val homeBg = if (api <= 100) Color(0xFFF1F5F9) else GeoOrangeLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("geometric_action_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GeometricStatusCard(
                category = "OUTDOORS",
                status = outdoorStatus,
                icon = Icons.Default.DirectionsRun,
                accentColor = outdoorColor,
                iconBgColor = outdoorBg,
                modifier = Modifier.weight(1f)
            )
            GeometricStatusCard(
                category = "SENSITIVE",
                status = maskStatus,
                icon = Icons.Default.Masks,
                accentColor = maskColor,
                iconBgColor = maskBg,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GeometricStatusCard(
                category = "HEALTH",
                status = healthStatus,
                icon = Icons.Default.LocalDrink,
                accentColor = healthColor,
                iconBgColor = healthBg,
                modifier = Modifier.weight(1f)
            )
            GeometricStatusCard(
                category = "HOME",
                status = homeStatus,
                icon = Icons.Default.Window,
                accentColor = homeColor,
                iconBgColor = homeBg,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GeometricStatusCard(
    category: String,
    status: String,
    icon: ImageVector,
    accentColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rounded-xl icon badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = category,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                minLines = 2
            )
        }
    }
}
