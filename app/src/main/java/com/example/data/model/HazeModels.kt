package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class HazeLevel(
    val title: String,
    val titleMalay: String,
    val rangeMin: Int,
    val rangeMax: Int,
    val color: Color,
    val containerColor: Color,
    val shortAdvice: String
) {
    GOOD(
        title = "Good",
        titleMalay = "Baik",
        rangeMin = 0,
        rangeMax = 50,
        color = HazeGreenGood,
        containerColor = HazeGreenBg,
        shortAdvice = "Air quality is considered satisfactory, and air pollution poses little or no risk."
    ),
    MODERATE(
        title = "Moderate",
        titleMalay = "Sederhana",
        rangeMin = 51,
        rangeMax = 100,
        color = HazeOrangeModerate,
        containerColor = HazeOrangeBg,
        shortAdvice = "Air quality is acceptable. Unusually sensitive individuals should consider limiting prolonged outdoor exertion."
    ),
    UNHEALTHY(
        title = "Unhealthy",
        titleMalay = "Tidak Sihat",
        rangeMin = 101,
        rangeMax = 200,
        color = HazeOrangeUnhealthy,
        containerColor = HazeOrangeDark,
        shortAdvice = "People with respiratory conditions, elderly and children should minimize outdoor exertion. Wear N95 masks."
    ),
    VERY_UNHEALTHY(
        title = "Very Unhealthy",
        titleMalay = "Sangat Tidak Sihat",
        rangeMin = 201,
        rangeMax = 300,
        color = HazeRedVeryUnhealthy,
        containerColor = Color(0xFF3B0D0D),
        shortAdvice = "Health alert: Everyone may experience more serious health effects. Stay indoors and use HEPA air purifiers."
    ),
    HAZARDOUS(
        title = "Hazardous",
        titleMalay = "Berbahaya",
        rangeMin = 301,
        rangeMax = 500,
        color = HazeHazardous,
        containerColor = Color(0xFF26050E),
        shortAdvice = "Health warning of emergency conditions. Entire population is likely to be affected. Avoid all outdoor activities."
    );

    companion object {
        fun fromApi(apiValue: Int): HazeLevel {
            return when {
                apiValue <= 50 -> GOOD
                apiValue <= 100 -> MODERATE
                apiValue <= 200 -> UNHEALTHY
                apiValue <= 300 -> VERY_UNHEALTHY
                else -> HAZARDOUS
            }
        }
    }
}

enum class LocationSource(val label: String, val iconName: String) {
    GPS("GPS Satellite Lock", "GpsFixed"),
    WIFI_POINT("Wi-Fi Access Point", "Wifi"),
    CELL_5G_LTE("5G / LTE Cell Tower", "CellTower"),
    MANUAL_ZONE("Penang Zone Selected", "LocationCity")
}

data class PenangStation(
    val id: String,
    val name: String,
    val district: String, // Island (Timur Laut, Barat Daya) or Mainland (Seberang Perai)
    val latitude: Double,
    val longitude: Double,
    val isOfficialCAQM: Boolean = true,
    val landmark: String
)

data class AirQualityReading(
    val station: PenangStation,
    val apiValue: Int,
    val hazeLevel: HazeLevel,
    val pm25: Double, // µg/m³
    val pm10: Double, // µg/m³
    val o3: Double,   // Ozone µg/m³
    val no2: Double,  // NO2 µg/m³
    val so2: Double,  // SO2 µg/m³
    val co: Double,   // CO mg/m³
    val temperature: Double, // °C
    val humidity: Int, // %
    val windSpeedKmH: Double,
    val windDirection: String,
    val locationSource: LocationSource,
    val networkPointDetail: String,
    val distanceKm: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class HealthRecommendation(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val urgency: RecommendationUrgency,
    val maskType: String? = null,
    val iconType: String = "info"
)

enum class RecommendationUrgency {
    NORMAL, ADVISORY, URGENT, CRITICAL
}

data class CustomAlertThresholds(
    val goodLimit: Int = 50, // 0..goodLimit is Good
    val moderateLimit: Int = 100, // (goodLimit+1)..moderateLimit is Moderate
    val unhealthyLimit: Int = 200, // (moderateLimit+1)..unhealthyLimit is Unhealthy, > unhealthyLimit is Very Unhealthy/Hazardous
    val notifyOnModerate: Boolean = false,
    val notifyOnUnhealthy: Boolean = true,
    val notifyOnHazardous: Boolean = true,
    val isCustomEnabled: Boolean = true
) {
    fun getHazeLevel(apiValue: Int): HazeLevel {
        return when {
            apiValue <= goodLimit -> HazeLevel.GOOD
            apiValue <= moderateLimit -> HazeLevel.MODERATE
            apiValue <= unhealthyLimit -> HazeLevel.UNHEALTHY
            apiValue <= (unhealthyLimit + 100) -> HazeLevel.VERY_UNHEALTHY
            else -> HazeLevel.HAZARDOUS
        }
    }

    fun shouldTriggerAlert(apiValue: Int): Boolean {
        if (!isCustomEnabled) return false
        if (notifyOnHazardous && apiValue > unhealthyLimit) return true
        if (notifyOnUnhealthy && apiValue > moderateLimit) return true
        if (notifyOnModerate && apiValue > goodLimit) return true
        return false
    }

    fun getTriggerReason(apiValue: Int, stationName: String): String {
        return when {
            apiValue > unhealthyLimit -> "EMERGENCY: $stationName haze level is $apiValue API, exceeding your Unhealthy threshold of $unhealthyLimit."
            apiValue > moderateLimit -> "WARNING: $stationName haze level reached $apiValue API, exceeding your Moderate threshold of $moderateLimit."
            apiValue > goodLimit -> "ADVISORY: $stationName haze level is $apiValue API, crossing your Good threshold of $goodLimit."
            else -> "INFO: $stationName air quality is normal ($apiValue API)."
        }
    }
}

data class UserHealthProfile(
    val hasAsthmaOrRespiratory: Boolean = false,
    val isElderlyOrHasChildren: Boolean = false,
    val isOutdoorActiveOrAthlete: Boolean = false,
    val alertThresholdApi: Int = 100,
    val pushAlertsEnabled: Boolean = true,
    val customThresholds: CustomAlertThresholds = CustomAlertThresholds()
)

enum class DateRangeType(val label: String, val daysCount: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_3_MONTHS("3 Months", 90),
    LAST_1_YEAR("1 Year", 365),
    CUSTOM("Custom", 0)
}

data class HistoricalHazeEntry(
    val dateTimestamp: Long,
    val dateFormatted: String, // e.g. "27 Aug 2026"
    val shortDate: String,     // e.g. "27 Aug"
    val stationId: String,
    val stationName: String,
    val avgApi: Int,
    val minApi: Int,
    val maxApi: Int,
    val pm25: Double,
    val pm10: Double,
    val level: HazeLevel,
    val isTransboundarySpike: Boolean = false,
    val monsoonSeasonNote: String? = null
)

data class TrendPoint(
    val label: String,
    val dateTimestamp: Long,
    val avgApi: Double,
    val maxApi: Int,
    val minApi: Int,
    val level: HazeLevel
)

data class HistoricalAnalytics(
    val rangeType: DateRangeType,
    val startDateFormatted: String,
    val endDateFormatted: String,
    val overallAverageApi: Double,
    val peakPollutionDay: HistoricalHazeEntry,
    val cleanestDay: HistoricalHazeEntry,
    val totalDays: Int,
    val goodDaysCount: Int,
    val moderateDaysCount: Int,
    val unhealthyDaysCount: Int,
    val veryUnhealthyDaysCount: Int,
    val dailyEntries: List<HistoricalHazeEntry>,
    val trendPoints: List<TrendPoint>
)

data class HourlyForecast(
    val timeLabel: String,
    val apiValue: Int,
    val hazeLevel: HazeLevel,
    val pm25: Double,
    val isCurrentHour: Boolean = false
)

data class PenangEmergencyContact(
    val hospitalName: String,
    val location: String,
    val phoneNumber: String,
    val hotlineName: String,
    val distanceEstimate: String
)
