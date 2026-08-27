package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "haze_readings")
data class HazeReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stationId: String,
    val stationName: String,
    val apiValue: Int,
    val hazeLevelName: String,
    val pm25: Double,
    val pm10: Double,
    val temperature: Double,
    val humidity: Int,
    val locationSource: String,
    val networkPointDetail: String,
    val timestamp: Long
)

@Entity(tableName = "haze_alerts")
data class HazeAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val apiValue: Int,
    val levelName: String,
    val stationName: String,
    val isRead: Boolean = false,
    val timestamp: Long
)
