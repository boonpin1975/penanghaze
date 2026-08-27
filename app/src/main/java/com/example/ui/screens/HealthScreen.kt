package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthRecommendation
import com.example.data.model.PenangEmergencyContact
import com.example.data.model.RecommendationUrgency
import com.example.data.model.UserHealthProfile
import com.example.ui.HazeUiState
import com.example.ui.components.CardContainer
import com.example.ui.theme.*

@Composable
fun HealthScreen(
    state: HazeUiState,
    onUpdateProfile: (UserHealthProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("health_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Row: Recommendations | Mask Guide | Penang Hospitals
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                contentColor = GeoOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = GeoOrange,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Advisory", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Mask Guide", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Masks, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("Penang Clinics", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (selectedSubTab) {
            0 -> AdvisoryTabContent(
                state = state,
                onUpdateProfile = onUpdateProfile
            )
            1 -> MaskGuideTabContent(currentApi = state.reading?.apiValue ?: 50)
            2 -> PenangEmergencyClinicsContent(
                contacts = state.emergencyContacts,
                onCall = { phone -> dialPhoneNumber(context, phone) }
            )
        }
    }
}

@Composable
fun AdvisoryTabContent(
    state: HazeUiState,
    onUpdateProfile: (UserHealthProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Personal Health Profile Customizer
        item {
            HealthProfileCard(
                profile = state.userProfile,
                onProfileChanged = onUpdateProfile
            )
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTOMATED HEALTH RECOMMENDATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.reading?.let {
                    Text(
                        text = "${it.station.name} (${it.apiValue} API)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = it.hazeLevel.color
                    )
                }
            }
        }

        // Recommendations List
        items(state.recommendations, key = { it.id }) { rec ->
            RecommendationItemCard(recommendation = rec)
        }

        // Outdoor Activity Suitability Card
        item {
            OutdoorActivitySuitabilityCard(apiValue = state.reading?.apiValue ?: 50)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HealthProfileCard(
    profile: UserHealthProfile,
    onProfileChanged: (UserHealthProfile) -> Unit
) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONAL VULNERABILITY PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GeoOrangeBg
                ) {
                    Text(
                        text = "ADAPTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = GeoOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Switch 1: Asthma / Respiratory
            ProfileSwitchRow(
                label = "Asthma / Bronchial Condition",
                subtitle = "Triggers inhaler & early bronchodilator warnings",
                checked = profile.hasAsthmaOrRespiratory,
                onCheckedChange = { onProfileChanged(profile.copy(hasAsthmaOrRespiratory = it)) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Switch 2: Elderly / Young Children
            ProfileSwitchRow(
                label = "Elderly or Toddlers in Household",
                subtitle = "School sports cancellation & kindergarten warnings",
                checked = profile.isElderlyOrHasChildren,
                onCheckedChange = { onProfileChanged(profile.copy(isElderlyOrHasChildren = it)) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Switch 3: Active Athlete / Outdoor Worker
            ProfileSwitchRow(
                label = "Outdoor Runner / Cyclist / Field Worker",
                subtitle = "Gurney Drive & Penang Hill cardio safe zones",
                checked = profile.isOutdoorActiveOrAthlete,
                onCheckedChange = { onProfileChanged(profile.copy(isOutdoorActiveOrAthlete = it)) }
            )
        }
    }
}

@Composable
fun ProfileSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = GeoOrange,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun RecommendationItemCard(recommendation: HealthRecommendation) {
    val isUrgent = recommendation.urgency == RecommendationUrgency.URGENT ||
            recommendation.urgency == RecommendationUrgency.CRITICAL
    val accent = if (isUrgent) GeoOrangeDark else GeoGreen
    val bgAccent = if (isUrgent) GeoOrangeBg else GeoGreenBg

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUrgent) GeoOrangeLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = recommendation.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        letterSpacing = 0.8.sp
                    )
                }

                if (recommendation.maskType != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bgAccent
                    ) {
                        Text(
                            text = recommendation.maskType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recommendation.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = recommendation.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun OutdoorActivitySuitabilityCard(apiValue: Int) {
    CardContainer(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PENANG OUTDOOR ACTIVITY SUITABILITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActivityItemRow(
                activity = "Gurney Drive / Coastal Jogging",
                suitability = if (apiValue <= 50) "Optimal" else if (apiValue <= 100) "Moderate" else "Unsafe",
                isSafe = apiValue <= 100
            )
            ActivityItemRow(
                activity = "Penang Hill / Moongate Trail Hike",
                suitability = if (apiValue <= 60) "Optimal" else if (apiValue <= 120) "Acceptable" else "Avoid",
                isSafe = apiValue <= 120
            )
            ActivityItemRow(
                activity = "Penang Bridge Cycling & Marathon",
                suitability = if (apiValue <= 50) "Optimal" else if (apiValue <= 90) "Limit < 45m" else "Cancel",
                isSafe = apiValue <= 90
            )
        }
    }
}

@Composable
fun ActivityItemRow(activity: String, suitability: String, isSafe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = activity, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSafe) GeoGreenBg else GeoOrangeBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSafe) GeoGreenLight else GeoOrangeLight)
        ) {
            Text(
                text = suitability,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSafe) GeoGreen else GeoOrangeDark,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun MaskGuideTabContent(currentApi: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CardContainer(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PENANG HAZE RESPIRATOR GUIDE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sub-micron PM2.5 particles penetrate standard cloth masks. Use electrostatic certified filters.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            MaskTypeCard(
                title = "N95 / NIOSH & KF94 Respirators",
                rating = "95% - 99% PM2.5 Filtration",
                recommendedFor = "API > 100 (Unhealthy / Hazardous)",
                details = "Tightly sealed with headstraps or 3D earloops. Captures microscopic soot, combustion ash, and sulfate haze aerosols.",
                isRecommendedNow = currentApi > 100
            )
        }

        item {
            MaskTypeCard(
                title = "3-Ply Medical / Surgical Mask",
                rating = "30% - 50% PM2.5 Filtration",
                recommendedFor = "API 51 - 100 (Moderate)",
                details = "Blocks large dust particles and sneezes, but lacks airtight edge seals for fine haze particles. Suitable for short outdoor walks.",
                isRecommendedNow = currentApi in 51..100
            )
        }

        item {
            MaskTypeCard(
                title = "Fabric / Cloth Face Cover",
                rating = "< 15% PM2.5 Filtration",
                recommendedFor = "API 0 - 50 (Good)",
                details = "Porous weave cannot stop PM2.5 particles. Not recommended during active transboundary haze episodes.",
                isRecommendedNow = false
            )
        }

        item {
            CardContainer(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "AIR PURIFIER & INDOOR PRECAUTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Use True HEPA (H13) air purifiers rated for your room square footage.\n" +
                                "• Ensure air conditioner is set to Internal Air Recirculation mode.\n" +
                                "• Avoid burning incense, frying food at high heat, or open flames indoors.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MaskTypeCard(
    title: String,
    rating: String,
    recommendedFor: String,
    details: String,
    isRecommendedNow: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            if (isRecommendedNow) 2.dp else 1.dp,
            if (isRecommendedNow) GeoOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isRecommendedNow) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GeoOrange
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Efficiency: $rating",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GeoGreen
            )

            Text(
                text = "Recommended for: $recommendedFor",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = details,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun PenangEmergencyClinicsContent(
    contacts: List<PenangEmergencyContact>,
    onCall: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CardContainer(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PENANG RESPIRATORY EMERGENCY CARE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If experiencing severe breathlessness, chest tightness, or asthma exacerbation, tap to call emergency hospital dispatch immediately.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(contacts) { contact ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.hospitalName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = contact.location,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${contact.hotlineName} • ${contact.distanceEstimate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GeoGreen
                        )
                    }

                    Button(
                        onClick = { onCall(contact.phoneNumber) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeoOrange,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun dialPhoneNumber(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
