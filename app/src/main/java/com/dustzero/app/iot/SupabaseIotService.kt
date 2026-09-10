package com.dustzero.app.iot

import com.dustzero.app.BuildConfig
import com.dustzero.app.models.AppConstants
import com.dustzero.app.models.SensorData
import com.dustzero.app.models.ThresholdConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Supabase-backed IoT service.
 *
 * Architecture:
 *   ESP32-S3 → Wi-Fi → Supabase (devices table) → Realtime → This service → ViewModel → UI
 *
 * Commands flow in reverse:
 *   UI → ViewModel → This service → Supabase (commands table) → ESP32-S3
 *
 * If Supabase is not configured (placeholder .env.example values), the service
 * automatically falls back to DemoIotService.
 */
class SupabaseIotService(
    private val fallbackDemoService: IotService
) : IotService {

    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _sensorData = MutableStateFlow(SensorData())
    override val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _demoModeEnabled = MutableStateFlow(false)
    override val demoModeEnabled: StateFlow<Boolean> = _demoModeEnabled.asStateFlow()

    override val config: StateFlow<ThresholdConfig> = fallbackDemoService.config

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY
    
    private val isConfigured = supabaseUrl.isNotBlank()
            && supabaseKey.isNotBlank()
            && supabaseUrl != "null"
            && supabaseUrl != "https://xyzcompany.supabase.co"

    private val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }

    init {
        if (isConfigured) {
            startRealtimeUpdates()
        } else {
            // Fallback to Demo Mode if Supabase is not configured
            setDemoMode(true)
        }
        
        // Listen to demo service data when demo mode is active
        scope.launch {
            fallbackDemoService.sensorData.collect { demoData ->
                if (_demoModeEnabled.value) {
                    _sensorData.value = demoData
                }
            }
        }
    }

    private fun startRealtimeUpdates() {
        scope.launch {
            try {
                // Initial fetch on connect
                val initialData = supabase.from(AppConstants.TABLE_DEVICES)
                    .select { filter { eq("device_id", AppConstants.DEVICE_ID) } }
                    .decodeSingleOrNull<DeviceDTO>()
                
                initialData?.let { updateLocalSensorData(it) }

                // Subscribe to realtime changes
                val channel = supabase.channel("public:${AppConstants.TABLE_DEVICES}")
                val changes = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = AppConstants.TABLE_DEVICES
                }

                channel.subscribe()

                changes.collect { _ ->
                    if (!_demoModeEnabled.value) {
                        try {
                            val currentData = supabase.from(AppConstants.TABLE_DEVICES)
                                .select { filter { eq("device_id", AppConstants.DEVICE_ID) } }
                                .decodeSingleOrNull<DeviceDTO>()
                            currentData?.let { updateLocalSensorData(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!_demoModeEnabled.value) {
                    _sensorData.update { it.copy(connected = false, cleaningState = "OFFLINE") }
                }
            }
        }
    }

    private fun updateLocalSensorData(dto: DeviceDTO) {
        _sensorData.update {
            it.copy(
                deviceId = dto.deviceId,
                connected = dto.connected,
                ldr1 = dto.ldr1,
                ldr2 = dto.ldr2,
                temperature = dto.temperature,
                solarVoltage = dto.solarVoltage,
                solarCurrent = dto.solarCurrent,
                solarPower = dto.solarPower,
                rainDetected = dto.rainDetected,
                sunDetected = dto.sunDetected,
                sunlightLevel = dto.sunlightLevel,
                cleaningState = dto.cleaningState,
                cleaningProgress = dto.cleaningProgress,
                cleaningSteps = dto.cleaningSteps,
                fault = dto.fault
            )
        }
    }

    override fun setDemoMode(enabled: Boolean) {
        _demoModeEnabled.value = enabled
        fallbackDemoService.setDemoMode(enabled)
        if (!enabled && isConfigured) {
            // Re-fetch real data when exiting demo mode
            scope.launch {
                try {
                    val currentData = supabase.from(AppConstants.TABLE_DEVICES)
                        .select { filter { eq("device_id", AppConstants.DEVICE_ID) } }
                        .decodeSingleOrNull<DeviceDTO>()
                    currentData?.let { updateLocalSensorData(it) }
                } catch (e: Exception) {
                    _sensorData.update { it.copy(connected = false, cleaningState = "OFFLINE") }
                }
            }
        } else if (!enabled && !isConfigured) {
            // Cannot exit demo mode if Supabase is not configured
            _sensorData.update { it.copy(connected = false, cleaningState = "OFFLINE (SUPABASE NOT CONFIGURED)") }
        }
    }

    override suspend fun startCleaning() {
        if (_demoModeEnabled.value || !isConfigured) {
            fallbackDemoService.startCleaning()
        } else {
            sendCommand(AppConstants.CMD_START_CLEANING)
        }
    }

    override suspend fun stopCleaning() {
        if (_demoModeEnabled.value || !isConfigured) {
            fallbackDemoService.stopCleaning()
        } else {
            sendCommand(AppConstants.CMD_STOP_CLEANING)
        }
    }

    override suspend fun homeMotor() {
        if (_demoModeEnabled.value || !isConfigured) {
            fallbackDemoService.homeMotor()
        } else {
            sendCommand(AppConstants.CMD_HOME_MOTOR)
        }
    }
    
    private suspend fun sendCommand(command: String) {
        try {
            val cmd = CommandDTO(deviceId = AppConstants.DEVICE_ID, command = command)
            supabase.from(AppConstants.TABLE_COMMANDS).insert(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateConfig(newConfig: ThresholdConfig) {
        fallbackDemoService.updateConfig(newConfig)
    }

    override fun setDemoScenario(scenario: String) {
        fallbackDemoService.setDemoScenario(scenario)
    }
}
