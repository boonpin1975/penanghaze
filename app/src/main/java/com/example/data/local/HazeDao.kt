package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HazeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: HazeReadingEntity): Long

    @Query("SELECT * FROM haze_readings ORDER BY timestamp DESC LIMIT 50")
    fun getRecentReadings(): Flow<List<HazeReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: HazeAlertEntity): Long

    @Query("SELECT * FROM haze_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<HazeAlertEntity>>

    @Query("UPDATE haze_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: Long)

    @Query("DELETE FROM haze_alerts")
    suspend fun clearAlerts()
}
