package com.dustzero.app.iot

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dustzero.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions

/**
 * Provides the singleton Supabase client.
 *
 * Session storage:
 *   The supabase-kt 3.x Auth plugin manages session persistence internally via its
 *   own settings abstraction. Since the exact settings API varies between minor versions,
 *   we store a copy of the session JSON in EncryptedSharedPreferences ourselves by
 *   saving/loading it around auth operations in AuthRepository.
 *
 *   For the Auth plugin itself, we rely on its default in-memory session storage in
 *   conjunction with AuthRepository.checkSession() (which calls refreshCurrentSession())
 *   on app startup to restore an existing session from the Supabase server using the
 *   refresh token stored in our EncryptedSharedPreferences.
 *
 * Usage:
 *   Call [initialize] once from DustZeroApp.onCreate() before any other Supabase access.
 */
object SupabaseClientProvider {

    val supabaseUrl = BuildConfig.SUPABASE_URL
    val supabaseKey = BuildConfig.SUPABASE_KEY

    val isConfigured = supabaseUrl.isNotBlank()
            && supabaseKey.isNotBlank()
            && supabaseUrl != "null"
            && supabaseUrl != "https://xyzcompany.supabase.co"

    private var _client: SupabaseClient? = null

    /**
     * The Supabase client. [initialize] must be called before first access.
     */
    val client: SupabaseClient
        get() = _client ?: createSupabaseClient(supabaseUrl, supabaseKey) {
            install(Postgrest)
            install(Realtime)
            install(Auth)
            install(Functions)
        }.also { _client = it }

    /**
     * Initializes the Supabase client. Call once from [com.dustzero.app.DustZeroApp.onCreate].
     * The [context] parameter is reserved for future use (e.g., plugging in a custom
     * session serializer once the supabase-kt settings API stabilises).
     */
    fun initialize(context: Context) {
        if (_client != null) return

        _client = createSupabaseClient(supabaseUrl, supabaseKey) {
            install(Postgrest)
            install(Realtime)
            install(Auth)
            install(Functions)
        }
    }
}
