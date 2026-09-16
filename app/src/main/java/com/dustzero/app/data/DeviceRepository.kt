package com.dustzero.app.data

import com.dustzero.app.iot.SupabaseClientProvider
import com.dustzero.app.models.AppConstants
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── DTOs ────────────────────────────────────────────────────────────────────

/** Lightweight summary of a device row — only the columns needed for listing/claiming. */
@Serializable
data class DeviceSummary(
    @SerialName("device_id") val deviceId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_name") val deviceName: String? = null
)

// ─── Claim Result ─────────────────────────────────────────────────────────────

/** Typed result from [DeviceRepository.claimDevice]. */
sealed class ClaimResult {
    /** Device claimed and user_id set to the current user. */
    object Success : ClaimResult()

    /**
     * Device is already owned by the current user — treat as success/switch.
     * Matches web app "already owned" behavior: just switch to the device.
     */
    object AlreadyOwned : ClaimResult()

    /** No `devices` row found for this device_id. */
    object NotFound : ClaimResult()

    /**
     * Device exists but its user_id belongs to a different user — cannot claim.
     * Error message matches web app exactly.
     */
    data class OwnedByOther(val deviceId: String) : ClaimResult()

    /** Unexpected error (network, permission, etc.) */
    data class Error(val message: String) : ClaimResult()
}

// ─── Repository ───────────────────────────────────────────────────────────────

/**
 * Repository for Supabase `devices` table queries scoped to the authenticated user.
 *
 * RLS policies on the `devices` table ensure:
 *   - Authenticated users can SELECT rows where user_id = auth.uid() OR user_id IS NULL
 *   - Authenticated users can UPDATE rows where user_id = auth.uid() OR user_id IS NULL
 *
 * This means we can query for a specific device_id even if it is unclaimed (user_id IS NULL),
 * which is required for the claim flow.
 */
class DeviceRepository {

    private val supabase = SupabaseClientProvider.client

    /**
     * Returns all devices owned by the currently authenticated user.
     * Relies on RLS: `WHERE user_id = auth.uid()`.
     */
    suspend fun getOwnedDevices(): List<DeviceSummary> {
        return try {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: return emptyList()

            supabase.from(AppConstants.TABLE_DEVICES)
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<DeviceSummary>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Claims the device with the given [deviceId] for the current user.
     *
     * Logic mirrors the web app's claim flow exactly:
     *   1. SELECT the device row
     *   2. Not found → [ClaimResult.NotFound]
     *   3. user_id IS NULL → UPDATE to set user_id = current user → [ClaimResult.Success]
     *   4. user_id == current user → [ClaimResult.AlreadyOwned]
     *   5. user_id != null && != current user → [ClaimResult.OwnedByOther]
     */
    suspend fun claimDevice(deviceId: String): ClaimResult {
        return try {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: return ClaimResult.Error("Not authenticated")

            // Step 1: Fetch the device row (RLS allows reading unclaimed devices too)
            val device = supabase.from(AppConstants.TABLE_DEVICES)
                .select {
                    filter { eq("device_id", deviceId) }
                }
                .decodeSingleOrNull<DeviceSummary>()

            when {
                // Step 2: Device does not exist at all
                device == null -> ClaimResult.NotFound

                // Step 4: Already owned by this user
                device.userId == currentUserId -> ClaimResult.AlreadyOwned

                // Step 5: Owned by a different user
                device.userId != null -> ClaimResult.OwnedByOther(deviceId)

                // Step 3: Unclaimed (user_id IS NULL) → claim it
                else -> {
                    supabase.from(AppConstants.TABLE_DEVICES)
                        .update({ set("user_id", currentUserId) }) {
                            filter { eq("device_id", deviceId) }
                        }
                    ClaimResult.Success
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ClaimResult.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /** Updates the location of a specific device. */
    suspend fun updateLocation(deviceId: String, lat: Double, lon: Double): Boolean {
        return try {
            supabase.from(AppConstants.TABLE_DEVICES)
                .update({ 
                    set("latitude", lat)
                    set("longitude", lon)
                }) {
                    filter { eq("device_id", deviceId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Updates the name of a specific device. */
    suspend fun updateName(deviceId: String, name: String): Boolean {
        return try {
            supabase.from(AppConstants.TABLE_DEVICES)
                .update({ 
                    set("device_name", name)
                }) {
                    filter { eq("device_id", deviceId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
