package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Divider()
        
        Text(text = "App Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Demo Mode", style = MaterialTheme.typography.bodyLarge)
                Text("Simulate hardware data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = demoModeEnabled,
                onCheckedChange = { viewModel.setDemoMode(it) }
            )
        }
        
        if (demoModeEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Demo Scenarios", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            val scenarios = listOf("NORMAL", "DUST", "CLOUDY", "RAIN", "OFFLINE")
            scenarios.forEach { scenario ->
                OutlinedButton(
                    onClick = { viewModel.setDemoScenario(scenario) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate $scenario")
                }
            }
        }
        
        Divider()
        Text(text = "Device Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        
        SettingRow(label = "Device Name", value = "SolarClean Controller")
        SettingRow(label = "Device ID", value = "solarclean-001")
        SettingRow(label = "Hardware", value = "ESP32-S3")
        SettingRow(label = "Firmware", value = "1.0.0")

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
