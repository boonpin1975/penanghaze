package com.example.data.repository

import com.example.data.model.HazeLevel
import com.example.data.model.HealthRecommendation
import com.example.data.model.RecommendationUrgency
import com.example.data.model.UserHealthProfile

object HealthRecommendationEngine {

    fun generateRecommendations(
        hazeLevel: HazeLevel,
        apiValue: Int,
        profile: UserHealthProfile,
        stationName: String
    ): List<HealthRecommendation> {
        val list = mutableListOf<HealthRecommendation>()

        // 1. General Population Status
        when (hazeLevel) {
            HazeLevel.GOOD -> {
                list.add(
                    HealthRecommendation(
                        id = "general_good",
                        category = "General Activity",
                        title = "Safe for All Outdoor Activities",
                        description = "Air quality across $stationName is pristine (API $apiValue). Excellent conditions for outdoor sports, hiking Penang Hill, and natural ventilation.",
                        urgency = RecommendationUrgency.NORMAL,
                        maskType = "No Mask Required",
                        iconType = "nature"
                    )
                )
                list.add(
                    HealthRecommendation(
                        id = "ventilation_good",
                        category = "Indoor Air & Ventilation",
                        title = "Open Windows for Fresh Air",
                        description = "Natural sea breezes and low ambient PM2.5 make it ideal to open windows and refresh indoor living spaces.",
                        urgency = RecommendationUrgency.NORMAL,
                        iconType = "air"
                    )
                )
            }
            HazeLevel.MODERATE -> {
                list.add(
                    HealthRecommendation(
                        id = "general_moderate",
                        category = "General Public",
                        title = "Moderate Haze Detected",
                        description = "API is currently $apiValue in $stationName. Healthy individuals can carry on routine activities, but sensitive individuals should monitor symptoms.",
                        urgency = RecommendationUrgency.ADVISORY,
                        maskType = "Optional for Sensitive Groups",
                        iconType = "visibility"
                    )
                )
                list.add(
                    HealthRecommendation(
                        id = "outdoor_mod",
                        category = "Exercise & Sports",
                        title = "Moderate Outdoor Exertion",
                        description = "Light to moderate outdoor exercise (walking, cycling) is acceptable. High-intensity marathon training along coastal highways should be kept under 60 minutes.",
                        urgency = RecommendationUrgency.ADVISORY,
                        iconType = "directions_run"
                    )
                )
            }
            HazeLevel.UNHEALTHY -> {
                list.add(
                    HealthRecommendation(
                        id = "general_unhealthy",
                        category = "Health Alert",
                        title = "Elevated Haze Alert (API $apiValue)",
                        description = "Particulate matter PM2.5 has breached healthy thresholds in $stationName. Reduce prolonged outdoor exposure immediately.",
                        urgency = RecommendationUrgency.URGENT,
                        maskType = "N95 / KF94 Recommended Outdoors",
                        iconType = "warning"
                    )
                )
                list.add(
                    HealthRecommendation(
                        id = "mask_unhealthy",
                        category = "Respiratory Protection",
                        title = "Wear N95 / KF94 Respirators",
                        description = "Standard cloth or 3-ply surgical masks offer low filtration against sub-micron PM2.5 haze particulates. Use certified N95 or KF94 respirators for outdoor transit.",
                        urgency = RecommendationUrgency.URGENT,
                        maskType = "N95 / KF94",
                        iconType = "masks"
                    )
                )
                list.add(
                    HealthRecommendation(
                        id = "indoor_purifier",
                        category = "Indoor Air Quality",
                        title = "Seal Windows & Run HEPA Purifier",
                        description = "Keep doors and windows shut. Switch home air conditioners to 'Recirculation' mode and run True HEPA filtration in bedrooms and study areas.",
                        urgency = RecommendationUrgency.URGENT,
                        iconType = "filter_alt"
                    )
                )
            }
            HazeLevel.VERY_UNHEALTHY -> {
                list.add(
                    HealthRecommendation(
                        id = "general_very_unhealthy",
                        category = "Critical Warning",
                        title = "Severe Haze Spike (API $apiValue)",
                        description = "Hazardous airborne particulate concentration in $stationName. Everyone, especially school children and elderly, should remain strictly indoors.",
                        urgency = RecommendationUrgency.CRITICAL,
                        maskType = "Strict N95 / FFP2 Required",
                        iconType = "dangerous"
                    )
                )
                list.add(
                    HealthRecommendation(
                        id = "indoor_shelter",
                        category = "Home Confinement",
                        title = "Maximize Indoor Sealing",
                        description = "Seal door bottom gaps with damp towels if smoke smell infiltrates. Avoid smoking, frying food, or burning joss sticks indoors.",
                        urgency = RecommendationUrgency.CRITICAL,
                        iconType = "home"
                    )
                )
            }
            HazeLevel.HAZARDOUS -> {
                list.add(
                    HealthRecommendation(
                        id = "general_hazardous",
                        category = "State Emergency Advisory",
                        title = "Emergency Hazardous Air (API $apiValue)",
                        description = "Severe health hazard across Penang. Stay indoors under sealed air filtration. Avoid all non-essential travel.",
                        urgency = RecommendationUrgency.CRITICAL,
                        maskType = "N95 / Elastomeric Mask",
                        iconType = "emergency"
                    )
                )
            }
        }

        // 2. Sensitive Group Customizations
        if (profile.hasAsthmaOrRespiratory) {
            val asthmaUrgency = if (apiValue > 70) RecommendationUrgency.CRITICAL else RecommendationUrgency.ADVISORY
            list.add(
                HealthRecommendation(
                    id = "profile_asthma",
                    category = "Asthma & Bronchial Care",
                    title = "Keep Inhalers / Relievers Accessible",
                    description = "Personal profile: Asthma active. Microscopic PM2.5 can trigger bronchial spasms. Keep your Salbutamol/reliever inhaler on hand. Seek medical help if wheezing persists.",
                    urgency = asthmaUrgency,
                    maskType = if (apiValue > 50) "N95 / KF94 Mask" else null,
                    iconType = "medical_services"
                )
            )
        }

        if (profile.isElderlyOrHasChildren) {
            val childUrgency = if (apiValue > 80) RecommendationUrgency.URGENT else RecommendationUrgency.NORMAL
            list.add(
                HealthRecommendation(
                    id = "profile_vulnerable",
                    category = "Elderly & Child Advisory",
                    title = "Protect Vulnerable Household Members",
                    description = "Children's respiratory rates are higher per body weight. Restrict outdoor kindergarten and school sports activities in $stationName when API exceeds 100.",
                    urgency = childUrgency,
                    iconType = "family_restroom"
                )
            )
        }

        if (profile.isOutdoorActiveOrAthlete) {
            if (apiValue > 75) {
                list.add(
                    HealthRecommendation(
                        id = "profile_athlete",
                        category = "Athletes & Runners",
                        title = "Shift Training Indoors",
                        description = "High lung tidal volume during running along Gurney Drive or cycling Penang Bridge draws deep particulate deposits into alveolar tissues. Shift workout to indoor gyms.",
                        urgency = RecommendationUrgency.URGENT,
                        iconType = "fitness_center"
                    )
                )
            }
        }

        // 3. Hydration & Eye Care
        if (apiValue > 60) {
            list.add(
                HealthRecommendation(
                    id = "hydration_care",
                    category = "Hydration & Eye Protection",
                    title = "Increase Fluid Intake (Min 2.5L / day)",
                    description = "Haze dust causes dry throat and conjunctival irritation. Drink plenty of warm water and use preservative-free saline eye drops if outdoor stinging occurs.",
                    urgency = RecommendationUrgency.ADVISORY,
                    iconType = "water_drop"
                )
            )
        }

        return list
    }
}
