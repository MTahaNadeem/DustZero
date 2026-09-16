package com.dustzero.app.widget

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.color.ColorProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dustzero.app.MainActivity
import com.dustzero.app.models.AppConstants

class DustZeroWidget : GlanceAppWidget() {
    companion object {
        private val SMALL_SIZE = DpSize(100.dp, 100.dp)
        private val WIDE_SIZE = DpSize(250.dp, 100.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val isLoggedIn = currentState(key = DustZeroWidgetState.isLoggedIn) ?: false
                val hasDevice = currentState(key = DustZeroWidgetState.hasDevice) ?: false

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(12.dp)
                ) {
                    if (!isLoggedIn) {
                        EmptyState("Open DustZero to sign in.")
                    } else if (!hasDevice) {
                        EmptyState("Open DustZero to add a device.")
                    } else {
                        if (size.width >= WIDE_SIZE.width) {
                            WideWidgetContent()
                        } else {
                            SmallWidgetContent()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState(message: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp
                )
            )
        }
    }

    @Composable
    private fun SmallWidgetContent() {
        val deviceName = currentState(key = DustZeroWidgetState.deviceName) ?: currentState(key = DustZeroWidgetState.deviceId) ?: "Unknown Device"
        val isOnline = currentState(key = DustZeroWidgetState.isOnline) ?: false
        val power = currentState(key = DustZeroWidgetState.power)
        val fault = currentState(key = DustZeroWidgetState.fault) ?: false
        val lastUpdatedMs = currentState(key = DustZeroWidgetState.lastUpdatedMs) ?: 0L

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(ColorProvider(day = statusColor, night = statusColor)) // Material Green/Red
                ) {}
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = deviceName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                if (fault) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(text = "⚠️", style = TextStyle(fontSize = 12.sp))
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            if (isOnline && power != null) {
                Text(
                    text = String.format(java.util.Locale.US, "%.4f W", power),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                Text(
                    text = "—",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            val timeString = if (lastUpdatedMs > 0) {
                DateUtils.getRelativeTimeSpanString(lastUpdatedMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
            } else {
                "Waiting for data..."
            }

            Text(
                text = timeString,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }

    @Composable
    private fun WideWidgetContent() {
        val deviceName = currentState(key = DustZeroWidgetState.deviceName) ?: currentState(key = DustZeroWidgetState.deviceId) ?: "Unknown Device"
        val isOnline = currentState(key = DustZeroWidgetState.isOnline) ?: false
        val power = currentState(key = DustZeroWidgetState.power)
        val voltage = currentState(key = DustZeroWidgetState.voltage)
        val temperature = currentState(key = DustZeroWidgetState.temperature)
        val cleaningStateRaw = currentState(key = DustZeroWidgetState.cleaningState) ?: AppConstants.STATE_IDLE
        val fault = currentState(key = DustZeroWidgetState.fault) ?: false
        val lastUpdatedMs = currentState(key = DustZeroWidgetState.lastUpdatedMs) ?: 0L

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(ColorProvider(day = statusColor, night = statusColor))
                ) {}
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = deviceName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                if (fault) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "FAULT",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFFF44336), night = Color(0xFFF44336)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                val timeString = if (lastUpdatedMs > 0) {
                    DateUtils.getRelativeTimeSpanString(lastUpdatedMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                } else {
                    "Waiting..."
                }
                Text(
                    text = timeString,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                // Power
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Power",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                    )
                    Text(
                        text = if (isOnline && power != null) String.format(java.util.Locale.US, "%.4f W", power) else "—",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }

                // Voltage
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Voltage",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                    )
                    Text(
                        text = if (isOnline && voltage != null) String.format(java.util.Locale.US, "%.2f V", voltage) else "—",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }

                // Temperature
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Temp",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                    )
                    Text(
                        text = if (isOnline && temperature != null) String.format(java.util.Locale.US, "%.1f °C", temperature) else "—",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
                
                // Status
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Status",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                    )
                    Text(
                        text = if (isOnline) AppConstants.cleaningStateLabel(cleaningStateRaw) else "Offline",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
