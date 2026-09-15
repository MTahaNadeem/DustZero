package com.dustzero.app.iot

import com.dustzero.app.models.SensorData
import com.dustzero.app.models.ThresholdConfig
import kotlinx.coroutines.flow.StateFlow

enum class CommandState {
    IDLE, SENDING, SENT, ACKNOWLEDGED, FAILED, TIMED_OUT
}

interface IotService {
    val sensorData: StateFlow<SensorData>
    val demoModeEnabled: StateFlow<Boolean>
    val config: StateFlow<ThresholdConfig>
    val manualCommandStatus: StateFlow<CommandState>

    fun setDemoMode(enabled: Boolean)
    suspend fun startCleaning()
    suspend fun startManualCleaning()
    suspend fun stopCleaning()

    fun updateConfig(newConfig: ThresholdConfig)
    fun setDemoScenario(scenario: String)
    suspend fun getDeviceHistory(rangeHours: Int): List<DeviceHistoryDTO>
    suspend fun refreshConnection(): Boolean

    /**
     * Switches the active device. The implementation must:
     * 1. Cancel any existing Realtime subscription for the previous device_id.
     * 2. Reset sensor data to a clean offline state.
     * 3. Start a new Realtime subscription for [deviceId].
     */
    fun switchDevice(deviceId: String)
}

