package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: CurrentAirQuality? = null,
    val hourly: HourlyAirQuality? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAirQuality(
    val time: String? = null,
    val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double? = null,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: Double? = null,
    val ozone: Double? = null,
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "european_aqi") val europeanAqi: Int? = null
)

@JsonClass(generateAdapter = true)
data class HourlyAirQuality(
    val time: List<String>? = null,
    val pm10: List<Double?>? = null,
    @Json(name = "pm2_5") val pm25: List<Double?>? = null,
    @Json(name = "us_aqi") val usAqi: List<Int?>? = null
)
