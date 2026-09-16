package com.dustzero.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.dustzero.app.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(viewModel: com.dustzero.app.viewmodel.MainViewModel, onSplashComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        
        // Wait for session check to complete (also flips viewModel.sessionCheckComplete = true)
        viewModel.checkAndRestoreSession()
        
        // Ensure splash is visible for at least 1.5 seconds
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 1500) {
            delay(1500 - elapsed)
        }
        
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = "DustZero Logo",
            modifier = Modifier.fillMaxWidth(0.8f),
            contentScale = ContentScale.Fit
        )
    }
}
