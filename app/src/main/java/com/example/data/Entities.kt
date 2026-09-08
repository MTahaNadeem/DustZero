package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String = "solarclean-001",
    val type: String,
    val severity: String, // INFO, WARNING, CRITICAL
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

@Entity(tableName = "cleaning_history")
data class CleaningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String = "solarclean-001",
    val triggerType: String, // Manual, Automatic
    val startTime: Long,
    val endTime: Long,
    val status: String,
    val durationSeconds: Long
)
