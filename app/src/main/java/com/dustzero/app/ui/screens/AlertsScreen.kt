package com.dustzero.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dustzero.app.ui.theme.DangerRed
import com.dustzero.app.ui.theme.PrimaryGreen
import com.dustzero.app.ui.theme.WarningAmber
import com.dustzero.app.viewmodel.MainViewModel

@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    // For demonstration purposes, we will use local state for the mock alerts to support "Mark all as read"
    var allRead by remember { mutableStateOf(false) }

    val mockAlerts = listOf(
        Triple(
            "WARNING",
            "LOW SOLAR OUTPUT - Solar output is lower than expected for current sunlight.",
            WarningAmber
        ),
        Triple(
            "INFO",
            "CLEANING COMPLETED - Cleaning cycle completed successfully.",
            MaterialTheme.colorScheme.secondary
        ),
        Triple(
            "SYSTEM",
            "RAIN DETECTED - Automatic cleaning is temporarily blocked.",
            DangerRed
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Alerts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Notifications, warnings, and system events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { allRead = true }) {
                Text(
                    text = "Mark all as read",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(mockAlerts.size) { index ->
                val alert = mockAlerts[index]
                MockAlertItem(
                    type = alert.first,
                    message = alert.second,
                    color = alert.third,
                    isRead = allRead
                )
            }
        }
    }
}

@Composable
fun MockAlertItem(type: String, message: String, color: Color, isRead: Boolean) {
    val icon = when (type) {
        "SYSTEM" -> Icons.Rounded.Error
        "WARNING" -> Icons.Rounded.Warning
        else -> Icons.Rounded.Info
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surface else color.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, if (isRead) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Just now",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, androidx.compose.foundation.shape.CircleShape)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
