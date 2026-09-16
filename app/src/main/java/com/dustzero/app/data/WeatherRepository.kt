package com.dustzero.app.data

import android.util.Log
import com.dustzero.app.iot.SupabaseClientProvider
import com.dustzero.app.models.WeatherData
import com.dustzero.app.models.WeatherResponse
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeatherRepository {
    private val supabase = SupabaseClientProvider.client
    private val TAG = "WeatherRepository"

    private var cachedWeather: WeatherData? = null
    private var lastFetchTime = 0L
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    private val mutex = Mutex()

    /**
     * Fetches weather insight for the given coordinates.
     * Caches the result for 30 minutes to save API calls.
     */
    suspend fun getWeatherInsight(lat: Double, lon: Double): Result<WeatherData> = mutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedWeather != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            Log.d(TAG, "Returning cached weather insight")
            return Result.success(cachedWeather!!)
        }

        return try {
            Log.d(TAG, "Fetching fresh weather insight from edge function")
            
            // Supabase-kt uses Ktor under the hood for requests. 
            val response = supabase.functions.invoke("get-weather") {
                method = HttpMethod.Get
                parameter("lat", lat.toString())
                parameter("lon", lon.toString())
            }.body<WeatherResponse>()

            val insight = generateCleaningInsight(response)
            
            val weatherData = WeatherData(
                current = response.current,
                next12Hours = response.next_12_hours,
                cleaningInsight = insight
            )

            cachedWeather = weatherData
            lastFetchTime = now
            Result.success(weatherData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather insight", e)
            Result.failure(e)
        }
    }
    
    private fun generateCleaningInsight(response: WeatherResponse): String {
        // Find if there's any rain expected in the next 12 hours
        val rainForecast = response.next_12_hours.indexOfFirst { 
            it.conditions.contains("Rain", ignoreCase = true) || it.pop >= 50 
        }
        
        return if (rainForecast != -1) {
            val hours = (rainForecast + 1) * 3
            "Rain expected in ~$hours hours — automatic cleaning will be blocked during rainfall."
        } else {
            "Clear skies for the next 12 hours — good conditions for automatic cleaning."
        }
    }
    
    fun clearCache() {
        cachedWeather = null
        lastFetchTime = 0L
    }
}
