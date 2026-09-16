package com.dustzero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dustzero.app.models.AppConstants
import com.dustzero.app.viewmodel.MainViewModel
import com.dustzero.app.ui.components.MetricCard
import com.dustzero.app.ui.components.StatusCard

@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit = {}) {
    val DangerRed = MaterialTheme.colorScheme.error
    val PrimaryGreen = MaterialTheme.colorScheme.primary
    val WarningAmber = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val panelStatus by viewModel.panelStatus.collectAsStateWithLifecycle()
    val isOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val hasFault by viewModel.hasFault.collectAsStateWithLifecycle()
    val activeDeviceId by viewModel.activeDeviceId.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val weatherLoading by viewModel.weatherLoading.collectAsStateWithLifecycle()
    val weatherError by viewModel.weatherError.collectAsStateWithLifecycle()

    // ─── No Device Selected — Empty State ─────────────────────────────────────
    if (activeDeviceId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.DevicesOther,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Device Connected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Go to Settings to add or claim your DustZero device to start monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go to Settings", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        return
    }

    var isStarting by remember { mutableStateOf(false) }
    var showOfflineError by remember { mutableStateOf(false) }

    val manualStatus by viewModel.manualCommandStatus.collectAsStateWithLifecycle()
    var showManualStartDialog by remember { mutableStateOf(false) }

    // Derive display values from live sensor data
    val isCleaning = AppConstants.isActivelyCleaning(sensorData.cleaningState)
    val canStart = isOnline && !isCleaning && !sensorData.rainDetected && !hasFault
    val canStop = isOnline && isCleaning

    LaunchedEffect(isCleaning, hasFault) {
        if (isCleaning || hasFault) {
            isStarting = false
        }
    }
    
    LaunchedEffect(sensorData.latitude, sensorData.longitude) {
        if (sensorData.latitude != null && sensorData.longitude != null) {
            viewModel.fetchWeather(sensorData.latitude!!, sensorData.longitude!!)
        }
    }

    LaunchedEffect(isStarting) {
        if (isStarting) {
            kotlinx.coroutines.delay(13000)
            if (isStarting) {
                isStarting = false
                showOfflineError = true // Reuse this flag to show error on button
            }
        }
    }


    val heroColor = when (panelStatus) {
        "OPTIMAL" -> PrimaryGreen
        "CLEANING" -> MaterialTheme.colorScheme.secondary
        "POSSIBLE DUST" -> WarningAmber
        "RAIN DETECTED" -> MaterialTheme.colorScheme.secondary
        "LOW SUNLIGHT" -> WarningAmber
        "FAULT" -> DangerRed
        else -> DangerRed // OFFLINE
    }
    val heroIcon = when (panelStatus) {
        "OPTIMAL" -> Icons.Rounded.CheckCircle
        "CLEANING" -> Icons.Rounded.CleaningServices
        "POSSIBLE DUST" -> Icons.Rounded.Warning
        "RAIN DETECTED" -> Icons.Rounded.CloudQueue
        "LOW SUNLIGHT" -> Icons.Rounded.WbCloudy
        "FAULT" -> Icons.Rounded.Error
        else -> Icons.Rounded.CloudOff
    }
    val heroDescription = when (panelStatus) {
        "OPTIMAL" -> "Strong sunlight detected. Panel performance is ideal."
        "CLEANING" -> "Cleaning cycle is currently active — ${AppConstants.cleaningStateLabel(sensorData.cleaningState)}."
        "POSSIBLE DUST" -> "Power output below baseline — dust accumulation likely."
        "RAIN DETECTED" -> "Rain detected. Cleaning is temporarily blocked."
        "LOW SUNLIGHT" -> "Insufficient sunlight for accurate performance reading."
        "FAULT" -> "Hardware fault reported. Manual inspection required."
        else -> "Device offline. Check Wi-Fi and cloud connection."
    }

    val sunlightStatus = sensorData.sunlightLevel.ifBlank { if (sensorData.sunDetected) "DETECTED" else "LOW" }
    val rainStatus = if (sensorData.rainDetected) "RAIN" else "NO RAIN"
    val rainColor = if (sensorData.rainDetected) DangerRed else PrimaryGreen
    val ldrDescription = "LDR1: ${sensorData.ldr1}\nLDR2: ${sensorData.ldr2}"
    val rainDescription = if (sensorData.rainDetected) "Cleaning blocked" else "Safe for cleaning"

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
                        .background(if (isOnline) PrimaryGreen else DangerRed)
                )
                if (isOnline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = if (isOnline) "Connected · ESP32-S3-001" else "Device Offline",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) MaterialTheme.colorScheme.onSurfaceVariant else DangerRed
                    )
                    if (isOnline) {
                        val timeSinceUpdate = (System.currentTimeMillis() - sensorData.updatedAtMs) / 1000
                        Text(
                            text = "Last updated ${timeSinceUpdate}s ago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Fault Banner (only shown when fault == true) ─────────────────────
        if (hasFault) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = "Fault",
                        tint = DangerRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SYSTEM FAULT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Text(
                            text = "Hardware fault detected — a sensor or display failed to initialize. Restart the device to clear this.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
        
        // ── Panel Efficiency ─────────────────────────────────────────────────
        // (Mock logic until baseline table integration is completed in ViewModel)
        val efficiencyPct = viewModel.panelEfficiency.collectAsStateWithLifecycle().value
        val baselineExists = viewModel.baselineExists.collectAsStateWithLifecycle().value
        
        Card(
            modifier = Modifier.fillMaxWidth().alpha(if (isOnline) 1f else 0.5f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Panel Efficiency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (!isOnline) {
                    Text("—", style = MaterialTheme.typography.bodyLarge)
                } else if (!baselineExists) {
                    Text("No baseline calibrated for current sunlight level. Please calibrate in Settings.", style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                } else {
                    val effColor = if (efficiencyPct > 85) PrimaryGreen else if (efficiencyPct > 60) WarningAmber else DangerRed
                    val effText = if (efficiencyPct > 85) "Optimal" else if (efficiencyPct > 60) "dust likely" else "dust critical"
                    Text(
                        text = "${efficiencyPct}% of clean baseline — $effText",
                        style = MaterialTheme.typography.bodyLarge,
                        color = effColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        // ── Weather & Cleaning Insights ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().alpha(if (isOnline) 1f else 0.5f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weather Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (sensorData.latitude == null || sensorData.longitude == null) {
                    Text("Location not set. Go to Settings to enable weather insights.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (weatherLoading && weatherData == null) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else if (weatherError != null && weatherData == null) {
                    Text("Weather unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (weatherData != null) {
                    val data = weatherData!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${data.current.temp}°C", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(data.current.description.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = data.cleaningInsight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (data.cleaningInsight.contains("block", ignoreCase = true)) WarningAmber else PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Key Metrics — Row 1 ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "POWER",
                value = if (isOnline) "%.4f".format(maxOf(0.0, sensorData.solarPower)) else "—",
                unit = "W",
                icon = Icons.Rounded.WbSunny,
                iconTint = WarningAmber,
                modifier = Modifier.weight(1f).alpha(if (isOnline) 1f else 0.5f)
            )
            MetricCard(
                title = "Voltage",
                value = if (isOnline) "%.2f".format(maxOf(0.0, sensorData.solarVoltage)) else "—",
                unit = "V",
                icon = Icons.Rounded.ElectricBolt,
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f).alpha(if (isOnline) 1f else 0.5f)
            )
        }

        // ── Key Metrics — Row 2 ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Current",
                value = if (isOnline) "%.3f".format(maxOf(0.0, sensorData.solarCurrent)) else "—",
                unit = "A",
                icon = Icons.Rounded.BatteryChargingFull,
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f).alpha(if (isOnline) 1f else 0.5f)
            )
            MetricCard(
                title = "Temperature",
                value = if (isOnline) "%.1f".format(sensorData.temperature) else "—",
                unit = "°C",
                icon = Icons.Rounded.Thermostat,
                iconTint = DangerRed,
                modifier = Modifier.weight(1f).alpha(if (isOnline) 1f else 0.5f)
            )
        }

        // ── Environment — Sunlight & Rain (equal-height via IntrinsicSize.Min) ─
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusCard(
                title = "Sunlight",
                status = if (isOnline) sunlightStatus else "—",
                icon = Icons.Rounded.WbSunny,
                color = WarningAmber,
                description = if (isOnline) ldrDescription else null,
                modifier = Modifier.weight(1f).alpha(if (isOnline) 1f else 0.5f)
            )
            StatusCard(
                title = "Rain Sensor",
                status = if (isOnline) rainStatus else "—",
                icon = Icons.Rounded.CloudQueue,
                color = rainColor,
                description = if (isOnline) rainDescription else null,
                modifier = Modifier.weight(1f).alpha(if (isOnline) 1f else 0.5f)
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
                    Button(
                        onClick = { 
                            if (!canStart) {
                                showOfflineError = true
                            } else {
                                showManualStartDialog = true
                            }
                        },
                        enabled = true,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!canStart) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondary,
                            contentColor = if (!canStart) Color.White.copy(alpha = 0.5f) else Color.White,
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        when (manualStatus) {
                            com.dustzero.app.iot.CommandState.SENDING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            com.dustzero.app.iot.CommandState.SENT -> Text("WAITING...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            else -> Text(if (showOfflineError && !canStart) "UNAVAILABLE" else "START MANUAL CLEANING", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    Button(
                        onClick = { viewModel.stopCleaning() },
                        enabled = canStop,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White,
                            disabledContainerColor = DangerRed.copy(alpha = 0.25f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("EMERGENCY STOP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isCleaning)
                        AppConstants.cleaningStateLabel(sensorData.cleaningState)
                    else if (manualStatus == com.dustzero.app.iot.CommandState.FAILED)
                        "Failed to start manual cleaning."
                    else
                        "Armed — waiting for conditions",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (manualStatus == com.dustzero.app.iot.CommandState.FAILED) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    
    if (showManualStartDialog) {
        AlertDialog(
            onDismissRequest = { showManualStartDialog = false },
            title = { Text("Start Manual Cleaning?") },
            text = { Text("This will start the solar panel cleaning cycle immediately. Make sure the cleaning mechanism is clear and safe to operate.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startManualCleaning()
                        showManualStartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualStartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
