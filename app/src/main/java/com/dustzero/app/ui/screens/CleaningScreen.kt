package com.dustzero.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dustzero.app.ui.theme.EmeraldGreen
import com.dustzero.app.ui.theme.RedError
import com.dustzero.app.ui.theme.OrangeSunlight
import com.dustzero.app.viewmodel.MainViewModel

@Composable
fun CleaningScreen(viewModel: MainViewModel) {
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsStateWithLifecycle()
    
    var showStartDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val isCleaning = sensorData.cleaningState != "IDLE" && sensorData.cleaningState != "OFFLINE"
    val stateColor = if (isCleaning) MaterialTheme.colorScheme.secondary else EmeraldGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cleaning Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage and monitor the ANTIGRAVITY cleaning mechanism",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.CleaningServices,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isCleaning) "CLEANING IN PROGRESS" else "READY",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = stateColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cycle Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text("Cycle Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isCleaning) "Current Cycle: In Progress" else "Current Cycle: No active cleaning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last Cleaning: 2026-09-11 14:30",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Next Cleaning: Available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress UI
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Progress UI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                val progress = if (isCleaning) sensorData.cleaningProgress / 100f else 0f
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500))
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = stateColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isCleaning) sensorData.cleaningState else "IDLE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        val canStart = (sensorData.connected || demoModeEnabled) && !isCleaning && !sensorData.rainDetected
        val canStop = (sensorData.connected || demoModeEnabled) && isCleaning
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { showStartDialog = true },
                enabled = canStart,
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("START CLEANING", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { showStopDialog = true },
                enabled = canStop,
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RedError
                ),
                border = BorderStroke(2.dp, RedError),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("STOP CLEANING", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start Cleaning?") },
            text = { Text("Are you sure you want to start the cleaning cycle? The system will sweep the panel automatically.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startCleaning()
                        showStartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Start Cleaning")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Cleaning?") },
            text = { Text("This will immediately stop the cleaning mechanism. It may be left in an intermediate position.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopCleaning()
                        showStopDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
