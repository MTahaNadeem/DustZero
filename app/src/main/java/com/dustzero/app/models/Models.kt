package com.dustzero.app.models

import com.dustzero.app.models.AppConstants

data class SensorData(
    val deviceId: String = AppConstants.DEVICE_ID,
    val connected: Boolean = true,
    val ldr1: Int = 47,
    val ldr2: Int = 71,
    val temperature: Double = 38.19,
    val solarVoltage: Double = 0.85,
    val solarCurrent: Double = 124.5,
    val solarPower: Double = 0.11,
    val rainDetected: Boolean = false,
    val sunDetected: Boolean = true,
    val sunlightLevel: String = "STRONG",
    val cleaningState: String = "IDLE",
    val cleaningProgress: Int = 0,
    val cleaningSteps: Int = 0,
    val fault: Boolean = false
)

data class ThresholdConfig(
    val sunDetectionThreshold: Int = 200,
    val expectedPowerClean: Double = 0.12,
    val powerDropTriggerPercent: Double = 40.0,
    val rainProtectionEnabled: Boolean = true,
    val autoCleaningEnabled: Boolean = true,
    val cleaningDistanceSteps: Int = 500,
    val cleaningCooldownMs: Long = 30 * 60 * 1000 // 30 mins
)
