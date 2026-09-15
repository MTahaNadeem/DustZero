package com.dustzero.app.iot

import com.dustzero.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    val supabaseUrl = BuildConfig.SUPABASE_URL
    val supabaseKey = BuildConfig.SUPABASE_KEY

    val isConfigured = supabaseUrl.isNotBlank()
            && supabaseKey.isNotBlank()
            && supabaseUrl != "null"
            && supabaseUrl != "https://xyzcompany.supabase.co"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth)
        }
    }
}
