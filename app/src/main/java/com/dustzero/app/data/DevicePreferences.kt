package com.dustzero.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's currently selected (active) device_id across app restarts.
 *
 * Backed by EncryptedSharedPreferences (AES256-GCM) — same master key pattern
 * as AuthRepository, so the device selection is stored securely alongside auth tokens.
 *
 * Usage:
 *   - Read  : devicePreferences.activeDeviceId (StateFlow)
 *   - Write : devicePreferences.setActiveDevice("dustzero-001")
 *   - Clear : devicePreferences.clearActiveDevice()   // call on sign-out
 */
class DevicePreferences(context: Context) {

    companion object {
        private const val PREFS_FILE = "device_prefs"
        private const val KEY_ACTIVE_DEVICE_ID = "active_device_id"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _activeDeviceId = MutableStateFlow<String?>(
        sharedPreferences.getString(KEY_ACTIVE_DEVICE_ID, null)
    )

    /** The currently active device_id. Null if no device has been claimed/selected yet. */
    val activeDeviceId: StateFlow<String?> = _activeDeviceId.asStateFlow()

    /** Sets the active device and persists the selection. */
    fun setActiveDevice(deviceId: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_DEVICE_ID, deviceId).apply()
        _activeDeviceId.value = deviceId
    }

    /** Clears the active device (called on sign-out so the next user starts fresh). */
    fun clearActiveDevice() {
        sharedPreferences.edit().remove(KEY_ACTIVE_DEVICE_ID).apply()
        _activeDeviceId.value = null
    }
}
