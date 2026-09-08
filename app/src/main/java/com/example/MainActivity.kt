package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.iot.DemoIotService
import com.example.iot.SupabaseIotService
import com.example.ui.AppNavigation
import com.example.ui.theme.AppTheme
import com.example.viewmodel.MainViewModel

import com.example.data.ThemePreferences
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.ThemeMode

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val dao = database.appDao()
    
    // Create Demo service as fallback
    val demoService = DemoIotService(dao)
    
    // Create Supabase service wrapping demo service
    val iotService = SupabaseIotService(demoService)
    
    val themePreferences = ThemePreferences(this)
    
    val viewModel = MainViewModel(iotService, dao, themePreferences)
    
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
