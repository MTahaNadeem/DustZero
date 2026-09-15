package com.dustzero.app.iot

import com.dustzero.app.data.AlertEntity
import com.dustzero.app.data.AppDao
import com.dustzero.app.models.AppConstants
import com.dustzero.app.models.SensorData
import com.dustzero.app.models.ThresholdConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Supabase-backed IoT service.
 *
 * Data flow (inbound — sensor readings):
 *   ESP32-S3 → Wi-Fi → Supabase `devices` table → Realtime subscription → SensorData StateFlow → ViewModel → UI
 *
 * Data flow (outbound — commands):
 *   UI → ViewModel → SupabaseIotService → INSERT into `commands` table → ESP32-S3 polls → executes
 *
 * Online/offline detection:
 *   Device is ONLINE only if: devices.connected == true AND devices.updated_at
 *   was updated within HEARTBEAT_TIMEOUT_MS (30s). The ESP32 firmware must update
 *   `updated_at` on every sensor write; if it crashes without clearing `connected`,
 *   the heartbeat window will catch it within 30s.
 *
 * Device switching:
 *   Call [switchDevice] to change the active device. The existing Realtime channel
 *   is unsubscribed, sensorData is reset to an offline state, and a new channel is
 *   subscribed for the new device_id. No stale data leaks from the previous device.
 *
 * Alert generation:
 *   There is NO `alerts` table in the current Supabase schema. Alerts are generated
 *   client-side by watching state changes in the Realtime subscription and inserting
 *   into the local Room database. This can be replaced with a real Supabase alerts
 *   table in a future schema migration — see generateAlertsForStateChange().
 *
 * Analytics:
 *   `device_history` table is queried for analytics charts when a device is selected.
 *
 * Fallback:
 *   If SUPABASE_URL / SUPABASE_KEY are placeholder values (unconfigured), the service
 *   automatically activates DemoIotService as a fallback.
 */
class SupabaseIotService(
    private val fallbackDemoService: IotService,
    private val dao: AppDao,
    initialDeviceId: String?
) : IotService {

    private val scope = CoroutineScope(Dispatchers.IO)

    // The currently active device_id. Null = no device selected yet.
    @Volatile
    private var currentDeviceId: String? = initialDeviceId

    private val _sensorData = MutableStateFlow(SensorData(deviceId = initialDeviceId ?: ""))
    override val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _demoModeEnabled = MutableStateFlow(false)
    override val demoModeEnabled: StateFlow<Boolean> = _demoModeEnabled.asStateFlow()

    override val config: StateFlow<ThresholdConfig> = fallbackDemoService.config

    private val isConfigured = SupabaseClientProvider.isConfigured
    private val supabase = SupabaseClientProvider.client

    // Tracks the active Realtime channel and subscription job so we can cancel them on switchDevice
    private var activeChannel: RealtimeChannel? = null
    private var subscriptionJob: Job? = null

    init {
        if (isConfigured) {
            if (currentDeviceId != null) {
                startRealtimeSubscription(currentDeviceId!!)
            }
            // else: no device selected yet — wait for switchDevice() to be called
        } else {
            // Supabase not configured → fall back to Demo Mode automatically
            setDemoMode(true)
        }

        // Mirror demo service data into _sensorData whenever demo mode is active
        scope.launch {
            fallbackDemoService.sensorData.collect { demoData ->
                if (_demoModeEnabled.value) {
                    _sensorData.value = demoData
                }
            }
        }
    }

    // ─── Device Switching ─────────────────────────────────────────────────────

    /**
     * Switches the active device.
     *
     * 1. Cancels the existing Realtime subscription (no stale data leaks).
     * 2. Resets sensorData to a clean offline state tagged with the new device_id.
     * 3. Starts a new Realtime subscription for [deviceId].
     */
    override fun switchDevice(deviceId: String) {
        if (deviceId == currentDeviceId) return // Already on this device

        currentDeviceId = deviceId

        // Cancel previous subscription
        subscriptionJob?.cancel()
        subscriptionJob = null

        // Unsubscribe previous channel
        scope.launch {
            try { activeChannel?.unsubscribe() } catch (_: Exception) {}
            activeChannel = null
        }

        // Reset to clean offline state for the new device
        _sensorData.value = SensorData(deviceId = deviceId, connected = false, isOnline = false)

        if (isConfigured && !_demoModeEnabled.value) {
            startRealtimeSubscription(deviceId)
        }
    }

    // ─── Realtime Subscription ────────────────────────────────────────────────

    private fun startRealtimeSubscription(deviceId: String) {
        subscriptionJob = scope.launch {
            try {
                // 1. Initial fetch — populate UI before the first Realtime event fires
                fetchAndApplyDevice(deviceId)

                // 2. Subscribe to UPDATE events on the devices table for this device.
                val channel = supabase.channel("devices:$deviceId")
                activeChannel = channel

                val changes = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = AppConstants.TABLE_DEVICES
                }

                channel.subscribe()

                // 3. On every Realtime update event, decode the payload
                changes.collect { action ->
                    if (!_demoModeEnabled.value && currentDeviceId == deviceId) {
                        try {
                            val dto = action.decodeRecordOrNull<DeviceDTO>()
                            // Filter: only accept updates for our currently selected device_id
                            if (dto != null && dto.deviceId == deviceId) {
                                val previousData = _sensorData.value
                                val newData = dtoToSensorData(dto)
                                _sensorData.value = newData
                                generateAlertsForStateChange(previousData, newData, deviceId)
                            } else {
                                // Fallback if partial update or decode fails
                                fetchAndApplyDevice(deviceId)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            fetchAndApplyDevice(deviceId)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!_demoModeEnabled.value) {
                    _sensorData.update { it.copy(connected = false, isOnline = false) }
                    dao.insertAlert(
                        AlertEntity(
                            deviceId = deviceId,
                            type = "Connection",
                            severity = "CRITICAL",
                            message = "CLOUD CONNECTION LOST - Unable to reach Supabase. Check internet connectivity."
                        )
                    )
                }
            }
        }
    }

    /**
     * Fetches the current `devices` row for [deviceId] and applies it to [_sensorData].
     */
    private suspend fun fetchAndApplyDevice(deviceId: String) {
        try {
            val dto = supabase.from(AppConstants.TABLE_DEVICES)
                .select { filter { eq("device_id", deviceId) } }
                .decodeSingleOrNull<DeviceDTO>()

            if (dto != null) {
                val previousData = _sensorData.value
                val newData = dtoToSensorData(dto)
                _sensorData.value = newData
                generateAlertsForStateChange(previousData, newData, deviceId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Converts a [DeviceDTO] from Supabase into a [SensorData] domain model.
     *
     * Online detection: combines `connected` column with heartbeat freshness.
     * If updated_at is null (device record was just created), fall back to `connected` alone.
     */
    private fun dtoToSensorData(dto: DeviceDTO): SensorData {
        val updatedAtMs = parseIso8601ToMs(dto.updatedAt)
        val heartbeatFresh = updatedAtMs > 0L &&
                (System.currentTimeMillis() - updatedAtMs) < AppConstants.HEARTBEAT_TIMEOUT_MS
        val isOnline = dto.connected && heartbeatFresh

        return SensorData(
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
            fault = dto.fault,
            updatedAtMs = updatedAtMs,
            isOnline = isOnline
        )
    }

    // ─── Client-Side Alert Generation ─────────────────────────────────────────
    //
    // NOTE: There is NO `alerts` table in the current Supabase schema.
    // This function watches for meaningful state transitions in the Realtime
    // feed and inserts alerts into the local Room database.
    //
    // FUTURE: When a `device_alerts` table is added to the schema, replace this
    // function with a Realtime subscription on that table and remove local inserts.

    private fun generateAlertsForStateChange(prev: SensorData, next: SensorData, deviceId: String) {
        scope.launch {
            // Rain detected (transition: false → true)
            if (!prev.rainDetected && next.rainDetected) {
                dao.insertAlert(AlertEntity(
                    deviceId = deviceId,
                    type = "Safety",
                    severity = "WARNING",
                    message = "RAIN DETECTED - Automatic cleaning is temporarily blocked for panel protection."
                ))
            }
            // Rain cleared (transition: true → false)
            if (prev.rainDetected && !next.rainDetected) {
                dao.insertAlert(AlertEntity(
                    deviceId = deviceId,
                    type = "Safety",
                    severity = "INFO",
                    message = "RAIN CLEARED - Cleaning operations can resume."
                ))
            }
            // Fault asserted (false → true)
            if (!prev.fault && next.fault) {
                dao.insertAlert(AlertEntity(
                    deviceId = deviceId,
                    type = "Fault",
                    severity = "CRITICAL",
                    message = "SYSTEM FAULT - The ESP32 has reported a hardware fault. Manual inspection required."
                ))
            }
            // Device went offline
            if (prev.isOnline && !next.isOnline) {
                dao.insertAlert(AlertEntity(
                    deviceId = deviceId,
                    type = "Connection",
                    severity = "CRITICAL",
                    message = "DEVICE OFFLINE - ESP32 stopped sending heartbeats. Check Wi-Fi and power."
                ))
            }
            // Device came back online
            if (!prev.isOnline && next.isOnline) {
                dao.insertAlert(AlertEntity(
                    deviceId = deviceId,
                    type = "Connection",
                    severity = "INFO",
                    message = "DEVICE ONLINE - ESP32 reconnected to cloud."
                ))
            }
            // Cleaning cycle completed (MOVING_UP → IDLE)
            if (prev.cleaningState == AppConstants.STATE_MOVING_UP
                && next.cleaningState == AppConstants.STATE_IDLE) {
                dao.insertAlert(AlertEntity(
                    deviceId = deviceId,
                    type = "Cleaning",
                    severity = "INFO",
                    message = "CLEANING COMPLETED - Cleaning cycle completed successfully."
                ))
            }
        }
    }

    // ─── Command Service ──────────────────────────────────────────────────────

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

    /**
     * Inserts a command row into the `commands` table with status = PENDING.
     * The ESP32 firmware polls this table, executes the command, and updates
     * status to ACKNOWLEDGED / COMPLETED / FAILED.
     */
    private suspend fun sendCommand(command: String) {
        val deviceId = currentDeviceId ?: return
        try {
            val cmd = CommandDTO(
                deviceId = deviceId,
                command = command,
                status = "PENDING"
            )
            supabase.from(AppConstants.TABLE_COMMANDS).insert(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
            dao.insertAlert(AlertEntity(
                deviceId = deviceId,
                type = "Command",
                severity = "WARNING",
                message = "COMMAND FAILED - Could not send '$command' to device. Check connectivity."
            ))
        }
    }

    // ─── Demo Mode ────────────────────────────────────────────────────────────

    override fun setDemoMode(enabled: Boolean) {
        _demoModeEnabled.value = enabled
        fallbackDemoService.setDemoMode(enabled)
        if (!enabled && isConfigured) {
            val deviceId = currentDeviceId
            if (deviceId != null) {
                scope.launch { fetchAndApplyDevice(deviceId) }
            }
        } else if (!enabled && !isConfigured) {
            _sensorData.update { it.copy(connected = false, isOnline = false) }
        }
    }

    override fun updateConfig(newConfig: ThresholdConfig) {
        fallbackDemoService.updateConfig(newConfig)
    }

    override fun setDemoScenario(scenario: String) {
        fallbackDemoService.setDemoScenario(scenario)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Parses an ISO-8601 UTC timestamp string (e.g. "2026-09-11T14:30:00+00:00")
     * returned by Supabase into epoch milliseconds for heartbeat comparison.
     * Returns 0L if the string is null or cannot be parsed.
     */
    private fun parseIso8601ToMs(iso: String?): Long {
        if (iso == null) return 0L
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    return sdf.parse(iso)?.time ?: continue
                } catch (_: Exception) { continue }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun getDeviceHistory(rangeHours: Int): List<DeviceHistoryDTO> {
        if (_demoModeEnabled.value || !isConfigured) return fallbackDemoService.getDeviceHistory(rangeHours)
        val deviceId = currentDeviceId ?: return emptyList()

        return try {
            val now = System.currentTimeMillis()
            val cutoff = now - (rangeHours * 60 * 60 * 1000L)
            val isoCutoff = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(cutoff)

            supabase.from("device_history")
                .select {
                    filter {
                        eq("device_id", deviceId)
                        gte("recorded_at", isoCutoff)
                    }
                    order("recorded_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<DeviceHistoryDTO>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun refreshConnection(): Boolean {
        if (_demoModeEnabled.value || !isConfigured) return fallbackDemoService.refreshConnection()
        val deviceId = currentDeviceId ?: return false

        return try {
            fetchAndApplyDevice(deviceId)
            _sensorData.value.isOnline
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
