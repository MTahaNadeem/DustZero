package com.dustzero.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import com.dustzero.app.iot.SupabaseClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val context: Context) {
    private val supabase = SupabaseClientProvider.client

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    init {
        // Retrieve session if needed or let supabase handle it
        try {
            _currentUser.value = supabase.auth.currentUserOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            _currentUser.value = supabase.auth.currentUserOrNull()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun checkSession(): Boolean {
        return try {
            supabase.auth.refreshCurrentSession()
            _currentUser.value = supabase.auth.currentUserOrNull()
            _currentUser.value != null
        } catch (e: Exception) {
            e.printStackTrace()
            _currentUser.value = null
            false
        }
    }
}
