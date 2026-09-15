package com.dustzero.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** The device_id this alert belongs to. Set at insertion time from the active device. */
    val deviceId: String = "",
    val type: String,
    val severity: String, // INFO, WARNING, CRITICAL
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

@Entity(tableName = "cleaning_history")
data class CleaningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** The device_id this cleaning history entry belongs to. Set at insertion time. */
    val deviceId: String = "",
    val triggerType: String, // Manual, Automatic
    val startTime: Long,
    val endTime: Long,
    val status: String,
    val durationSeconds: Long
)

