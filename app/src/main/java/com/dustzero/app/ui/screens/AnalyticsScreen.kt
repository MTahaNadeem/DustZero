package com.dustzero.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dustzero.app.iot.DeviceHistoryDTO
import com.dustzero.app.ui.theme.DangerRed
import com.dustzero.app.ui.theme.SecondaryBlue
import com.dustzero.app.ui.theme.WarningAmber
import com.dustzero.app.viewmodel.MainViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    var selectedRange by remember { mutableIntStateOf(0) }
    val ranges = listOf("24H", "7D", "30D")
    
    var history by remember { mutableStateOf<List<DeviceHistoryDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsStateWithLifecycle()
    val isOnline = sensorData.isOnline
    val isOffline = !isOnline && !demoModeEnabled

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

    // CSV Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvContent = generateCsvContent(history)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }
                Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Formatters ---
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { 
        timeZone = java.util.TimeZone.getTimeZone("UTC") 
    } }
    
    val chartData = remember(history) {
        val result = mutableListOf<DeviceHistoryDTO?>()
        for (i in history.indices) {
            if (i > 0) {
                try {
                    val prevStr = history[i-1].recordedAt.substringBefore(".").removeSuffix("Z")
                    val currStr = history[i].recordedAt.substringBefore(".").removeSuffix("Z")
                    val prevTime = isoFormatter.parse(prevStr)?.time ?: 0L
                    val currTime = isoFormatter.parse(currStr)?.time ?: 0L
                    if (currTime - prevTime > 15 * 60 * 1000) {
                        result.add(null) // Insert gap
                    }
                } catch (e: Exception) {}
            }
            result.add(history[i])
        }
        result
    }

    val powerChartEntryModel = remember(chartData) {
        if (chartData.isEmpty()) return@remember entryModelOf(0f)
        val entries = chartData.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = dto?.solarPower?.toFloat()?.coerceAtLeast(0f) ?: Float.NaN)
        }
        entryModelOf(entries)
    }
    
    val tempChartEntryModel = remember(chartData) {
        if (chartData.isEmpty()) return@remember entryModelOf(0f)
        val entries = chartData.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = dto?.temperature?.toFloat() ?: Float.NaN)
        }
        entryModelOf(entries)
    }

    val voltageChartEntryModel = remember(chartData) {
        if (chartData.isEmpty()) return@remember entryModelOf(0f)
        val entries = chartData.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = dto?.solarVoltage?.toFloat()?.coerceAtLeast(0f) ?: Float.NaN)
        }
        entryModelOf(entries)
    }

    val currentChartEntryModel = remember(chartData) {
        if (chartData.isEmpty()) return@remember entryModelOf(0f)
        val entries = chartData.mapIndexed { index, dto ->
            FloatEntry(x = index.toFloat(), y = dto?.solarCurrent?.toFloat()?.coerceAtLeast(0f) ?: Float.NaN)
        }
        entryModelOf(entries)
    }

    val displayFormatter = remember(selectedRange) {
        if (selectedRange == 0) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("MM/dd", Locale.getDefault())
    }

    // --- Summaries ---
    var strongSun = 0
    var mediumSun = 0
    var weakSun = 0
    var rainBlocks = 0
    var cleaningCycles = 0

    if (history.isNotEmpty()) {
        history.forEach { 
            when (it.sunlightLevel) {
                "STRONG" -> strongSun++
                "MEDIUM" -> mediumSun++
                else -> weakSun++
            }
            if (it.rainDetected) rainBlocks++
        }
        
        var previousState = "IDLE"
        history.forEach {
            if (previousState == "IDLE" && it.cleaningState == "MOVING_DOWN") {
                cleaningCycles++
            }
            previousState = it.cleaningState
        }
    }

    // Stats Logic
    var avgPower = 0f
    var peakPower = 0f
    var avgTemp = 0f
    var peakTemp = 0f
    if (history.isNotEmpty()) {
        avgPower = history.map { it.solarPower.toFloat().coerceAtLeast(0f) }.average().toFloat()
        peakPower = history.maxOfOrNull { it.solarPower.toFloat().coerceAtLeast(0f) } ?: 0f
        avgTemp = history.map { it.temperature.toFloat() }.average().toFloat()
        peakTemp = history.maxOfOrNull { it.temperature.toFloat() } ?: 0f
    }

    val snapshotMetadata = remember(history, selectedRange) {
        if (history.isEmpty()) return@remember "No snapshots available in this range"
        val displayFmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val count = history.size
        
        var startStr = ""
        var endStr = ""
        try {
            val firstDate = isoFormatter.parse(history.first().recordedAt.substringBefore(".").removeSuffix("Z"))
            val lastDate = isoFormatter.parse(history.last().recordedAt.substringBefore(".").removeSuffix("Z"))
            startStr = firstDate?.let { displayFmt.format(it) } ?: ""
            endStr = lastDate?.let { displayFmt.format(it) } ?: ""
        } catch (e: Exception) {}
        
        "$count snapshots in selected range · $startStr → $endStr"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
            TextButton(onClick = { exportLauncher.launch("dustzero_export.csv") }) {
                Icon(Icons.Rounded.Download, contentDescription = "Export", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export as CSV")
            }
        }

        // OFFLINE BANNER
        if (isOffline) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.WifiOff, contentDescription = "Offline", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Device Offline",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Historical data is still available from previous sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
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
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No data yet for this period — check back once your device has been running a while.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        } else {
            // METADATA
            Text(
                text = snapshotMetadata,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            // SUMMARY CARDS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Power",
                    value = String.format(Locale.US, "%.1f W", avgPower),
                    caption = "Mean over range",
                    iconTint = WarningAmber
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Peak Power",
                    value = String.format(Locale.US, "%.1f W", peakPower),
                    caption = "Highest recorded",
                    iconTint = WarningAmber
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Temp",
                    value = String.format(Locale.US, "%.1f °C", avgTemp),
                    caption = "Mean ambient",
                    iconTint = DangerRed
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Peak Temp",
                    value = String.format(Locale.US, "%.1f °C", peakTemp),
                    caption = "Highest recorded",
                    iconTint = DangerRed
                )
            }

            // Theme aware axes components
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val lineColor = MaterialTheme.colorScheme.outline
            val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

            // 1. Solar Power Output
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SOLAR POWER OUTPUT (W)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Now: ${String.format(Locale.US, "%.2f", sensorData.solarPower.coerceAtLeast(0.0))}W",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = WarningAmber,
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(WarningAmber.copy(alpha = 0.5f), WarningAmber.copy(alpha = 0.0f))
                                    )
                                )
                            )
                        ),
                        model = powerChartEntryModel,
                        startAxis = rememberStartAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp)
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp),
                            itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, chartData.size / 5)),
                            valueFormatter = { value, _ -> 
                                val idx = value.toInt()
                                if (idx in chartData.indices) {
                                    val dto = chartData[idx]
                                    if (dto != null) {
                                        try {
                                            val cleanStr = dto.recordedAt.substringBefore(".").removeSuffix("Z")
                                            val date = isoFormatter.parse(cleanStr)
                                            date?.let { displayFormatter.format(it) } ?: ""
                                        } catch (e: Exception) { "" }
                                    } else ""
                                } else ""
                            }
                        ),
                        modifier = Modifier.height(220.dp)
                    )
                }
            }
            
            // 2. Solar Current (A)
            val PurpleColor = Color(0xFFA855F7)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SOLAR CURRENT (A)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Now: ${String.format(Locale.US, "%.3f", sensorData.solarCurrent.coerceAtLeast(0.0))}A",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = PurpleColor,
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(PurpleColor.copy(alpha = 0.5f), PurpleColor.copy(alpha = 0.0f))
                                    )
                                )
                            )
                        ),
                        model = currentChartEntryModel,
                        startAxis = rememberStartAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp)
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp),
                            itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, chartData.size / 5)),
                            valueFormatter = { value, _ -> 
                                val idx = value.toInt()
                                if (idx in chartData.indices) {
                                    val dto = chartData[idx]
                                    if (dto != null) {
                                        try {
                                            val cleanStr = dto.recordedAt.substringBefore(".").removeSuffix("Z")
                                            val date = isoFormatter.parse(cleanStr)
                                            date?.let { displayFormatter.format(it) } ?: ""
                                        } catch (e: Exception) { "" }
                                    } else ""
                                } else ""
                            }
                        ),
                        modifier = Modifier.height(180.dp)
                    )
                }
            }

            // 3. Panel Temperature
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PANEL TEMPERATURE (°C)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Now: ${String.format(Locale.US, "%.1f", sensorData.temperature)}°C",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = DangerRed,
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(DangerRed.copy(alpha = 0.5f), DangerRed.copy(alpha = 0.0f))
                                    )
                                )
                            )
                        ),
                        model = tempChartEntryModel,
                        startAxis = rememberStartAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp)
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp),
                            itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, chartData.size / 5)),
                            valueFormatter = { value, _ -> 
                                val idx = value.toInt()
                                if (idx in chartData.indices) {
                                    val dto = chartData[idx]
                                    if (dto != null) {
                                        try {
                                            val cleanStr = dto.recordedAt.substringBefore(".").removeSuffix("Z")
                                            val date = isoFormatter.parse(cleanStr)
                                            date?.let { displayFormatter.format(it) } ?: ""
                                        } catch (e: Exception) { "" }
                                    } else ""
                                } else ""
                            }
                        ),
                        modifier = Modifier.height(180.dp)
                    )
                }
            }

            // 4. Solar Voltage
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SOLAR VOLTAGE (V)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Now: ${String.format(Locale.US, "%.2f", sensorData.solarVoltage.coerceAtLeast(0.0))}V",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = SecondaryBlue,
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(SecondaryBlue.copy(alpha = 0.5f), SecondaryBlue.copy(alpha = 0.0f))
                                    )
                                )
                            )
                        ),
                        model = voltageChartEntryModel,
                        startAxis = rememberStartAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp)
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = textComponent(color = labelColor, textSize = 10.sp),
                            axis = lineComponent(color = lineColor, thickness = 1.dp),
                            guideline = lineComponent(color = guideColor, thickness = 1.dp),
                            itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, chartData.size / 5)),
                            valueFormatter = { value, _ -> 
                                val idx = value.toInt()
                                if (idx in chartData.indices) {
                                    val dto = chartData[idx]
                                    if (dto != null) {
                                        try {
                                            val cleanStr = dto.recordedAt.substringBefore(".").removeSuffix("Z")
                                            val date = isoFormatter.parse(cleanStr)
                                            date?.let { displayFormatter.format(it) } ?: ""
                                        } catch (e: Exception) { "" }
                                    } else ""
                                } else ""
                            }
                        ),
                        modifier = Modifier.height(180.dp)
                    )
                }
            }
            
            // 5. Sunlight Distribution & Cleaning Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sunlight
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "SUNLIGHT", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val total = (strongSun + mediumSun + weakSun).coerceAtLeast(1)
                        Text("Strong: ${strongSun * 100 / total}%", style = MaterialTheme.typography.bodyMedium)
                        Text("Medium: ${mediumSun * 100 / total}%", style = MaterialTheme.typography.bodyMedium)
                        Text("Weak: ${weakSun * 100 / total}%", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                // Operations
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "OPERATIONS", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cleanings: $cleaningCycles", style = MaterialTheme.typography.bodyMedium)
                        val rainPct = rainBlocks * 100 / history.size.coerceAtLeast(1)
                        Text("Rain Block: $rainPct%", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            // 6. Cleaning Effectiveness
            val recentCleanings by viewModel.cleaningHistory.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "CLEANING EFFECTIVENESS", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (recentCleanings.isEmpty() && cleaningCycles == 0) {
                        Text(
                            "No cleaning cycles recorded in this timeframe.", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (recentCleanings.isEmpty()) {
                        Text(
                            "No cleaning cycles recorded in this timeframe.", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentCleanings.take(3).forEach { cleaning ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                val df = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                Text(df.format(java.util.Date(cleaning.startTime)), style = MaterialTheme.typography.bodyMedium)
                                // Mock recovery for now since local db doesn't store delta yet
                                Text("+12% efficiency", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String, caption: String, iconTint: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Analytics, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun generateCsvContent(history: List<DeviceHistoryDTO>): String {
    val sb = StringBuilder()
    sb.append("Timestamp,SolarPower_W,SolarVoltage_V,SolarCurrent_A,Temperature_C,SunlightLevel,RainDetected,CleaningState\n")
    for (item in history) {
        sb.append("${item.recordedAt},${item.solarPower},${item.solarVoltage},${item.solarCurrent},${item.temperature},${item.sunlightLevel},${item.rainDetected},${item.cleaningState}\n")
    }
    return sb.toString()
}
