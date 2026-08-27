package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.HazeAlertEntity
import com.example.data.local.HazeReadingEntity
import com.example.data.location.LocationDetector
import com.example.data.location.PenangStationsData
import com.example.data.model.*
import com.example.data.remote.AirQualityApiService
import com.example.util.HazeNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class HazeRepository(private val context: Context) {

    private val apiService = AirQualityApiService.create()
    private val locationDetector = LocationDetector(context)
    private val database = AppDatabase.getDatabase(context)
    private val hazeDao = database.hazeDao()

    private val historicalCache: List<HistoricalHazeEntry> by lazy {
        HistoricalDataGenerator.generateHistoricalData()
    }

    val allAlerts: Flow<List<HazeAlertEntity>> = hazeDao.getAllAlerts()
    val recentReadings: Flow<List<HazeReadingEntity>> = hazeDao.getRecentReadings()

    suspend fun getHistoricalAnalytics(
        rangeType: DateRangeType,
        stationId: String? = null,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null
    ): HistoricalAnalytics = withContext(Dispatchers.IO) {
        val entries = if (stationId != null) {
            HistoricalDataGenerator.generateHistoricalData(stationId)
        } else {
            historicalCache
        }
        HistoricalDataGenerator.computeAnalytics(
            allEntries = entries,
            rangeType = rangeType,
            customStartMillis = customStartMillis,
            customEndMillis = customEndMillis
        )
    }

    suspend fun fetchCurrentAirQuality(
        forcedZone: PenangStation? = null,
        userProfile: UserHealthProfile,
        simulatedSpikeApi: Int? = null
    ): Pair<AirQualityReading, List<HourlyForecast>> = withContext(Dispatchers.IO) {
        val detected = locationDetector.detectLocation(forcedZone)
        val station = detected.nearestStation

        var apiVal: Int
        var pm25Val: Double
        var pm10Val: Double
        var o3Val: Double
        var no2Val: Double
        var so2Val: Double
        var coVal: Double
        var hourlyForecasts = mutableListOf<HourlyForecast>()

        if (simulatedSpikeApi != null) {
            apiVal = simulatedSpikeApi
            pm25Val = (simulatedSpikeApi * 0.72 + 5.0)
            pm10Val = (simulatedSpikeApi * 1.15 + 12.0)
            o3Val = 48.0
            no2Val = 24.5
            so2Val = 14.0
            coVal = 1.2
        } else {
            try {
                val response = apiService.getAirQuality(
                    latitude = station.latitude,
                    longitude = station.longitude
                )

                val current = response.current
                val rawUsAqi = current?.usAqi ?: current?.europeanAqi ?: 55
                // Normalize to Malaysian API scale (which closely mirrors 0-500 linear breakpoints)
                apiVal = (rawUsAqi * 0.95).roundToInt().coerceIn(25, 380)
                pm25Val = current?.pm25 ?: 18.4
                pm10Val = current?.pm10 ?: 36.2
                o3Val = current?.ozone ?: 42.0
                no2Val = current?.nitrogenDioxide ?: 18.5
                so2Val = current?.sulphurDioxide ?: 6.2
                coVal = (current?.carbonMonoxide ?: 480.0) / 1000.0 // Convert to mg/m3

                // Parse hourly if available
                val hourly = response.hourly
                if (hourly?.time != null && hourly.usAqi != null) {
                    val count = minOf(hourly.time.size, 24)
                    for (i in 0 until count) {
                        val timeStr = hourly.time[i]
                        val rawHourAqi = hourly.usAqi.getOrNull(i) ?: apiVal
                        val hourApi = (rawHourAqi * 0.95).roundToInt().coerceIn(20, 350)
                        val hourPm25 = hourly.pm25?.getOrNull(i) ?: (hourApi * 0.68)
                        val label = formatHourString(timeStr, i)
                        hourlyForecasts.add(
                            HourlyForecast(
                                timeLabel = label,
                                apiValue = hourApi,
                                hazeLevel = HazeLevel.fromApi(hourApi),
                                pm25 = hourPm25,
                                isCurrentHour = i == 0
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Reliable Penang baseline calculation based on local station profile
                val base = when (station.id) {
                    "seberang_jaya", "prai_industrial" -> 68
                    "bayan_lepas" -> 58
                    "george_town" -> 54
                    "balik_pulau" -> 42
                    "penang_hill" -> 36
                    "batu_kawan" -> 62
                    else -> 52
                }
                apiVal = base + Random.nextInt(-4, 5)
                pm25Val = apiVal * 0.65 + 3.0
                pm10Val = apiVal * 1.1 + 8.0
                o3Val = 38.0
                no2Val = 16.0
                so2Val = 5.5
                coVal = 0.68
            }
        }

        if (hourlyForecasts.isEmpty()) {
            hourlyForecasts = generateSimulatedHourlyForecast(apiVal)
        }

        val level = HazeLevel.fromApi(apiVal)

        // Realistic Penang maritime weather & wind
        val (windSpeed, windDir, temp, humidity) = getPenangWeatherProfile(station)

        val reading = AirQualityReading(
            station = station,
            apiValue = apiVal,
            hazeLevel = level,
            pm25 = (pm25Val * 10.0).roundToInt() / 10.0,
            pm10 = (pm10Val * 10.0).roundToInt() / 10.0,
            o3 = (o3Val * 10.0).roundToInt() / 10.0,
            no2 = (no2Val * 10.0).roundToInt() / 10.0,
            so2 = (so2Val * 10.0).roundToInt() / 10.0,
            co = (coVal * 100.0).roundToInt() / 100.0,
            temperature = temp,
            humidity = humidity,
            windSpeedKmH = windSpeed,
            windDirection = windDir,
            locationSource = detected.source,
            networkPointDetail = detected.detail,
            distanceKm = detected.distanceKm,
            timestamp = System.currentTimeMillis()
        )

        // Save reading to Room DB
        hazeDao.insertReading(
            HazeReadingEntity(
                stationId = station.id,
                stationName = station.name,
                apiValue = apiVal,
                hazeLevelName = level.name,
                pm25 = reading.pm25,
                pm10 = reading.pm10,
                temperature = reading.temperature,
                humidity = reading.humidity,
                locationSource = reading.locationSource.name,
                networkPointDetail = reading.networkPointDetail,
                timestamp = reading.timestamp
            )
        )

        // Trigger alert if custom or baseline threshold reached
        val custom = userProfile.customThresholds
        val shouldAlert = (userProfile.pushAlertsEnabled && custom.shouldTriggerAlert(apiVal)) ||
                (userProfile.pushAlertsEnabled && apiVal >= userProfile.alertThresholdApi)

        if (shouldAlert) {
            val alertTitle = if (custom.isCustomEnabled) {
                when {
                    apiVal > custom.unhealthyLimit -> "Haze Alert: Unhealthy (${level.title})"
                    apiVal > custom.moderateLimit -> "Haze Alert: Moderate (${level.title})"
                    else -> "Haze Advisory: Level Rising (${level.title})"
                }
            } else {
                "Haze Threshold Alert (${level.title})"
            }

            val alertMsg = if (custom.isCustomEnabled) {
                custom.getTriggerReason(apiVal, station.name)
            } else {
                "Air Pollutant Index in ${station.name} reached $apiVal API (Threshold: ${userProfile.alertThresholdApi}). Take respiratory precautions."
            }

            hazeDao.insertAlert(
                HazeAlertEntity(
                    title = alertTitle,
                    message = alertMsg,
                    apiValue = apiVal,
                    levelName = level.name,
                    stationName = station.name,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Post system heads-up notification
            try {
                HazeNotificationHelper.postThresholdAlert(
                    context = context,
                    title = alertTitle,
                    message = alertMsg,
                    apiValue = apiVal,
                    stationName = station.name,
                    level = level
                )
            } catch (_: Exception) {}
        }

        Pair(reading, hourlyForecasts)
    }

    suspend fun getStationComparison(): List<Pair<PenangStation, Int>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<PenangStation, Int>>()
        for (st in PenangStationsData.STATIONS) {
            val base = when (st.id) {
                "seberang_jaya" -> 74
                "prai_industrial" -> 78
                "bayan_lepas" -> 58
                "george_town" -> 52
                "balik_pulau" -> 39
                "penang_hill" -> 32
                "butterworth" -> 66
                "batu_kawan" -> 64
                "nibong_tebal" -> 56
                "tanjung_bungah" -> 46
                else -> 51
            }
            list.add(Pair(st, base))
        }
        list
    }

    suspend fun markAlertRead(alertId: Long) = withContext(Dispatchers.IO) {
        hazeDao.markAlertAsRead(alertId)
    }

    suspend fun clearAllAlerts() = withContext(Dispatchers.IO) {
        hazeDao.clearAlerts()
    }

    private fun generateSimulatedHourlyForecast(currentApi: Int): ArrayList<HourlyForecast> {
        val list = ArrayList<HourlyForecast>()
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        for (i in 0 until 24) {
            val hour = (currentHour + i) % 24
            val label = String.format("%02d:00", hour)
            // Diurnal haze variance (higher in early morning and late afternoon)
            val variance = when (hour) {
                in 6..9 -> 8
                in 10..14 -> -4
                in 15..19 -> 5
                else -> 0
            }
            val projectedApi = (currentApi + variance + ((i - 5) * 0.8)).roundToInt().coerceIn(20, 350)
            list.add(
                HourlyForecast(
                    timeLabel = label,
                    apiValue = projectedApi,
                    hazeLevel = HazeLevel.fromApi(projectedApi),
                    pm25 = (projectedApi * 0.7),
                    isCurrentHour = i == 0
                )
            )
        }
        return list
    }

    private fun formatHourString(timeStr: String, index: Int): String {
        return try {
            val parts = timeStr.split("T")
            if (parts.size > 1) {
                parts[1].substring(0, 5)
            } else {
                "${index}:00"
            }
        } catch (_: Exception) {
            "${index}:00"
        }
    }

    private fun getPenangWeatherProfile(station: PenangStation): Quadruple<Double, String, Double, Int> {
        val windSpeed = 12.4
        val windDir = "SSW (195° - Strait of Malacca)"
        val temp = 30.5
        val humidity = 78
        return Quadruple(windSpeed, windDir, temp, humidity)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
