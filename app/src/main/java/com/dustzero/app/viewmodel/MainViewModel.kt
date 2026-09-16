package com.dustzero.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dustzero.app.data.AppDao
import com.dustzero.app.data.ClaimResult
import com.dustzero.app.data.DevicePreferences
import com.dustzero.app.data.DeviceRepository
import com.dustzero.app.data.DeviceSummary
import com.dustzero.app.data.ThemeMode
import com.dustzero.app.data.ThemePreferences
import com.dustzero.app.iot.IotService
import com.dustzero.app.models.AppConstants
import com.dustzero.app.models.ThresholdConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dustzero.app.iot.DeviceHistoryDTO
import com.dustzero.app.data.AuthRepository

class MainViewModel(
    private val iotService: IotService,
    private val dao: AppDao,
    private val themePreferences: ThemePreferences,
    val authRepository: AuthRepository,
    private val devicePreferences: DevicePreferences,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    val themeMode = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    val sensorData = iotService.sensorData
    val demoModeEnabled = iotService.demoModeEnabled
    val config = iotService.config
    val manualCommandStatus = iotService.manualCommandStatus

    val currentUser = authRepository.currentUser

    // True once the initial session check (run during splash) has completed.
    // Navigation must NOT redirect to Login until this is true.
    private val _sessionCheckComplete = MutableStateFlow(false)
    val sessionCheckComplete: StateFlow<Boolean> = _sessionCheckComplete.asStateFlow()

    suspend fun checkAndRestoreSession() {
        authRepository.checkSession()
        _sessionCheckComplete.value = true
    }

    // ─── Active Device ────────────────────────────────────────────────────────

    /** The currently selected device_id. Null = no device selected yet. */
    val activeDeviceId: StateFlow<String?> = devicePreferences.activeDeviceId

    // ─── Owned Device List ────────────────────────────────────────────────────

    private val _ownedDevices = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val ownedDevices: StateFlow<List<DeviceSummary>> = _ownedDevices.asStateFlow()

    private val _deviceListLoading = MutableStateFlow(false)
    val deviceListLoading: StateFlow<Boolean> = _deviceListLoading.asStateFlow()

    private val _deviceListError = MutableStateFlow<String?>(null)
    val deviceListError: StateFlow<String?> = _deviceListError.asStateFlow()

    /** Fetches the list of devices owned by the current user from Supabase. */
    fun loadOwnedDevices() {
        viewModelScope.launch {
            _deviceListLoading.value = true
            _deviceListError.value = null
            try {
                _ownedDevices.value = deviceRepository.getOwnedDevices()
            } catch (e: Exception) {
                _deviceListError.value = "Could not load devices: ${e.message}"
            } finally {
                _deviceListLoading.value = false
            }
        }
    }

    /**
     * Switches to the device with [deviceId] as the active device.
     * Persists the selection and triggers an immediate Realtime subscription switch.
     */
    fun selectDevice(deviceId: String) {
        devicePreferences.setActiveDevice(deviceId)
        iotService.switchDevice(deviceId)
    }

    /**
     * Claims the device with [deviceId] for the current user (or switches to it if already owned).
     * On success: saves it as the active device, switches Realtime subscription, refreshes device list.
     * [onResult] is called on the main thread with the final [ClaimResult].
     */
    fun claimDevice(deviceId: String, onResult: (ClaimResult) -> Unit) {
        viewModelScope.launch {
            val result = deviceRepository.claimDevice(deviceId)
            if (result is ClaimResult.Success || result is ClaimResult.AlreadyOwned) {
                // Select the newly claimed / confirmed device
                selectDevice(deviceId)
                // Refresh the owned devices list so dropdown includes it
                loadOwnedDevices()
            }
            onResult(result)
        }
    }

    /**
     * Signs out the current user, clears the active device selection, and resets state.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            devicePreferences.clearActiveDevice()
            _ownedDevices.value = emptyList()
        }
    }

    // ─── Derived device state ─────────────────────────────────────────────────

    /**
     * True when the device is genuinely reachable: connected==true AND
     * updated_at heartbeat is fresh (within HEARTBEAT_TIMEOUT_MS).
     */
    val isDeviceOnline = sensorData.map { it.isOnline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * True when devices.fault == true. Surface this as a red banner on Dashboard
     * and as an entry in the alerts list.
     */
    val hasFault = sensorData.map { it.fault }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Panel status string derived from live sensor data.
     * Drives the hero status card on Dashboard.
     */
    val panelStatus = sensorData.map { data ->
        when {
            data.fault -> "FAULT"
            !data.isOnline -> "OFFLINE"
            data.rainDetected -> "RAIN DETECTED"
            AppConstants.isActivelyCleaning(data.cleaningState) -> "CLEANING"
            !data.sunDetected -> "LOW SUNLIGHT"
            data.solarPower < (config.value.expectedPowerClean *
                    (1.0 - config.value.powerDropTriggerPercent / 100.0)) -> "POSSIBLE DUST"
            else -> "OPTIMAL"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "OPTIMAL")

    // ─── Efficiency (Mock logic) ──────────────────────────────────────────────

    val baselineExists = MutableStateFlow(true)
    val panelEfficiency = MutableStateFlow(78)

    // ─── Alerts (local Room DB) ────────────────────────────────────────────────
    // NOTE: There is no `alerts` table in Supabase. Alerts are generated
    // client-side by SupabaseIotService watching state changes, and stored locally.

    val alerts = dao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cleaningHistory = dao.getCleaningHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlertsCount = alerts.map { it.count { alert -> !alert.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Commands ─────────────────────────────────────────────────────────────

    private val _systemStopped = MutableStateFlow(false)
    val systemStopped = _systemStopped.asStateFlow()

    fun startCleaning() {
        _systemStopped.value = false
        viewModelScope.launch {
            iotService.startCleaning()
        }
    }

    fun startManualCleaning() {
        _systemStopped.value = false
        viewModelScope.launch {
            iotService.startManualCleaning()
        }
    }

    fun stopCleaning() {
        _systemStopped.value = true
        viewModelScope.launch {
            iotService.stopCleaning()
        }
    }

    fun updateConfig(config: ThresholdConfig) {
        iotService.updateConfig(config)
    }

    suspend fun getDeviceHistory(rangeHours: Int): List<DeviceHistoryDTO> {
        return iotService.getDeviceHistory(rangeHours)
    }

    fun refreshConnection(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isOnline = iotService.refreshConnection()
            onResult(isOnline)
        }
    }

    // ─── Demo mode ────────────────────────────────────────────────────────────

    fun setDemoMode(enabled: Boolean) = iotService.setDemoMode(enabled)

    fun setDemoScenario(scenario: String) = iotService.setDemoScenario(scenario)

    // ─── Alerts ───────────────────────────────────────────────────────────────

    fun markAlertRead(alertId: Int) {
        viewModelScope.launch {
            dao.markAlertRead(alertId)
        }
    }
}
