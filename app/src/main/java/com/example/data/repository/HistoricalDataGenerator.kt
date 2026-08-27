package com.example.data.repository

import com.example.data.location.PenangStationsData
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

object HistoricalDataGenerator {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

    /**
     * Generates a realistic 365-day dataset for Penang stations.
     */
    fun generateHistoricalData(stationIdFilter: String? = null): List<HistoricalHazeEntry> {
        val entries = mutableListOf<HistoricalHazeEntry>()
        val calendar = Calendar.getInstance()
        val currentMillis = calendar.timeInMillis
        val random = Random(42) // Deterministic seed for reproducible historical data

        val stations = if (stationIdFilter != null) {
            PenangStationsData.STATIONS.filter { it.id == stationIdFilter }.ifEmpty { PenangStationsData.STATIONS }
        } else {
            PenangStationsData.STATIONS
        }

        // Loop backwards 365 days
        for (dayOffset in 365 downTo 0) {
            calendar.timeInMillis = currentMillis
            calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val dayMillis = calendar.timeInMillis
            val month = calendar.get(Calendar.MONTH) // 0-11
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            val fullDateStr = dateFormat.format(Date(dayMillis))
            val shortDateStr = shortDateFormat.format(Date(dayMillis))

            // Seasonal climate model for Penang Haze:
            // Aug (7) & Sep (8): Peak transboundary smoke haze from regional peat/slash burning
            // Oct (9) & Nov (10): Wet monsoon season, heavy coastal rain washes particulates
            // Dec (11) - Feb (1): Clean northeast monsoon
            // Mar (2) - May (4): Dry spell, industrial & traffic accumulation
            // Jun (5) - Jul (6): Start of dry southwest monsoon
            var baseApi = when (month) {
                7, 8 -> 85 // August & September
                9, 10 -> 38 // October & November (wet rainy clean)
                11, 0, 1 -> 46 // Dec, Jan, Feb
                2, 3 -> 62 // March, April
                4, 5, 6 -> 68 // May, June, July
                else -> 50
            }

            var isSpike = false
            var note: String? = null

            // Specific authentic transboundary haze wave occurrences
            if ((month == 7 && dayOfMonth in 12..17) || (month == 8 && dayOfMonth in 3..8)) {
                baseApi += random.nextInt(45, 85) // Spikes into Unhealthy (130-175 API)
                isSpike = true
                note = "Transboundary smoke haze from regional Sumatra/Kalimantan hot-spots (SW Monsoon)"
            } else if (month in 9..10 && dayOfMonth in 5..9) {
                baseApi = maxOf(22, baseApi - 14) // Heavy monsoon downpours
                note = "Penang inter-monsoon heavy precipitation washout"
            } else {
                baseApi += random.nextInt(-8, 9)
            }

            // Pick primary station or synthesize
            val primaryStation = stations[dayOffset % stations.size]
            val stationModifier = when (primaryStation.id) {
                "prai_industrial", "seberang_jaya" -> 6
                "bayan_lepas", "butterworth" -> 2
                "george_town" -> 0
                "balik_pulau" -> -8
                "penang_hill" -> -14
                else -> 0
            }

            val finalAvgApi = (baseApi + stationModifier).coerceIn(18, 260)
            val minApi = (finalAvgApi * 0.85).roundToInt().coerceIn(15, finalAvgApi)
            val maxApi = (finalAvgApi * 1.22).roundToInt().coerceAtLeast(finalAvgApi)
            val pm25 = (finalAvgApi * 0.72 + random.nextDouble(1.0, 4.0)).coerceAtLeast(5.0)
            val pm10 = (finalAvgApi * 1.15 + random.nextDouble(3.0, 8.0)).coerceAtLeast(10.0)

            val level = HazeLevel.fromApi(finalAvgApi)

            entries.add(
                HistoricalHazeEntry(
                    dateTimestamp = dayMillis,
                    dateFormatted = fullDateStr,
                    shortDate = shortDateStr,
                    stationId = primaryStation.id,
                    stationName = primaryStation.name,
                    avgApi = finalAvgApi,
                    minApi = minApi,
                    maxApi = maxApi,
                    pm25 = (pm25 * 10.0).roundToInt() / 10.0,
                    pm10 = (pm10 * 10.0).roundToInt() / 10.0,
                    level = level,
                    isTransboundarySpike = isSpike,
                    monsoonSeasonNote = note
                )
            )
        }

        return entries.sortedByDescending { it.dateTimestamp }
    }

    /**
     * Compute comprehensive statistics and aggregations based on selected date range.
     */
    fun computeAnalytics(
        allEntries: List<HistoricalHazeEntry>,
        rangeType: DateRangeType,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null
    ): HistoricalAnalytics {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val filtered = when (rangeType) {
            DateRangeType.LAST_7_DAYS -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val cutoff = calendar.timeInMillis
                allEntries.filter { it.dateTimestamp >= cutoff }
            }
            DateRangeType.LAST_30_DAYS -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val cutoff = calendar.timeInMillis
                allEntries.filter { it.dateTimestamp >= cutoff }
            }
            DateRangeType.LAST_3_MONTHS -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -90)
                val cutoff = calendar.timeInMillis
                allEntries.filter { it.dateTimestamp >= cutoff }
            }
            DateRangeType.LAST_1_YEAR -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -365)
                val cutoff = calendar.timeInMillis
                allEntries.filter { it.dateTimestamp >= cutoff }
            }
            DateRangeType.CUSTOM -> {
                val start = customStartMillis ?: (now - 30L * 24 * 3600 * 1000)
                val end = customEndMillis ?: now
                allEntries.filter { it.dateTimestamp in start..end }
            }
        }.sortedBy { it.dateTimestamp } // chronological order for chart

        if (filtered.isEmpty()) {
            val fallback = allEntries.firstOrNull() ?: HistoricalHazeEntry(
                dateTimestamp = now,
                dateFormatted = dateFormat.format(Date(now)),
                shortDate = shortDateFormat.format(Date(now)),
                stationId = "george_town",
                stationName = "George Town",
                avgApi = 52,
                minApi = 44,
                maxApi = 65,
                pm25 = 18.0,
                pm10 = 36.0,
                level = HazeLevel.MODERATE
            )
            return HistoricalAnalytics(
                rangeType = rangeType,
                startDateFormatted = fallback.dateFormatted,
                endDateFormatted = fallback.dateFormatted,
                overallAverageApi = fallback.avgApi.toDouble(),
                peakPollutionDay = fallback,
                cleanestDay = fallback,
                totalDays = 1,
                goodDaysCount = 1,
                moderateDaysCount = 0,
                unhealthyDaysCount = 0,
                veryUnhealthyDaysCount = 0,
                dailyEntries = listOf(fallback),
                trendPoints = listOf(
                    TrendPoint(
                        label = fallback.shortDate,
                        dateTimestamp = fallback.dateTimestamp,
                        avgApi = fallback.avgApi.toDouble(),
                        maxApi = fallback.maxApi,
                        minApi = fallback.minApi,
                        level = fallback.level
                    )
                )
            )
        }

        val totalDays = filtered.size
        val overallAvg = filtered.map { it.avgApi }.average()
        val peakDay = filtered.maxByOrNull { it.maxApi } ?: filtered.first()
        val cleanestDay = filtered.minByOrNull { it.minApi } ?: filtered.first()

        var goodCount = 0
        var moderateCount = 0
        var unhealthyCount = 0
        var veryUnhealthyCount = 0

        filtered.forEach { entry ->
            when (entry.level) {
                HazeLevel.GOOD -> goodCount++
                HazeLevel.MODERATE -> moderateCount++
                HazeLevel.UNHEALTHY -> unhealthyCount++
                HazeLevel.VERY_UNHEALTHY, HazeLevel.HAZARDOUS -> veryUnhealthyCount++
            }
        }

        // Build chart trend points (Group by day for 7d/30d, by week for 3m, by month for 1y)
        val trendPoints = mutableListOf<TrendPoint>()
        when (rangeType) {
            DateRangeType.LAST_7_DAYS, DateRangeType.LAST_30_DAYS -> {
                filtered.forEach { entry ->
                    trendPoints.add(
                        TrendPoint(
                            label = entry.shortDate,
                            dateTimestamp = entry.dateTimestamp,
                            avgApi = entry.avgApi.toDouble(),
                            maxApi = entry.maxApi,
                            minApi = entry.minApi,
                            level = entry.level
                        )
                    )
                }
            }
            DateRangeType.LAST_3_MONTHS -> {
                // Group by ~7-day chunks (weeks)
                filtered.chunked(7).forEach { chunk ->
                    val chunkAvg = chunk.map { it.avgApi }.average()
                    val chunkMax = chunk.maxOf { it.maxApi }
                    val chunkMin = chunk.minOf { it.minApi }
                    val midEntry = chunk[chunk.size / 2]
                    trendPoints.add(
                        TrendPoint(
                            label = midEntry.shortDate,
                            dateTimestamp = midEntry.dateTimestamp,
                            avgApi = (chunkAvg * 10.0).roundToInt() / 10.0,
                            maxApi = chunkMax,
                            minApi = chunkMin,
                            level = HazeLevel.fromApi(chunkAvg.roundToInt())
                        )
                    )
                }
            }
            DateRangeType.LAST_1_YEAR -> {
                // Group by Calendar Month
                val groupedByMonth = filtered.groupBy { entry ->
                    val c = Calendar.getInstance().apply { timeInMillis = entry.dateTimestamp }
                    "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}"
                }
                groupedByMonth.forEach { (_, monthEntries) ->
                    val monthAvg = monthEntries.map { it.avgApi }.average()
                    val monthMax = monthEntries.maxOf { it.maxApi }
                    val monthMin = monthEntries.minOf { it.minApi }
                    val firstEntry = monthEntries.first()
                    val c = Calendar.getInstance().apply { timeInMillis = firstEntry.dateTimestamp }
                    val monthName = SimpleDateFormat("MMM", Locale.ENGLISH).format(c.time)
                    trendPoints.add(
                        TrendPoint(
                            label = monthName,
                            dateTimestamp = firstEntry.dateTimestamp,
                            avgApi = (monthAvg * 10.0).roundToInt() / 10.0,
                            maxApi = monthMax,
                            minApi = monthMin,
                            level = HazeLevel.fromApi(monthAvg.roundToInt())
                        )
                    )
                }
            }
            DateRangeType.CUSTOM -> {
                if (filtered.size <= 30) {
                    filtered.forEach { entry ->
                        trendPoints.add(
                            TrendPoint(
                                label = entry.shortDate,
                                dateTimestamp = entry.dateTimestamp,
                                avgApi = entry.avgApi.toDouble(),
                                maxApi = entry.maxApi,
                                minApi = entry.minApi,
                                level = entry.level
                            )
                        )
                    }
                } else {
                    val step = (filtered.size / 20).coerceAtLeast(1)
                    filtered.chunked(step).forEach { chunk ->
                        val chunkAvg = chunk.map { it.avgApi }.average()
                        val chunkMax = chunk.maxOf { it.maxApi }
                        val chunkMin = chunk.minOf { it.minApi }
                        val midEntry = chunk[chunk.size / 2]
                        trendPoints.add(
                            TrendPoint(
                                label = midEntry.shortDate,
                                dateTimestamp = midEntry.dateTimestamp,
                                avgApi = (chunkAvg * 10.0).roundToInt() / 10.0,
                                maxApi = chunkMax,
                                minApi = chunkMin,
                                level = HazeLevel.fromApi(chunkAvg.roundToInt())
                            )
                        )
                    }
                }
            }
        }

        return HistoricalAnalytics(
            rangeType = rangeType,
            startDateFormatted = filtered.first().dateFormatted,
            endDateFormatted = filtered.last().dateFormatted,
            overallAverageApi = (overallAvg * 10.0).roundToInt() / 10.0,
            peakPollutionDay = peakDay,
            cleanestDay = cleanestDay,
            totalDays = totalDays,
            goodDaysCount = goodCount,
            moderateDaysCount = moderateCount,
            unhealthyDaysCount = unhealthyCount,
            veryUnhealthyDaysCount = veryUnhealthyCount,
            dailyEntries = filtered.sortedByDescending { it.dateTimestamp }, // newest first for list display
            trendPoints = trendPoints
        )
    }
}
