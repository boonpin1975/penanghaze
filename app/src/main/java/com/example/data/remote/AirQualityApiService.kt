package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AirQualityApiService {

    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,us_aqi,european_aqi",
        @Query("hourly") hourly: String = "pm10,pm2_5,us_aqi",
        @Query("timezone") timezone: String = "Asia/Kuala_Lumpur",
        @Query("forecast_days") forecastDays: Int = 2
    ): OpenMeteoAirQualityResponse

    companion object {
        private const val BASE_URL = "https://air-quality-api.open-meteo.com/"

        fun create(): AirQualityApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AirQualityApiService::class.java)
        }
    }
}
