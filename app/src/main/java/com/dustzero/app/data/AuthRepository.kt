package com.dustzero.app.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import com.dustzero.app.iot.SupabaseClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "DZ_Auth"

// ─── Typed Auth Result ────────────────────────────────────────────────────────

/**
 * Typed result from [AuthRepository.signIn].
 * Callers check this sealed class instead of a bare Boolean so that
 * UI can show specific messages for each failure case.
 */
sealed class AuthResult {
    /** Sign-in succeeded. User is now authenticated. */
    object Success : AuthResult()

    /**
     * Supabase rejected the sign-in because the email address has not been confirmed.
     * UI should show a "Resend confirmation email" option.
     */
    object EmailNotConfirmed : AuthResult()

    /** Wrong email or password. */
    object InvalidCredentials : AuthResult()

    /** Any other error (network, server, etc.) */
    data class Error(val message: String) : AuthResult()
}

// ─── Repository ───────────────────────────────────────────────────────────────

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
        // Only reads in-memory state — always null on cold start (expected)
        val savedToken = sharedPreferences.getString("refresh_token", null)
        Log.d(TAG, "[INIT] EncryptedSharedPrefs token present: ${savedToken != null}")
        try {
            _currentUser.value = supabase.auth.currentUserOrNull()
            Log.d(TAG, "[INIT] In-memory currentUser: ${_currentUser.value?.id}")
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

    /**
     * Signs in with email + password and returns a typed [AuthResult].
     *
     * Detects "email_not_confirmed" specifically so the UI can offer a
     * "Resend confirmation email" action (matching the web app's UX).
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            _currentUser.value = supabase.auth.currentUserOrNull()
            
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                sharedPreferences.edit().putString("refresh_token", session.refreshToken).apply()
                Log.d(TAG, "[SIGN_IN] Session saved. refresh_token length=${session.refreshToken.length}")
            } else {
                Log.e(TAG, "[SIGN_IN] Sign-in succeeded but currentSessionOrNull() returned null!")
            }
            
            AuthResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            val message = e.message?.lowercase() ?: ""
            when {
                // Supabase returns "Email not confirmed" for unverified accounts
                message.contains("email not confirmed") ||
                message.contains("email_not_confirmed") ->
                    AuthResult.EmailNotConfirmed

                // Invalid credentials
                message.contains("invalid login credentials") ||
                message.contains("invalid_credentials") ||
                message.contains("invalid email or password") ->
                    AuthResult.InvalidCredentials

                else -> AuthResult.Error(e.message ?: "Sign in failed")
            }
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
            sharedPreferences.edit().remove("refresh_token").apply()
            _currentUser.value = null
        } catch (e: Exception) {
            e.printStackTrace()
            // Force local sign out even if network fails
            sharedPreferences.edit().remove("refresh_token").apply()
            _currentUser.value = null
        }
    }

    suspend fun checkSession(): Boolean {
        return try {
            val refreshToken = sharedPreferences.getString("refresh_token", null)
            Log.d(TAG, "[CHECK_SESSION] Stored refresh_token present: ${refreshToken != null}")
            if (refreshToken != null) {
                Log.d(TAG, "[CHECK_SESSION] Calling refreshSession(token)...")
                try {
                    // Try to get the session returned by refreshSession if it returns one
                    val session = supabase.auth.refreshSession(refreshToken)
                    
                    // If supabase-kt doesn't immediately update currentSessionOrNull(), we can try importing it
                    if (session != null) {
                        try {
                            // If session is a string (some versions return string), or if importAuthToken takes string token
                            supabase.auth.importAuthToken(session.toString())
                        } catch (e: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[CHECK_SESSION] refreshSession failed", e)
                }
                
                // wait a tiny bit for the session status to propagate if it's asynchronous
                kotlinx.coroutines.delay(100)

                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    sharedPreferences.edit().putString("refresh_token", session.refreshToken).apply()
                    Log.d(TAG, "[CHECK_SESSION] Refresh succeeded. User: ${session.user?.id}")
                } else {
                    Log.e(TAG, "[CHECK_SESSION] refreshSession() called but currentSessionOrNull() is still null!")
                }
                _currentUser.value = supabase.auth.currentUserOrNull()
                val result = _currentUser.value != null
                Log.d(TAG, "[CHECK_SESSION] Final result: $result")
                result
            } else {
                Log.d(TAG, "[CHECK_SESSION] No token in storage — routing to Login")
                _currentUser.value = null
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[CHECK_SESSION] Exception during session restore: ${e.message}", e)
            sharedPreferences.edit().remove("refresh_token").apply()
            _currentUser.value = null
            false
        }
    }

    /**
     * Sends a password reset email to [email].
     * Returns true on success.
     */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Resends the account confirmation email to [email].
     * Returns true on success.
     */
    suspend fun resendConfirmationEmail(email: String): Boolean {
        return try {
            // supabase-kt 3.x: resendEmail takes OtpType.Email, not the Email provider
            supabase.auth.resendEmail(io.github.jan.supabase.auth.OtpType.Email.SIGNUP, email)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
