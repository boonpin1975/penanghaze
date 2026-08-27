package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomAlertThresholds
import com.example.data.model.HazeLevel
import com.example.data.model.TrendPoint
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HazeTrendChart(
    trendPoints: List<TrendPoint>,
    thresholds: CustomAlertThresholds,
    modifier: Modifier = Modifier,
    height: Int = 220
) {
    if (trendPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No trend data available for this range",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        return
    }

    var selectedIndex by remember(trendPoints) { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedIndex?.let { trendPoints.getOrNull(it) }

    // Chart value range
    val maxApi = maxOf(trendPoints.maxOf { it.maxApi }.toFloat(), thresholds.unhealthyLimit.toFloat() * 1.1f, 150f)
    val minApi = 0f

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val gridLineColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Scrubber Tooltip when user touches the chart
        if (selectedPoint != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedPoint.level.color.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedPoint.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Peak: ${selectedPoint.maxApi} API • Min: ${selectedPoint.minApi} API",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = selectedPoint.level.containerColor
                    ) {
                        Text(
                            text = "${selectedPoint.avgApi.roundToInt()} API (${selectedPoint.level.title})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = selectedPoint.level.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        } else {
            // Default hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAP OR DRAG CHART TO INSPECT VALUES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Thresholds: ${thresholds.goodLimit} / ${thresholds.moderateLimit} / ${thresholds.unhealthyLimit}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = GeoOrange
                )
            }
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .pointerInput(trendPoints) {
                    detectTapGestures { offset ->
                        val pointWidth = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                        val index = ((offset.x + pointWidth / 2) / pointWidth).toInt().coerceIn(0, trendPoints.size - 1)
                        selectedIndex = index
                    }
                }
                .pointerInput(trendPoints) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pointWidth = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                            val index = ((offset.x + pointWidth / 2) / pointWidth).toInt().coerceIn(0, trendPoints.size - 1)
                            selectedIndex = index
                        },
                        onDrag = { change, _ ->
                            val pointWidth = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                            val index = ((change.position.x + pointWidth / 2) / pointWidth).toInt().coerceIn(0, trendPoints.size - 1)
                            selectedIndex = index
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val paddingBottom = 24.dp.toPx()
                val paddingTop = 12.dp.toPx()
                val chartHeight = h - paddingBottom - paddingTop

                fun getY(api: Float): Float {
                    val norm = (api - minApi) / (maxApi - minApi)
                    return paddingTop + chartHeight * (1f - norm.coerceIn(0f, 1f))
                }

                fun getX(index: Int): Float {
                    if (trendPoints.size == 1) return w / 2
                    return (index.toFloat() / (trendPoints.size - 1)) * w
                }

                // 1. Draw horizontal threshold lines (Good, Moderate, Unhealthy)
                val goodY = getY(thresholds.goodLimit.toFloat())
                val moderateY = getY(thresholds.moderateLimit.toFloat())
                val unhealthyY = getY(thresholds.unhealthyLimit.toFloat())

                // Threshold guidelines
                drawDashedLine(start = Offset(0f, goodY), end = Offset(w, goodY), color = HazeGreenGood.copy(alpha = 0.4f))
                drawDashedLine(start = Offset(0f, moderateY), end = Offset(w, moderateY), color = HazeOrangeModerate.copy(alpha = 0.4f))
                drawDashedLine(start = Offset(0f, unhealthyY), end = Offset(w, unhealthyY), color = HazeOrangeUnhealthy.copy(alpha = 0.4f))

                // 2. Draw Area Gradient & Spline Path
                val linePath = Path()
                val areaPath = Path()

                trendPoints.forEachIndexed { i, pt ->
                    val x = getX(i)
                    val y = getY(pt.avgApi.toFloat())

                    if (i == 0) {
                        linePath.moveTo(x, y)
                        areaPath.moveTo(x, h - paddingBottom)
                        areaPath.lineTo(x, y)
                    } else {
                        val prevX = getX(i - 1)
                        val prevY = getY(trendPoints[i - 1].avgApi.toFloat())
                        val cx1 = (prevX + x) / 2
                        val cy1 = prevY
                        val cx2 = (prevX + x) / 2
                        val cy2 = y
                        linePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        areaPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    }
                }

                val lastX = getX(trendPoints.size - 1)
                areaPath.lineTo(lastX, h - paddingBottom)
                areaPath.close()

                // Area gradient
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        GeoOrange.copy(alpha = 0.35f),
                        GeoOrangeLight.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = h - paddingBottom
                )
                drawPath(path = areaPath, brush = gradientBrush)

                // Line path
                drawPath(
                    path = linePath,
                    color = GeoOrange,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 3. Draw Data Points
                trendPoints.forEachIndexed { i, pt ->
                    val x = getX(i)
                    val y = getY(pt.avgApi.toFloat())
                    val isSelected = selectedIndex == i

                    // Outer halo if selected
                    if (isSelected) {
                        drawCircle(
                            color = pt.level.color.copy(alpha = 0.3f),
                            radius = 10.dp.toPx(),
                            center = Offset(x, y)
                        )
                        // Vertical guideline
                        drawLine(
                            color = pt.level.color.copy(alpha = 0.5f),
                            start = Offset(x, paddingTop),
                            end = Offset(x, h - paddingBottom),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Dot
                    drawCircle(
                        color = PureWhite,
                        radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = pt.level.color,
                        radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // 4. Draw X-axis label ticks
                val labelStep = maxOf(1, trendPoints.size / 6)
                for (i in trendPoints.indices step labelStep) {
                    val x = getX(i)
                    val label = trendPoints[i].label
                    // small dot for tick
                    drawCircle(
                        color = gridLineColor,
                        radius = 2.dp.toPx(),
                        center = Offset(x, h - paddingBottom + 6.dp.toPx())
                    )
                }
            }

            // X-axis textual labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (trendPoints.isNotEmpty()) {
                    Text(
                        text = trendPoints.first().label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (trendPoints.size > 2) {
                        val mid = trendPoints[trendPoints.size / 2]
                        Text(
                            text = mid.label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = trendPoints.last().label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDashedLine(
    start: Offset,
    end: Offset,
    color: Color,
    dashWidth: Float = 10f,
    gapWidth: Float = 10f,
    strokeWidth: Float = 2f
) {
    var currentX = start.x
    val y = start.y
    while (currentX < end.x) {
        val nextX = (currentX + dashWidth).coerceAtMost(end.x)
        drawLine(
            color = color,
            start = Offset(currentX, y),
            end = Offset(nextX, y),
            strokeWidth = strokeWidth
        )
        currentX += dashWidth + gapWidth
    }
}
