package com.example.focusflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun FocusFlowTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors = LocalAppColors.current

    val colorScheme = darkColorScheme(
        primary      = appColors.primary,
        secondary    = CyberSecondary,
        tertiary     = CyberTertiary,
        background   = appColors.bgDeep,
        surface      = appColors.cardBg,
        onPrimary    = if (appColors.isDark) appColors.bgDeep else Color.White,
        onBackground = appColors.textMain,
        onSurface    = appColors.textMain,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.bgDeep.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = appColors.isLightStatusBar
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}