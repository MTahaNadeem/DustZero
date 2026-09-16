package com.dustzero.app.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DustZeroWidgetState {
    val isLoggedIn = booleanPreferencesKey("is_logged_in")
    val hasDevice = booleanPreferencesKey("has_device")
    
    val deviceId = stringPreferencesKey("device_id")
    val deviceName = stringPreferencesKey("device_name")
    val isOnline = booleanPreferencesKey("is_online")
    
    val power = doublePreferencesKey("power")
    val voltage = doublePreferencesKey("voltage")
    val temperature = doublePreferencesKey("temperature")
    val cleaningState = stringPreferencesKey("cleaning_state")
    val fault = booleanPreferencesKey("fault")
    
    val lastUpdatedMs = longPreferencesKey("last_updated_ms")
}
