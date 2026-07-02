package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ZenTeal,
    secondary = ZenViolet,
    tertiary = ZenAmber,
    background = ZenDeepNight,
    surface = ZenSurface,
    onPrimary = ZenDeepNight,
    onSecondary = ZenDeepNight,
    onBackground = ZenTextWhite,
    onSurface = ZenTextWhite,
    surfaceVariant = ZenSurfaceVariant,
    onSurfaceVariant = ZenSoftGray
  )

private val LightColorScheme = DarkColorScheme // We keep the app in a cohesive zen dark theme for optimal ambient reflection!

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
