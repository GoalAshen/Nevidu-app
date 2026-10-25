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
  primary = EcoEmerald,
  onPrimary = Color.Black,
  primaryContainer = EcoEmeraldDark,
  onPrimaryContainer = EcoEmeraldLight,
  secondary = EcoCyan,
  onSecondary = Color.Black,
  secondaryContainer = EcoCyanDark,
  onSecondaryContainer = EcoCyanLight,
  tertiary = EcoWaterBlue,
  onTertiary = Color.White,
  background = EcoDarkBg,
  onBackground = EcoDarkTextPrimary,
  surface = EcoDarkSurface,
  onSurface = EcoDarkTextPrimary,
  surfaceVariant = EcoDarkSurfaceVariant,
  onSurfaceVariant = EcoDarkTextSecondary,
  outline = EcoDarkCardBorder,
  error = EcoRed,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = EcoEmeraldDark,
  onPrimary = Color.White,
  primaryContainer = EcoLightSurfaceVariant,
  onPrimaryContainer = EcoEmeraldDark,
  secondary = EcoCyanDark,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCFFAFE),
  onSecondaryContainer = EcoCyanDark,
  tertiary = EcoWaterBlueDark,
  onTertiary = Color.White,
  background = EcoLightBg,
  onBackground = EcoLightTextPrimary,
  surface = EcoLightSurface,
  onSurface = EcoLightTextPrimary,
  surfaceVariant = EcoLightSurfaceVariant,
  onSurfaceVariant = EcoLightTextSecondary,
  outline = EcoLightCardBorder,
  error = EcoRed,
  onError = Color.White
)

@Composable
fun EcoControlTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve distinctive green/blue nature theme by default
  content: @Composable () -> Unit,
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
