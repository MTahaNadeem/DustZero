package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeSunlight
import com.example.ui.theme.RedError
import com.example.viewmodel.MainViewModel
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusCard

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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DustZero",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Smart Solar Panel Cleaning System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val isOnline = sensorData.connected || demoModeEnabled
            val statusDotColor by animateColorAsState(
                targetValue = if (isOnline) GreenPrimary else RedError,
                animationSpec = tween(500)
            )
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (demoModeEnabled) "DEMO ONLINE" else if (isOnline) "ESP32 ONLINE" else "ESP32 OFFLINE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusDotColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOnline) "LIVE" else "No connection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Status
        val (statusColor, statusIcon, statusDesc) = when (panelStatus) {
            "OPTIMAL" -> Triple(GreenPrimary, Icons.Rounded.CheckCircle, "Strong sunlight detected. Panel is producing expected power.")
            "POSSIBLE DUST", "LOW OUTPUT" -> Triple(OrangeSunlight, Icons.Rounded.Warning, "Power output is significantly below the configured clean-panel baseline.")
            "CLEANING REQUIRED" -> Triple(OrangeSunlight, Icons.Rounded.CleaningServices, "Performance degraded. Cleaning is highly recommended.")
            "LOW SUNLIGHT" -> Triple(Color(0xFF64748B), Icons.Rounded.WbCloudy, "Sunlight is too weak for performance baseline check.")
            "RAIN DETECTED" -> Triple(MaterialTheme.colorScheme.secondary, Icons.Rounded.WaterDrop, "Rain detected. Automatic cleaning is blocked.")
            "CLEANING" -> Triple(MaterialTheme.colorScheme.secondary, Icons.Rounded.Autorenew, "Cleaning mechanism is currently running.")
            "OFFLINE" -> Triple(RedError, Icons.Rounded.ErrorOutline, "Waiting for ESP32 sensor data...")
            "ERROR" -> Triple(RedError, Icons.Rounded.Error, "System fault detected. Check alerts.")
            else -> Triple(MaterialTheme.colorScheme.secondary, Icons.Rounded.Info, "Status: $panelStatus")
        }
        
        StatusCard(
            title = "Solar Panel Status",
            status = panelStatus,
            icon = statusIcon,
            color = statusColor,
            description = statusDesc
        )

        // Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                title = "Solar Power",
                value = if (sensorData.connected || demoModeEnabled) String.format("%.2f", sensorData.solarPower) else "--",
                unit = "W",
                icon = Icons.Rounded.WbSunny,
                iconTint = OrangeSunlight,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Voltage",
                value = if (sensorData.connected || demoModeEnabled) String.format("%.2f", sensorData.solarVoltage) else "--",
                unit = "V",
                icon = Icons.Rounded.ElectricBolt,
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                title = "Current",
                value = if (sensorData.connected || demoModeEnabled) String.format("%.1f", sensorData.solarCurrent) else "--",
                unit = "mA",
                icon = Icons.Rounded.BatteryChargingFull,
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Temperature",
                value = if (sensorData.connected || demoModeEnabled) String.format("%.1f", sensorData.temperature) else "--",
                unit = "°C",
                icon = Icons.Rounded.Thermostat,
                iconTint = RedError,
                modifier = Modifier.weight(1f)
            )
        }

        // Sunlight & Rain row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val isStrongSun = sensorData.sunlightLevel == "STRONG"
            StatusCard(
                title = "Sunlight",
                status = if (sensorData.connected || demoModeEnabled) sensorData.sunlightLevel else "--",
                icon = if (isStrongSun) Icons.Rounded.WbSunny else Icons.Rounded.CloudQueue,
                color = if (isStrongSun) OrangeSunlight else Color(0xFF64748B),
                description = if (sensorData.connected || demoModeEnabled) "LDR1: ${sensorData.ldr1}\nLDR2: ${sensorData.ldr2}" else "No data",
                modifier = Modifier.weight(1f)
            )
            
            val isRain = sensorData.rainDetected
            StatusCard(
                title = "Rain",
                status = if (!sensorData.connected && !demoModeEnabled) "--" else if (isRain) "DETECTED" else "NO RAIN",
                icon = if (isRain) Icons.Rounded.WaterDrop else Icons.Rounded.WbSunny,
                color = if (!sensorData.connected && !demoModeEnabled) Color.Gray else if (isRain) MaterialTheme.colorScheme.secondary else GreenPrimary,
                description = if (!sensorData.connected && !demoModeEnabled) "No data" else if (isRain) "Cleaning blocked" else "Safe for cleaning",
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Cleaning Status
        val isCleaning = sensorData.cleaningState != "IDLE" && sensorData.cleaningState != "OFFLINE"
        StatusCard(
            title = "Cleaning Mechanism",
            status = if (!sensorData.connected && !demoModeEnabled) "--" else if (isCleaning) "IN PROGRESS" else "READY",
            icon = Icons.Rounded.CleaningServices,
            color = if (isCleaning) MaterialTheme.colorScheme.secondary else GreenPrimary,
            description = if (isCleaning) "${sensorData.cleaningState} - ${sensorData.cleaningProgress}%" else "No cleaning currently in progress"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

