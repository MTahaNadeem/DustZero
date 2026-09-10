package com.dustzero.app.iot

import com.dustzero.app.data.AlertEntity
import com.dustzero.app.data.AppDao
import com.dustzero.app.data.CleaningHistoryEntity
import com.dustzero.app.models.AppConstants
import com.dustzero.app.models.SensorData
import com.dustzero.app.models.ThresholdConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DemoIotService(private val dao: AppDao) : IotService {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _sensorData = MutableStateFlow(SensorData())
    override val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _demoModeEnabled = MutableStateFlow(true)
    override val demoModeEnabled: StateFlow<Boolean> = _demoModeEnabled.asStateFlow()

    private val _config = MutableStateFlow(ThresholdConfig())
    override val config: StateFlow<ThresholdConfig> = _config.asStateFlow()

    override fun setDemoMode(enabled: Boolean) {
        _demoModeEnabled.value = enabled
        if (!enabled) {
            _sensorData.update { it.copy(connected = false, cleaningState = "OFFLINE") }
        } else {
            setDemoScenario("NORMAL")
        }
    }

    override suspend fun startCleaning() {
        if (_sensorData.value.rainDetected) return
        if (_sensorData.value.cleaningState != "IDLE" && _sensorData.value.cleaningState != "READY") return
        
        val startTime = System.currentTimeMillis()
        
        _sensorData.update { it.copy(cleaningState = "MOVING DOWN", cleaningProgress = 0) }
        simulateProgress(0..50)
        
        _sensorData.update { it.copy(cleaningState = "PAUSED AT BOTTOM") }
        delay(2000)
        
        _sensorData.update { it.copy(cleaningState = "MOVING UP") }
        simulateProgress(50..100)
        
        _sensorData.update { it.copy(cleaningState = "READY", cleaningProgress = 0, solarPower = _config.value.expectedPowerClean, solarVoltage = 0.9, solarCurrent = 133.0) }
        
        val endTime = System.currentTimeMillis()
        dao.insertCleaningHistory(
            CleaningHistoryEntity(
                triggerType = "Manual",
                startTime = startTime,
                endTime = endTime,
                status = "Completed",
                durationSeconds = (endTime - startTime) / 1000
            )
        )
    }

    override suspend fun stopCleaning() {
        _sensorData.update { it.copy(cleaningState = "STOPPED", cleaningProgress = 0) }
    }

    override suspend fun homeMotor() {
        _sensorData.update { it.copy(cleaningState = "READY", cleaningProgress = 0) }
    }

    override fun updateConfig(newConfig: ThresholdConfig) {
        _config.value = newConfig
    }

    override fun setDemoScenario(scenario: String) {
        when (scenario) {
            "NORMAL" -> {
                _sensorData.update { 
                    it.copy(connected = true, ldr1 = 47, ldr2 = 71, rainDetected = false, sunDetected = true, sunlightLevel = "STRONG", solarPower = 0.11, cleaningState = "READY") 
                }
            }
            "DUST" -> {
                _sensorData.update { 
                    it.copy(connected = true, ldr1 = 47, ldr2 = 71, rainDetected = false, sunDetected = true, sunlightLevel = "STRONG", solarPower = 0.06, cleaningState = "READY") 
                }
                scope.launch {
                    dao.insertAlert(AlertEntity(type = "Performance", severity = "WARNING", message = "Possible dust detected. Power output is significantly below baseline."))
                }
            }
            "CLOUDY" -> {
                _sensorData.update { 
                    it.copy(connected = true, ldr1 = 850, ldr2 = 910, rainDetected = false, sunDetected = false, sunlightLevel = "WEAK", solarPower = 0.03, cleaningState = "READY") 
                }
            }
            "RAIN" -> {
                _sensorData.update { 
                    it.copy(connected = true, rainDetected = true, sunDetected = false, sunlightLevel = "WEAK", solarPower = 0.01, cleaningState = "READY") 
                }
                scope.launch {
                    dao.insertAlert(AlertEntity(type = "Safety", severity = "INFO", message = "Rain detected. Automatic cleaning blocked."))
                }
            }
            "OFFLINE" -> {
                _sensorData.update { it.copy(connected = false, cleaningState = "OFFLINE") }
                scope.launch {
                    dao.insertAlert(AlertEntity(type = "Connection", severity = "CRITICAL", message = "ESP32 disconnected."))
                }
            }
        }
    }
    
    private suspend fun simulateProgress(range: IntRange) {
        val totalSteps = config.value.cleaningDistanceSteps
        for (i in range) {
            if (_sensorData.value.cleaningState == "STOPPED") break
            delay(100)
            val currentSteps = (totalSteps * (i / 100.0)).toInt()
            _sensorData.update { it.copy(cleaningProgress = i, cleaningSteps = currentSteps) }
        }
    }
}
