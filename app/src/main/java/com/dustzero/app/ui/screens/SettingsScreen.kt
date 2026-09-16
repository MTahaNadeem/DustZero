package com.dustzero.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dustzero.app.data.ClaimResult
import com.dustzero.app.data.ThemeMode
import com.dustzero.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import com.dustzero.app.models.AppConstants
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }

    val demoModeEnabled by viewModel.demoModeEnabled.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    // Local UI state
    var autoCleaning by remember { mutableStateOf(true) }
    var pushAlerts by remember { mutableStateOf(true) }
    var offlineAlerts by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeDeviceId by viewModel.activeDeviceId.collectAsStateWithLifecycle()
    val ownedDevices by viewModel.ownedDevices.collectAsStateWithLifecycle()
    val deviceListLoading by viewModel.deviceListLoading.collectAsStateWithLifecycle()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()

    // ─── Device claim UI state ────────────────────────────────────────────────
    var claimDeviceInput by remember { mutableStateOf("") }
    var claimLoading by remember { mutableStateOf(false) }
    var claimResult by remember { mutableStateOf<String?>(null) }
    var claimIsError by remember { mutableStateOf(false) }

    // ─── Device dropdown state ────────────────────────────────────────────────
    var deviceDropdownExpanded by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> if (isGranted) { pushAlerts = true } }
    )

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions.entries.any { it.value }
            if (granted) {
                viewModel.fetchCurrentLocation(
                    onSuccess = { lat, lon ->
                        viewModel.updateDeviceLocation(lat, lon)
                        scope.launch { snackbarHostState.showSnackbar("Location detected") }
                    },
                    onError = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            } else {
                val activity = context as? android.app.Activity
                val shouldShowRationale = activity?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
                } ?: false

                if (!shouldShowRationale) {
                    // Permanently denied
                    showPermissionDeniedDialog = true
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Location permission is required to automatically detect your location.") }
                }
            }
        }
    )

    var cleaningCooldown by remember { mutableStateOf("30") }
    var cleaningDistance by remember { mutableStateOf("500") }
    var sunlightThreshold by remember { mutableStateOf("200") }
    var powerBaseline by remember { mutableStateOf("0.05") }

    // Load owned devices whenever screen appears
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.loadOwnedDevices()
        }
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text("Allow Location Access") },
            text = { Text("Allow DustZero to access your location?\n\nYour location is used to determine the weather near your solar panel. DustZero does not continuously track your location.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationaleDialog = false
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permission Disabled") },
            text = { Text("Location permission is disabled. Enable it from Android Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(AppConstants.APP_NAME, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(AppConstants.APP_SUBTITLE, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Version: $versionName", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("DustZero is an automated solar panel cleaning system designed to maintain peak efficiency. This companion app provides real-time monitoring and manual overrides for your hardware.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Developed by MTahaNadeem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
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

        // ─── ACCOUNT Section ─────────────────────────────────────────────────
        SettingsSection(title = "ACCOUNT") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Signed in as",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentUser?.email ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowAction(
                icon = Icons.AutoMirrored.Rounded.Logout,
                label = "Sign Out",
                onClick = { viewModel.signOut() }
            )
        }

        // ─── DEVICE CONNECTION Section ────────────────────────────────────────
        SettingsSection(title = "DEVICE CONNECTION") {

            // ── Select Device Dropdown ────────────────────────────────────────
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Rounded.DevicesOther,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Select Device",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (deviceListLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }

                if (ownedDevices.isEmpty() && !deviceListLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            "No devices found. Claim your first device below ↓",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (ownedDevices.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = deviceDropdownExpanded,
                        onExpandedChange = { deviceDropdownExpanded = !deviceDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = activeDeviceId ?: "Select a device",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = deviceDropdownExpanded,
                            onDismissRequest = { deviceDropdownExpanded = false }
                        ) {
                            ownedDevices.forEach { device ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.DeveloperBoard,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (device.deviceId == activeDeviceId)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(device.deviceName ?: device.deviceId)
                                            if (device.deviceName != null) {
                                                Text(" (${device.deviceId})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        deviceDropdownExpanded = false
                                        viewModel.selectDevice(device.deviceId)
                                        claimResult = null
                                    },
                                    trailingIcon = {
                                        if (device.deviceId == activeDeviceId) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // ── Live Device Info ──────────────────────────────────────────────
            if (activeDeviceId != null) {
                SettingRowInfo(
                    icon = Icons.Rounded.QrCode,
                    label = "Device ID",
                    value = sensorData.deviceId.ifEmpty { activeDeviceId ?: "—" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                SettingRowInfo(
                    icon = Icons.Rounded.DeveloperBoard,
                    label = "Device Name",
                    value = sensorData.deviceName ?: "—"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                SettingRowInfo(
                    icon = Icons.Rounded.Memory,
                    label = "Firmware Version",
                    value = "—"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }

            // ── Refresh Connection ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        if (!isRefreshing) {
                            isRefreshing = true
                            viewModel.refreshConnection { isOnline ->
                                isRefreshing = false
                                scope.launch {
                                    if (isOnline) {
                                        snackbarHostState.showSnackbar("Connection refreshed — Device Online")
                                    } else {
                                        snackbarHostState.showSnackbar("Device Offline — Unable to reach device")
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = activeDeviceId != null
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refreshing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh Connection", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // ── Add / Claim New Device ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Add / Claim New Device",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = claimDeviceInput,
                    onValueChange = {
                        claimDeviceInput = it
                        claimResult = null
                    },
                    label = { Text("Device ID") },
                    placeholder = { Text("e.g. dustzero-001") },
                    supportingText = {
                        Text(
                            "Enter the exact device ID configured in your ESP32 firmware to claim it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    isError = claimIsError && claimResult != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                // Claim result message
                if (claimResult != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (claimIsError)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = claimResult!!,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (claimIsError)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val deviceId = claimDeviceInput.trim()
                        if (deviceId.isBlank()) {
                            claimResult = "Please enter a device ID."
                            claimIsError = true
                            return@Button
                        }
                        claimLoading = true
                        claimResult = null
                        viewModel.claimDevice(deviceId) { result ->
                            claimLoading = false
                            when (result) {
                                is ClaimResult.Success -> {
                                    claimResult = "Device claimed and selected successfully!"
                                    claimIsError = false
                                    claimDeviceInput = ""
                                }
                                is ClaimResult.AlreadyOwned -> {
                                    claimResult = "Device already in your account — switched to it."
                                    claimIsError = false
                                    claimDeviceInput = ""
                                }
                                is ClaimResult.NotFound -> {
                                    claimResult = "No device found with this ID. Check it matches your ESP32 firmware's DEVICE_ID."
                                    claimIsError = true
                                }
                                is ClaimResult.OwnedByOther -> {
                                    claimResult = "Cannot claim device '${result.deviceId}'. It may be owned by another user."
                                    claimIsError = true
                                }
                                is ClaimResult.Error -> {
                                    claimResult = result.message
                                    claimIsError = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !claimLoading
                ) {
                    if (claimLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adding...")
                    } else {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add / Switch Device", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        
        // ─── LOCATION & WEATHER ──────────────────────────────────────────────
        SettingsSection(title = "LOCATION & WEATHER") {
            var nameInput by remember { mutableStateOf(sensorData.deviceName ?: "") }
            
            // Sync input with sensorData when it changes externally
            LaunchedEffect(sensorData.deviceName) {
                nameInput = sensorData.deviceName ?: ""
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Device Identity", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.updateDeviceName(nameInput.trim())
                            scope.launch { snackbarHostState.showSnackbar("Device name updated successfully") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = activeDeviceId != null
                ) {
                    Text("Save Name")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Device Location", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                
                val lat = sensorData.latitude
                val lon = sensorData.longitude
                val hasLocation = lat != null && lon != null
                
                val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

                Spacer(modifier = Modifier.height(8.dp))
                
                if (hasLocation) {
                    Text("Location saved", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Latitude: $lat", style = MaterialTheme.typography.bodyMedium)
                    Text("Longitude: $lon", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Location not set", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Set your device location to enable local weather information.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var showManualDialog by remember { mutableStateOf(false) }
                
                if (showManualDialog) {
                    var manualLat by remember { mutableStateOf(lat?.toString() ?: "") }
                    var manualLon by remember { mutableStateOf(lon?.toString() ?: "") }
                    var latError by remember { mutableStateOf(false) }
                    var lonError by remember { mutableStateOf(false) }
                    
                    AlertDialog(
                        onDismissRequest = { showManualDialog = false },
                        title = { Text("Enter Coordinates") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = manualLat,
                                    onValueChange = { 
                                        manualLat = it
                                        val v = it.toDoubleOrNull()
                                        latError = v == null || v < -90 || v > 90
                                    },
                                    label = { Text("Latitude") },
                                    isError = latError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    supportingText = { if (latError) Text("Must be between -90 and 90") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualLon,
                                    onValueChange = { 
                                        manualLon = it
                                        val v = it.toDoubleOrNull()
                                        lonError = v == null || v < -180 || v > 180
                                    },
                                    label = { Text("Longitude") },
                                    isError = lonError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    supportingText = { if (lonError) Text("Must be between -180 and 180") }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val latVal = manualLat.toDoubleOrNull()
                                    val lonVal = manualLon.toDoubleOrNull()
                                    if (latVal != null && lonVal != null && latVal in -90.0..90.0 && lonVal in -180.0..180.0) {
                                        viewModel.updateDeviceLocation(latVal, lonVal)
                                        scope.launch { snackbarHostState.showSnackbar("Location saved") }
                                        showManualDialog = false
                                    }
                                },
                                enabled = !latError && !lonError && manualLat.isNotBlank() && manualLon.isNotBlank()
                            ) { Text("Save Location") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showManualDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            val coarseLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (fineLoc == PackageManager.PERMISSION_GRANTED || coarseLoc == PackageManager.PERMISSION_GRANTED) {
                                viewModel.fetchCurrentLocation(
                                    onSuccess = { l, n ->
                                        viewModel.updateDeviceLocation(l, n)
                                        scope.launch { snackbarHostState.showSnackbar("Location detected") }
                                    },
                                    onError = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            } else {
                                showPermissionRationaleDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = activeDeviceId != null && !isFetchingLocation
                    ) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detecting...")
                        } else {
                            Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Current")
                        }
                    }
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = activeDeviceId != null
                    ) {
                        Text(if (hasLocation) "Update Manually" else "Enter Manually")
                    }
                }
            }
        }

        // ─── DEVICE CALIBRATION ───────────────────────────────────────────────
        SettingsSection(title = "DEVICE CALIBRATION") {
            SettingRowAction(
                icon = Icons.Rounded.Tune,
                label = "Calibrate Clean Panel Baseline",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Baseline calibrated for current sunlight level")
                    }
                }
            )
        }

        // ─── CLEANING CONFIGURATIONS ──────────────────────────────────────────
        SettingsSection(title = "CLEANING CONFIGURATIONS") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha=0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha=0.3f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("These settings require firmware v2.0+. Edits will not affect current hardware.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SettingRowSwitch(
                icon = Icons.Rounded.Autorenew,
                label = "Automatic Cleaning",
                description = "Trigger cleaning based on sensor data",
                checked = autoCleaning,
                onCheckedChange = { autoCleaning = it }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInput(
                icon = Icons.Rounded.Timer,
                label = "Cleaning Cooldown (mins)",
                value = cleaningCooldown,
                onValueChange = { cleaningCooldown = it }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInput(
                icon = Icons.Rounded.Straighten,
                label = "Cleaning Distance (steps)",
                value = cleaningDistance,
                onValueChange = { cleaningDistance = it }
            )
        }

        // ─── THRESHOLDS ───────────────────────────────────────────────────────
        SettingsSection(title = "THRESHOLDS") {
            SettingRowInput(
                icon = Icons.Rounded.WbSunny,
                label = "Sunlight Threshold",
                value = sunlightThreshold,
                onValueChange = { sunlightThreshold = it }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInput(
                icon = Icons.Rounded.ElectricBolt,
                label = "Power Baseline Threshold",
                value = powerBaseline,
                onValueChange = { powerBaseline = it }
            )
        }

        // ─── NOTIFICATIONS ────────────────────────────────────────────────────
        SettingsSection(title = "NOTIFICATIONS") {
            SettingRowSwitch(
                icon = Icons.Rounded.NotificationsActive,
                label = "Push Alerts",
                description = "Get notified of faults and cycle completion",
                checked = pushAlerts,
                onCheckedChange = { checked ->
                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@SettingRowSwitch
                        }
                    }
                    pushAlerts = checked
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowSwitch(
                icon = Icons.Rounded.WifiOff,
                label = "Offline Alerts",
                description = "Notify when device loses connection",
                checked = offlineAlerts,
                onCheckedChange = { offlineAlerts = it }
            )
        }

        // ─── APPEARANCE ───────────────────────────────────────────────────────
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
                        selected = currentTheme == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) { Text("Light") }
                    SegmentedButton(
                        selected = currentTheme == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) { Text("Dark") }
                    SegmentedButton(
                        selected = currentTheme == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) { Text("System") }
                }
            }
        }

        // ─── DEVELOPER & DEMO ─────────────────────────────────────────────────
        SettingsSection(title = "DEVELOPER & DEMO") {
            SettingRowSwitch(
                icon = Icons.Rounded.Science,
                label = "Demo Mode",
                description = "Simulate hardware data",
                checked = demoModeEnabled,
                onCheckedChange = { viewModel.setDemoMode(it) }
            )
            if (demoModeEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trigger Scenario", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    val scenarios = listOf("NORMAL", "DUST", "CLOUDY", "RAIN", "FAULT", "OFFLINE")
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

        // ─── APP ──────────────────────────────────────────────────────────────
        SettingsSection(title = "APP") {
            SettingRowAction(
                icon = Icons.Rounded.Info,
                label = "About DustZero",
                onClick = { showAboutDialog = true }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingRowInfo(icon = Icons.Rounded.SystemUpdate, label = "App Version", value = versionName)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
    }
}

// ─── Shared Settings Components ───────────────────────────────────────────────

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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
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

@Composable
fun SettingRowInput(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(100.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingRowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
