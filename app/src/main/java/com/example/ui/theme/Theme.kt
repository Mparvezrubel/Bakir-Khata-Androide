package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = KhataDarkPrimary,
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF005328),
    onPrimaryContainer = Color(0xFFA6F5B9),
    secondary = Color(0xFF8CD7A7),
    onSecondary = Color(0xFF00381D),
    background = KhataDarkBackground,
    surface = KhataDarkSurface,
    surfaceVariant = KhataDarkSurfaceVariant,
    onBackground = KhataDarkTextPrimary,
    onSurface = KhataDarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = KhataPrimary,
    onPrimary = KhataOnPrimary,
    primaryContainer = KhataPrimaryContainer,
    onPrimaryContainer = KhataOnPrimaryContainer,
    secondary = KhataSecondary,
    onSecondary = KhataOnSecondary,
    secondaryContainer = KhataSecondaryContainer,
    onSecondaryContainer = KhataOnSecondaryContainer,
    background = KhataBackground,
    surface = KhataSurface,
    surfaceVariant = KhataSurfaceVariant,
    outline = KhataOutline,
    onBackground = KhataTextPrimary,
    onSurface = KhataTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep custom branding consistent
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
