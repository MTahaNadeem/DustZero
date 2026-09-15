package com.dustzero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dustzero.app.data.AppDatabase
import com.dustzero.app.iot.DemoIotService
import com.dustzero.app.iot.SupabaseIotService
import com.dustzero.app.ui.AppNavigation
import com.dustzero.app.ui.theme.AppTheme
import com.dustzero.app.viewmodel.MainViewModel
import com.dustzero.app.data.AuthRepository

import com.dustzero.app.data.ThemePreferences
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
        delay(1500) // Delay to show splash logo, ideally replace with auth check
        isReady = true
    }
    splashScreen.setKeepOnScreenCondition { !isReady }
    
    val database = AppDatabase.getDatabase(this)
    val dao = database.appDao()
    
    // Create Demo service as fallback
    val demoService = DemoIotService(dao)
    
    // Create Supabase service wrapping demo service; dao is needed for local alert generation
    val iotService = SupabaseIotService(demoService, dao)
    
    val themePreferences = ThemePreferences(this)
    val authRepository = AuthRepository(this)
    
    val viewModel = MainViewModel(iotService, dao, themePreferences, authRepository)
    
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
