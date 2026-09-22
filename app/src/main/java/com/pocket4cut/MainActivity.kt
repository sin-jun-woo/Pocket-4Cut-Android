package com.pocket4cut

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocket4cut.presentation.navigation.PocketNavHost
import com.pocket4cut.presentation.navigation.Routes
import com.pocket4cut.presentation.settings.AppSettings
import com.pocket4cut.ui.designsystem.theme.ThemeManager
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.theme.Pocket4CutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        AppSettings.init(this)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            DisposableEffect(AppSettings.keepScreenOn) {
                if (AppSettings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            Pocket4CutTheme {
                val navController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                val captureChrome = entry?.destination?.route?.startsWith(Routes.CAPTURE) == true
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (captureChrome) Color.Black else AppColors.Background.primary,
                ) { innerPadding ->
                    PocketNavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}
