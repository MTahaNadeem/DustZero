package com.dustzero.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dustzero.app.data.DevicePreferences
import com.dustzero.app.iot.DeviceDTO
import com.dustzero.app.iot.SupabaseClientProvider
import com.dustzero.app.models.AppConstants
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(DustZeroWidget::class.java)
        
        if (glanceIds.isEmpty()) return Result.success()

        val supabase = SupabaseClientProvider.client
        val currentUser = try { supabase.auth.currentUserOrNull() } catch (e: Exception) { null }
        
        val devicePrefs = DevicePreferences(context)
        val activeDeviceId = devicePrefs.activeDeviceId.value

        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs ->
                if (currentUser == null) {
                    prefs[DustZeroWidgetState.isLoggedIn] = false
                    prefs[DustZeroWidgetState.hasDevice] = false
                } else if (activeDeviceId == null) {
                    prefs[DustZeroWidgetState.isLoggedIn] = true
                    prefs[DustZeroWidgetState.hasDevice] = false
                } else {
                    prefs[DustZeroWidgetState.isLoggedIn] = true
                    prefs[DustZeroWidgetState.hasDevice] = true
                    
                    try {
                        val dto = supabase.from(AppConstants.TABLE_DEVICES)
                            .select { filter { eq("device_id", activeDeviceId) } }
                            .decodeSingleOrNull<DeviceDTO>()
                            
                        if (dto != null) {
                            prefs[DustZeroWidgetState.deviceId] = dto.deviceId
                            if (dto.deviceName != null) {
                                prefs[DustZeroWidgetState.deviceName] = dto.deviceName
                            }
                            
                            val updatedAtMs = parseIso8601ToMs(dto.updatedAt)
                            val heartbeatFresh = updatedAtMs > 0L && 
                                (System.currentTimeMillis() - updatedAtMs) < AppConstants.HEARTBEAT_TIMEOUT_MS
                            val isOnline = dto.connected && heartbeatFresh
                            
                            prefs[DustZeroWidgetState.isOnline] = isOnline
                            prefs[DustZeroWidgetState.power] = dto.solarPower
                            prefs[DustZeroWidgetState.voltage] = dto.solarVoltage
                            prefs[DustZeroWidgetState.temperature] = dto.temperature
                            prefs[DustZeroWidgetState.cleaningState] = dto.cleaningState
                            prefs[DustZeroWidgetState.fault] = dto.fault
                            prefs[DustZeroWidgetState.lastUpdatedMs] = System.currentTimeMillis()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // On network failure, we leave the last known data alone and just update lastUpdatedMs 
                        // if we want to show when we last checked, but it's better to keep the last successful 
                        // fetch time so the user knows the data is stale. So we don't update lastUpdatedMs here.
                    }
                }
            }
            DustZeroWidget().update(context, glanceId)
        }

        return Result.success()
    }
    
    private fun parseIso8601ToMs(iso: String?): Long {
        if (iso == null) return 0L
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    return sdf.parse(iso)?.time ?: continue
                } catch (_: Exception) { continue }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }
}
