package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
fun CleaningScreen(viewModel: MainViewModel) {
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cleaning System",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Status: ${sensorData.cleaningState}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Graphical representation of the panel and brush
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(300.dp)
                .background(Color(0xFF042F1C), RoundedCornerShape(12.dp))
        ) {
            // Brush
            val progressHeight = (300 * (sensorData.cleaningProgress / 100f)).dp
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .offset(y = progressHeight - if (sensorData.cleaningProgress == 100) 24.dp else 0.dp)
                    .background(Color(0xFF34A853), RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Progress: ${sensorData.cleaningProgress}%", style = MaterialTheme.typography.titleMedium)
        Text(text = "Steps: ${sensorData.cleaningSteps}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.weight(1f))

        if (sensorData.rainDetected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Automatic cleaning blocked because rain is detected.",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Make sure the cleaning mechanism is clear before starting.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.startCleaning() },
                enabled = sensorData.connected && (sensorData.cleaningState == "IDLE" || sensorData.cleaningState == "READY") && !sensorData.rainDetected,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("START CLEANING")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { viewModel.stopCleaning() },
                enabled = sensorData.connected && sensorData.cleaningState != "IDLE" && sensorData.cleaningState != "STOPPED" && sensorData.cleaningState != "READY",
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("STOP CLEANING")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.homeMotor() },
            enabled = sensorData.connected,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("HOME MOTOR")
        }
    }
}
