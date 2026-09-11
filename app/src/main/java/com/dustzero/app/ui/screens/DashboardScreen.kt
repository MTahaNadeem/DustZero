package com.dustzero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.dustzero.app.ui.theme.DangerRed
import com.dustzero.app.ui.theme.PrimaryGreen
import com.dustzero.app.ui.theme.WarningAmber
import com.dustzero.app.viewmodel.MainViewModel
import com.dustzero.app.ui.components.MetricCard
import com.dustzero.app.ui.components.StatusCard

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val scrollState = rememberScrollState()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val panelStatus by viewModel.panelStatus.collectAsStateWithLifecycle()

    // Derive display values from live sensor data
    val isConnected = sensorData.connected
    val isCleaning = sensorData.cleaningState != "IDLE"
            && sensorData.cleaningState != "READY"
            && sensorData.cleaningState != "OFFLINE"

    val heroColor = when (panelStatus) {
        "OPTIMAL" -> PrimaryGreen
        "CLEANING" -> MaterialTheme.colorScheme.secondary
        "POSSIBLE DUST" -> WarningAmber
        "RAIN DETECTED" -> MaterialTheme.colorScheme.secondary
        "LOW SUNLIGHT" -> WarningAmber
        else -> DangerRed // OFFLINE
    }
    val heroIcon = when (panelStatus) {
        "OPTIMAL" -> Icons.Rounded.CheckCircle
        "CLEANING" -> Icons.Rounded.CleaningServices
        "POSSIBLE DUST" -> Icons.Rounded.Warning
        "RAIN DETECTED" -> Icons.Rounded.CloudQueue
        "LOW SUNLIGHT" -> Icons.Rounded.WbCloudy
        else -> Icons.Rounded.CloudOff
    }
    val heroDescription = when (panelStatus) {
        "OPTIMAL" -> "Strong sunlight detected. Panel performance is ideal."
        "CLEANING" -> "Cleaning cycle is currently active."
        "POSSIBLE DUST" -> "Power output below baseline — dust accumulation likely."
        "RAIN DETECTED" -> "Rain detected. Cleaning is temporarily blocked."
        "LOW SUNLIGHT" -> "Insufficient sunlight for accurate performance reading."
        else -> "Device is offline. Check Wi-Fi and cloud connection."
    }

    val sunlightStatus = sensorData.sunlightLevel.ifBlank { if (sensorData.sunDetected) "DETECTED" else "LOW" }
    val rainStatus = if (sensorData.rainDetected) "RAIN" else "NO RAIN"
    val rainColor = if (sensorData.rainDetected) DangerRed else PrimaryGreen
    val ldrDescription = "LDR1: ${sensorData.ldr1}\nLDR2: ${sensorData.ldr2}"
    val rainDescription = if (sensorData.rainDetected) "Cleaning blocked" else "Safe for cleaning"

    val canStart = isConnected && !isCleaning && !sensorData.rainDetected
    val canStop = isConnected && isCleaning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Top Header ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DustZero",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) PrimaryGreen else DangerRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Connected · ESP32-S3-001" else "Device Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        DangerRed
                )
            }
        }

        // ── Hero Status Banner ───────────────────────────────────────────────
        StatusCard(
            title = "PANEL STATUS",
            status = panelStatus,
            icon = heroIcon,
            color = heroColor,
            description = heroDescription
        )

        // ── Key Metrics — Row 1 ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Solar Power",
                value = "%.2f".format(sensorData.solarPower),
                unit = "W",
                icon = Icons.Rounded.WbSunny,
                iconTint = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Voltage",
                value = "%.2f".format(sensorData.solarVoltage),
                unit = "V",
                icon = Icons.Rounded.ElectricBolt,
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Key Metrics — Row 2 ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Current",
                value = "%.1f".format(sensorData.solarCurrent),
                unit = "mA",
                icon = Icons.Rounded.BatteryChargingFull,
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Temperature",
                value = "%.1f".format(sensorData.temperature),
                unit = "°C",
                icon = Icons.Rounded.Thermostat,
                iconTint = DangerRed,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Environment — Sunlight & Rain (equal-height cards) ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // <-- makes both cards the same height
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusCard(
                title = "Sunlight",
                status = sunlightStatus,
                icon = Icons.Rounded.WbSunny,
                color = WarningAmber,
                description = ldrDescription,
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                title = "Rain Sensor",
                status = rainStatus,
                icon = Icons.Rounded.CloudQueue,
                color = rainColor,
                description = rainDescription,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Quick Actions ────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // START — filled green, disabled when cleaning/offline
                    Button(
                        onClick = { viewModel.startCleaning() },
                        enabled = canStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White,
                            disabledContainerColor = PrimaryGreen.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("START", fontWeight = FontWeight.Bold)
                    }
                    // STOP — filled red when active, visually muted when inactive
                    Button(
                        onClick = { viewModel.stopCleaning() },
                        enabled = canStop,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White,
                            disabledContainerColor = DangerRed.copy(alpha = 0.25f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("STOP", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isCleaning)
                        "Status: ${sensorData.cleaningState}"
                    else
                        "System Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
