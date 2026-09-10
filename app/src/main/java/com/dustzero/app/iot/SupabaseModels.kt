package com.dustzero.app.iot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDTO(
    @SerialName("device_id") val deviceId: String,
    val connected: Boolean = false,
    val ldr1: Int = 0,
    val ldr2: Int = 0,
    val temperature: Double = 0.0,
    @SerialName("solar_voltage") val solarVoltage: Double = 0.0,
    @SerialName("solar_current") val solarCurrent: Double = 0.0,
    @SerialName("solar_power") val solarPower: Double = 0.0,
    @SerialName("rain_detected") val rainDetected: Boolean = false,
    @SerialName("sun_detected") val sunDetected: Boolean = false,
    @SerialName("sunlight_level") val sunlightLevel: String = "WEAK",
    @SerialName("cleaning_state") val cleaningState: String = "IDLE",
    @SerialName("cleaning_progress") val cleaningProgress: Int = 0,
    @SerialName("cleaning_steps") val cleaningSteps: Int = 0,
    val fault: Boolean = false
)

@Serializable
data class CommandDTO(
    @SerialName("device_id") val deviceId: String,
    val command: String
)
