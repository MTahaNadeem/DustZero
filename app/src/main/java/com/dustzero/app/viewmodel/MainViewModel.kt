package com.dustzero.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dustzero.app.data.AppDao
import com.dustzero.app.data.ThemeMode
import com.dustzero.app.data.ThemePreferences
import com.dustzero.app.iot.IotService
import com.dustzero.app.models.ThresholdConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val iotService: IotService,
    private val dao: AppDao,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val themeMode = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    val sensorData = iotService.sensorData
    val demoModeEnabled = iotService.demoModeEnabled
    val config = iotService.config
    
    val alerts = dao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val cleaningHistory = dao.getCleaningHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val unreadAlertsCount = alerts.map { it.count { alert -> !alert.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val panelStatus = sensorData.map { data ->
        when {
            !data.connected -> "OFFLINE"
            data.rainDetected -> "RAIN DETECTED"
            data.cleaningState != "IDLE" && data.cleaningState != "READY" && data.cleaningState != "STOPPED" -> "CLEANING"
            !data.sunDetected -> "LOW SUNLIGHT"
            data.solarPower < (config.value.expectedPowerClean * (1.0 - config.value.powerDropTriggerPercent / 100.0)) -> "POSSIBLE DUST"
            else -> "OPTIMAL"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "OPTIMAL")

    fun setDemoMode(enabled: Boolean) = iotService.setDemoMode(enabled)
    
    fun setDemoScenario(scenario: String) = iotService.setDemoScenario(scenario)

    fun startCleaning() {
        viewModelScope.launch {
            iotService.startCleaning()
        }
    }

    fun stopCleaning() {
        viewModelScope.launch {
            iotService.stopCleaning()
        }
    }
    
    fun homeMotor() {
        viewModelScope.launch {
            iotService.homeMotor()
        }
    }
    
    fun updateConfig(config: ThresholdConfig) {
        iotService.updateConfig(config)
    }

    fun markAlertRead(alertId: Int) {
        viewModelScope.launch {
            dao.markAlertRead(alertId)
        }
    }
}
