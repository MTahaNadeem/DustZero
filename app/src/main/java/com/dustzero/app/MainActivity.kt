package com.dustzero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dustzero.app.data.AppDatabase
import com.dustzero.app.data.DevicePreferences
import com.dustzero.app.data.DeviceRepository
import com.dustzero.app.iot.DemoIotService
import com.dustzero.app.iot.SupabaseIotService
import com.dustzero.app.ui.AppNavigation
import com.dustzero.app.ui.theme.AppTheme
import com.dustzero.app.viewmodel.MainViewModel
import com.dustzero.app.data.AuthRepository
import com.dustzero.app.data.ThemePreferences
import com.dustzero.app.data.LocationService
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.dustzero.app.data.ThemeMode
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    var isReady = false
    lifecycleScope.launch {
        delay(1500)
        isReady = true
    }
    splashScreen.setKeepOnScreenCondition { !isReady }
    
    // Trigger widget update on app open
    com.dustzero.app.widget.WidgetUpdateWorker.enqueueImmediate(this)

    val database = AppDatabase.getDatabase(this)
    val dao = database.appDao()

    // Device preferences — persists the active device_id across restarts
    val devicePreferences = DevicePreferences(this)

    // Demo service as fallback for when Supabase is unconfigured
    val demoService = DemoIotService(dao)

    // Supabase IoT service — initialise with the previously saved device_id (null = no device yet)
    val iotService = SupabaseIotService(
        context = this.applicationContext,
        fallbackDemoService = demoService,
        dao = dao,
        initialDeviceId = devicePreferences.activeDeviceId.value
    )

    val themePreferences = ThemePreferences(this)
    val authRepository = AuthRepository(this)
    val deviceRepository = DeviceRepository()
    val weatherRepository = com.dustzero.app.data.WeatherRepository()
    val locationService = LocationService(this)

    val viewModel = MainViewModel(
        iotService = iotService,
        dao = dao,
        themePreferences = themePreferences,
        authRepository = authRepository,
        devicePreferences = devicePreferences,
        deviceRepository = deviceRepository,
        weatherRepository = weatherRepository,
        locationService = locationService
    )

    setContent {
      val currentThemeMode by themePreferences.themeMode.collectAsState()

      val useDarkTheme = when (currentThemeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      AppTheme(useDarkTheme = useDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation(viewModel = viewModel)
        }
      }
    }
  }
}
