package com.dustzero.app

import android.app.Application
import com.dustzero.app.iot.SupabaseClientProvider

/**
 * DustZero Application class.
 *
 * Registered in AndroidManifest.xml as the application-level class.
 * Initializes the Supabase client with encrypted session storage so that
 * auth tokens persist across app restarts without touching plain SharedPreferences.
 */
class DustZeroApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Supabase with EncryptedSharedPreferences-backed session storage.
        // Must be called before any SupabaseClientProvider.client access.
        SupabaseClientProvider.initialize(this)
    }
}
