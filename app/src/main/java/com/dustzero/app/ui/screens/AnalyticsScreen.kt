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

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val scrollState = rememberScrollState()
    
    var selectedRange by remember { mutableIntStateOf(0) }
    val ranges = listOf("24H", "7D", "30D")

    val powerChartEntryModel = remember(selectedRange) { 
        when (selectedRange) {
            1 -> entryModelOf(0.12f, 0.11f, 0.11f, 0.09f, 0.07f, 0.06f, 0.11f)
            2 -> entryModelOf(0.12f, 0.10f, 0.11f, 0.08f, 0.09f, 0.07f, 0.10f, 0.12f, 0.11f, 0.09f)
            else -> entryModelOf(0.12f, 0.11f, 0.13f, 0.09f, 0.07f, 0.06f, 0.11f, 0.12f)
        }
    }
    
    // Panel Temperature vs. Voltage (Series 1: Temp, Series 2: Voltage * 40 for scale)
    val dualChartEntryModel = remember(selectedRange) { 
        when (selectedRange) {
            1 -> entryModelOf(
                entriesOf(32.1f, 34.5f, 36.2f, 38.1f, 37.5f, 36.0f, 35.2f),
                entriesOf(0.85f*40, 0.86f*40, 0.82f*40, 0.80f*40, 0.81f*40, 0.84f*40, 0.85f*40)
            )
            2 -> entryModelOf(
                entriesOf(32.1f, 33.5f, 36.2f, 35.1f, 37.5f, 36.0f, 34.2f, 33.1f, 36.5f, 38.1f),
                entriesOf(0.85f*40, 0.84f*40, 0.82f*40, 0.83f*40, 0.81f*40, 0.84f*40, 0.85f*40, 0.86f*40, 0.82f*40, 0.80f*40)
            )
            else -> entryModelOf(
                entriesOf(32.1f, 34.5f, 35.2f, 38.1f, 37.5f, 36.0f, 35.2f, 34.5f),
                entriesOf(0.85f*40, 0.82f*40, 0.84f*40, 0.80f*40, 0.81f*40, 0.83f*40, 0.84f*40, 0.85f*40)
            )
        }
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
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SOLAR POWER OUTPUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Chart(
                    chart = lineChart(),
                    model = powerChartEntryModel,
                    startAxis = rememberStartAxis(guideline = null),
                    bottomAxis = rememberBottomAxis(guideline = null),
                    modifier = Modifier.height(220.dp)
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "PANEL TEMPERATURE VS. VOLTAGE",
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
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
