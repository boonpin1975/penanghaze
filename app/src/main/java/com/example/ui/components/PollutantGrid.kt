package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AirQualityReading
import com.example.ui.theme.*

@Composable
fun PollutantGrid(
    reading: AirQualityReading,
    modifier: Modifier = Modifier
) {
    CardContainer(
        modifier = modifier.testTag("pollutant_grid"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POLLUTANT BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "DOE / WHO Limits",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GeoOrange
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x3 Geometric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PollutantItem(
                    name = "PM2.5",
                    value = "${reading.pm25}",
                    unit = "µg/m³",
                    safeLimit = "< 25 µg/m³",
                    isSafe = reading.pm25 <= 25,
                    modifier = Modifier.weight(1f)
                )
                PollutantItem(
                    name = "PM10",
                    value = "${reading.pm10}",
                    unit = "µg/m³",
                    safeLimit = "< 50 µg/m³",
                    isSafe = reading.pm10 <= 50,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PollutantItem(
                    name = "Ozone (O₃)",
                    value = "${reading.o3}",
                    unit = "µg/m³",
                    safeLimit = "< 100 µg/m³",
                    isSafe = reading.o3 <= 100,
                    modifier = Modifier.weight(1f)
                )
                PollutantItem(
                    name = "Nitrogen (NO₂)",
                    value = "${reading.no2}",
                    unit = "µg/m³",
                    safeLimit = "< 40 µg/m³",
                    isSafe = reading.no2 <= 40,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PollutantItem(
                    name = "Sulfur (SO₂)",
                    value = "${reading.so2}",
                    unit = "µg/m³",
                    safeLimit = "< 20 µg/m³",
                    isSafe = reading.so2 <= 20,
                    modifier = Modifier.weight(1f)
                )
                PollutantItem(
                    name = "Carbon (CO)",
                    value = "${reading.co}",
                    unit = "mg/m³",
                    safeLimit = "< 4.0 mg/m³",
                    isSafe = reading.co <= 4.0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PollutantItem(
    name: String,
    value: String,
    unit: String,
    safeLimit: String,
    isSafe: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isSafe) GeoGreenBg.copy(alpha = 0.5f) else GeoOrangeBg.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSafe) GeoGreenLight else GeoOrangeLight
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSafe) GeoGreenLight else GeoOrangeLight
                ) {
                    Text(
                        text = if (isSafe) "SAFE" else "WARN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSafe) GeoGreen else GeoOrangeDark,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSafe) MaterialTheme.colorScheme.onSurface else GeoOrangeDark
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Text(
                text = safeLimit,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
