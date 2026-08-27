package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Stage step progression
    var currentStep by remember { mutableIntStateOf(0) }
    val stepLabels = listOf(
        "Connecting to DOE CAQM Stations...",
        "Calibrating 11 Penang Sensor Points...",
        "Loading Atmospheric Dispersion Grid...",
        "Real-Time Air Quality Defense Ready"
    )

    // Animation drivers
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    
    // Glowing pulse on central icon
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Ring rotation for orbital particle aura
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Fade-in of content
    var contentVisible by remember { mutableStateOf(false) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        contentVisible = true
        
        // Progress stage 1
        progressAnim.animateTo(0.3f, tween(600, easing = LinearOutSlowInEasing))
        currentStep = 1
        delay(400)

        // Progress stage 2
        progressAnim.animateTo(0.65f, tween(600, easing = LinearOutSlowInEasing))
        currentStep = 2
        delay(400)

        // Progress stage 3
        progressAnim.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))
        currentStep = 3
        delay(450)

        // Complete & transition
        onSplashFinished()
    }

    val bgGradient = if (isDarkTheme) {
        listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0F1113)
        )
    } else {
        listOf(
            Color(0xFFFFF7ED),
            Color(0xFFFEF3C7),
            Color(0xFFF8FAFC)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Floating atmospheric ambient particles in the background
        Canvas(modifier = Modifier.fillMaxSize().alpha(if (isDarkTheme) 0.35f else 0.25f)) {
            val particleColors = listOf(GeoGreen, GeoOrange, GeoOrangeDark, GeoBlue)
            val count = 16
            for (i in 0 until count) {
                val angle = (i * (360f / count) + rotationDegrees * 0.5f) * (Math.PI / 180f)
                val radius = (size.minDimension * 0.22f) + (i % 4) * 35f
                val cx = size.width / 2f + (cos(angle) * radius).toFloat()
                val cy = size.height / 2f + (sin(angle) * radius).toFloat()
                val particleRadius = 4f + (i % 3) * 3f
                drawCircle(
                    color = particleColors[i % particleColors.size],
                    radius = particleRadius,
                    center = Offset(cx, cy)
                )
            }
        }

        // Central Branding Content
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.92f, animationSpec = tween(600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                // Central Glowing App Logo Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(150.dp)
                ) {
                    // Ambient halo glow behind
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale * 1.08f)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        GeoOrange.copy(alpha = if (isDarkTheme) 0.4f else 0.25f),
                                        GeoGreen.copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )

                    // Outer orbit dashed ring
                    Canvas(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                    ) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    GeoGreen,
                                    GeoOrange,
                                    GeoOrangeDark,
                                    GeoBlue,
                                    GeoGreen
                                )
                            ),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Main App Emblem
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0xFF1E293B) else PureWhite,
                        shadowElevation = 10.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            2.5.dp,
                            Brush.linearGradient(listOf(GeoOrange, GeoOrangeDark))
                        ),
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pulseScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = "Penang Air Quality",
                                    tint = GeoOrange,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GeoGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PULAU PINANG",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GeoGreen,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Titles
                Text(
                    text = "PENANG HAZE",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = if (isDarkTheme) PureWhite else Color(0xFF0F172A)
                )

                Text(
                    text = "AIR QUALITY & HEALTH DEFENSE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    color = GeoOrange
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Official Coverage Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else PureWhite,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GeoGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "11 Official CAQM & Sensor Stations Active",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Progress Bar & Loading Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.78f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GeoGreen, GeoOrange, GeoOrangeDark)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                    fadeOut(animationSpec = tween(150))
                        },
                        label = "step_transition"
                    ) { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (step == 3) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GeoGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = stepLabels.getOrElse(step) { "Initializing..." },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = if (step == 3) GeoGreen else (if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                        }
                    }
                }
            }
        }

        // Bottom Accreditation / Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = GeoOrange,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DOE MALAYSIA API STANDARDS • REAL-TIME GPS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Department of Environment • Island & Seberang Perai",
                fontSize = 8.5.sp,
                color = if (isDarkTheme) Color(0xFF475569) else Color(0xFFA0AEC0)
            )
        }
    }
}
