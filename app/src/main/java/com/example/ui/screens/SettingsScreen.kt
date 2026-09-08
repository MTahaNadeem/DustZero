package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    // Mock settings states for UI
    var autoCleaning by remember { mutableStateOf(true) }
    var pushAlerts by remember { mutableStateOf(true) }
    var offlineAlerts by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Preferences and device configuration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(title = "DEVICE") {
            SettingRowInfo(icon = Icons.Rounded.DeveloperBoard, label = "Device Name", value = "DustZero Controller")
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInfo(icon = Icons.Rounded.QrCode, label = "Device ID", value = "dustzero-001")
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInfo(icon = Icons.Rounded.Memory, label = "Firmware Version", value = "v1.2.4")
        }

        SettingsSection(title = "CLEANING CONFIGURATION") {
            SettingRowSwitch(
                icon = Icons.Rounded.Autorenew,
                label = "Automatic Cleaning",
                description = "Trigger cleaning based on sensor data",
                checked = autoCleaning,
                onCheckedChange = { autoCleaning = it }
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInfo(icon = Icons.Rounded.Timer, label = "Cleaning Cooldown", value = "24 hours")
        }
        
        SettingsSection(title = "APPEARANCE") {
            val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(text = "Choose how DustZero looks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = currentTheme == com.example.data.ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(com.example.data.ThemeMode.LIGHT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Light")
                    }
                    SegmentedButton(
                        selected = currentTheme == com.example.data.ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(com.example.data.ThemeMode.DARK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Dark")
                    }
                    SegmentedButton(
                        selected = currentTheme == com.example.data.ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(com.example.data.ThemeMode.SYSTEM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("System")
                    }
                }
            }
        }
        
        SettingsSection(title = "NOTIFICATIONS") {
            SettingRowSwitch(
                icon = Icons.Rounded.NotificationsActive,
                label = "Cleaning Alerts",
                description = "Notify when a cleaning cycle starts",
                checked = pushAlerts,
                onCheckedChange = { pushAlerts = it }
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowSwitch(
                icon = Icons.Rounded.WifiOff,
                label = "Device Offline Alerts",
                description = "Notify if ESP32 disconnects",
                checked = offlineAlerts,
                onCheckedChange = { offlineAlerts = it }
            )
        }

        SettingsSection(title = "DEVELOPER & DEMO") {
            SettingRowSwitch(
                icon = Icons.Rounded.Science,
                label = "Demo Mode",
                description = "Simulate hardware data",
                checked = demoModeEnabled,
                onCheckedChange = { viewModel.setDemoMode(it) }
            )
            
            if (demoModeEnabled) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trigger Scenario", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    val scenarios = listOf("NORMAL", "DUST", "CLOUDY", "RAIN", "OFFLINE")
                    scenarios.forEach { scenario ->
                        OutlinedButton(
                            onClick = { viewModel.setDemoScenario(scenario) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simulate $scenario")
                        }
                    }
                }
            }
        }
        
        SettingsSection(title = "APP") {
            SettingRowInfo(icon = Icons.Rounded.Info, label = "About DustZero", value = "")
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInfo(icon = Icons.Rounded.SystemUpdate, label = "App Version", value = "1.0.0")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingRowInfo(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        if (value.isNotEmpty()) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingRowSwitch(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

