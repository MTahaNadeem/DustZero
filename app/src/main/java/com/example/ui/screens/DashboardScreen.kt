package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val panelStatus by viewModel.panelStatus.collectAsStateWithLifecycle()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SolarClean AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Smart Solar Monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (demoModeEnabled) Color(0xFF03A9F4) else if (sensorData.connected) Color(0xFF0F9D58) else Color(0xFFB3261E),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (demoModeEnabled) "DEMO MODE" else if (sensorData.connected) "ONLINE" else "OFFLINE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (demoModeEnabled) Color(0xFF03A9F4) else if (sensorData.connected) Color(0xFF0F9D58) else Color(0xFFB3261E)
                )
            }
        }

        Divider()

        // Hero Status
        val statusColor = when (panelStatus) {
            "OPTIMAL" -> Color(0xFF0F9D58)
            "POSSIBLE DUST", "LOW SUNLIGHT" -> Color(0xFFF2A900)
            "RAIN DETECTED", "OFFLINE" -> Color(0xFFB3261E)
            "CLEANING" -> Color(0xFF03A9F4)
            else -> MaterialTheme.colorScheme.primary
        }
        val statusIcon = when (panelStatus) {
            "OPTIMAL" -> Icons.Default.CheckCircle
            "POSSIBLE DUST" -> Icons.Default.Warning
            "LOW SUNLIGHT" -> Icons.Default.WbCloudy
            "RAIN DETECTED" -> Icons.Default.Error
            "CLEANING" -> Icons.Default.Info
            "OFFLINE" -> Icons.Default.Error
            else -> Icons.Default.Info
        }
        
        com.example.ui.components.StatusCard(
            title = "Solar Panel Status",
            status = panelStatus,
            icon = statusIcon,
            color = statusColor,
            description = when (panelStatus) {
                "OPTIMAL" -> "Panel is producing expected power."
                "POSSIBLE DUST" -> "Power output is significantly below the configured clean-panel baseline."
                "LOW SUNLIGHT" -> "Sunlight is too weak for performance baseline check."
                "RAIN DETECTED" -> "Rain detected. Automatic cleaning is blocked."
                "CLEANING" -> "Cleaning mechanism is currently running."
                "OFFLINE" -> "Waiting for ESP32 sensor data..."
                else -> null
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            com.example.ui.components.MetricCard(
                title = "Power",
                value = if (sensorData.connected) sensorData.solarPower.toString() else "--",
                unit = "W",
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f)
            )
            com.example.ui.components.MetricCard(
                title = "Voltage",
                value = if (sensorData.connected) sensorData.solarVoltage.toString() else "--",
                unit = "V",
                icon = Icons.Default.ElectricMeter,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            com.example.ui.components.MetricCard(
                title = "Current",
                value = if (sensorData.connected) sensorData.solarCurrent.toString() else "--",
                unit = "mA",
                icon = Icons.Default.ElectricMeter,
                modifier = Modifier.weight(1f)
            )
            com.example.ui.components.MetricCard(
                title = "Temp",
                value = if (sensorData.connected) sensorData.temperature.toString() else "--",
                unit = "°C",
                icon = Icons.Default.DeviceThermostat,
                modifier = Modifier.weight(1f)
            )
        }

        // Sunlight & Rain
        com.example.ui.components.StatusCard(
            title = "Sunlight",
            status = if (sensorData.connected) sensorData.sunlightLevel else "--",
            icon = Icons.Default.WbSunny,
            color = Color(0xFFF2A900),
            description = if (sensorData.connected) "LDR1: ${sensorData.ldr1}   LDR2: ${sensorData.ldr2}" else null,
            modifier = Modifier.fillMaxWidth()
        )
        
        com.example.ui.components.StatusCard(
            title = "Rain",
            status = if (!sensorData.connected) "--" else if (sensorData.rainDetected) "RAIN DETECTED" else "NO RAIN",
            icon = Icons.Default.WbCloudy,
            color = if (!sensorData.connected) Color.Gray else if (sensorData.rainDetected) Color(0xFF03A9F4) else Color(0xFF0F9D58),
            modifier = Modifier.fillMaxWidth()
        )

        // Cleaning Status
        com.example.ui.components.StatusCard(
            title = "Cleaning",
            status = sensorData.cleaningState,
            icon = Icons.Default.Info,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
