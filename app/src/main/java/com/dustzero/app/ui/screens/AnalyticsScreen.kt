package com.dustzero.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dustzero.app.viewmodel.MainViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entriesOf
import com.patrykandpatrick.vico.core.entry.entryModelOf

import com.dustzero.app.iot.DeviceHistoryDTO
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val scrollState = rememberScrollState()
    
    var selectedRange by remember { mutableIntStateOf(0) }
    val ranges = listOf("24H", "7D", "30D")
    
    var history by remember { mutableStateOf<List<DeviceHistoryDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedRange) {
        isLoading = true
        val hours = when (selectedRange) {
            0 -> 24
            1 -> 24 * 7
            else -> 24 * 30
        }
        history = viewModel.getDeviceHistory(hours)
        isLoading = false
    }

    val powerChartEntryModel = remember(history) {
        if (history.isEmpty()) return@remember entryModelOf(0f)
        val entries = history.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = dto.solarPower.toFloat())
        }
        entryModelOf(entries)
    }
    
    // Panel Temperature vs. Voltage (Series 1: Temp, Series 2: Voltage * 40 for scale)
    val dualChartEntryModel = remember(history) {
        if (history.isEmpty()) return@remember entryModelOf(0f)
        val tempEntries = history.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = dto.temperature.toFloat())
        }
        val voltageEntries = history.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = (dto.solarVoltage * 40).toFloat())
        }
        entryModelOf(tempEntries, voltageEntries)
    }

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
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Historical performance and sensor data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ranges.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedRange,
                    onClick = { selectedRange = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size)
                ) {
                    Text(label)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        "No data yet for this period — check back once your device has been running a while.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "SOLAR POWER OUTPUT (W)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Chart(
                        chart = lineChart(),
                        model = powerChartEntryModel,
                        startAxis = rememberStartAxis(guideline = null),
                        bottomAxis = rememberBottomAxis(guideline = null, valueFormatter = { value, _ -> 
                            val idx = value.toInt()
                            if (idx >= 0 && idx < history.size) {
                                // Simplified time label, e.g. "12:00" or just "" to avoid clutter
                                "" 
                            } else ""
                        }),
                        modifier = Modifier.height(220.dp)
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PANEL TEMPERATURE (°C) VS. VOLTAGE (V x 40)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Chart(
                        chart = lineChart(),
                        model = dualChartEntryModel,
                        startAxis = rememberStartAxis(guideline = null),
                        bottomAxis = rememberBottomAxis(guideline = null),
                        modifier = Modifier.height(220.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
