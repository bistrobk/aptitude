package com.griffith.chatbot.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Aptitude Dark Color Scheme
private val AptitudeDarkColorScheme = darkColorScheme(
    // Primary colors
    primary = AptitudePrimaryDark,
    onPrimary = AptitudeOnPrimaryDark,
    primaryContainer = AptitudePrimaryContainerDark,
    onPrimaryContainer = AptitudeOnPrimaryContainerDark,

    // Secondary colors
    secondary = AptitudeSecondaryDark,
    onSecondary = AptitudeOnSecondaryDark,
    secondaryContainer = AptitudeSecondaryContainerDark,
    onSecondaryContainer = AptitudeOnSecondaryContainerDark,

    // Tertiary colors
    tertiary = AptitudeTertiary,
    onTertiary = AptitudeOnTertiary,
    tertiaryContainer = AptitudeTertiaryContainerDark,
    onTertiaryContainer = AptitudeOnTertiaryContainerDark,

    // Error colors
    error = AptitudeError,
    onError = AptitudeOnError,
    errorContainer = AptitudeErrorContainer,
    onErrorContainer = AptitudeOnErrorContainer,

    // Surface colors
    surface = AptitudeSurfaceDark,
    onSurface = AptitudeOnSurfaceDark,
    surfaceVariant = AptitudeSurfaceVariantDark,
    onSurfaceVariant = AptitudeOnSurfaceDark,

    // Background colors
    background = AptitudeBackgroundDark,
    onBackground = AptitudeOnBackgroundDark,

    // Outline colors
    outline = AptitudeOutline,
    outlineVariant = AptitudeOutlineVariant,

    // Inverse colors
    inverseSurface = AptitudeSurface,
    inverseOnSurface = AptitudeOnSurface,
    inversePrimary = AptitudeInversePrimary,

    // Scrim
    scrim = AptitudeScrim
)

// Aptitude Light Color Scheme
private val AptitudeLightColorScheme = lightColorScheme(
    // Primary colors
    primary = AptitudePrimary,
    onPrimary = AptitudeOnPrimary,
    primaryContainer = AptitudePrimaryContainer,
    onPrimaryContainer = AptitudeOnPrimaryContainer,

    // Secondary colors
    secondary = AptitudeSecondary,
    onSecondary = AptitudeOnSecondary,
    secondaryContainer = AptitudeSecondaryContainer,
    onSecondaryContainer = AptitudeOnSecondaryContainer,

    // Tertiary colors
    tertiary = AptitudeTertiary,
    onTertiary = AptitudeOnTertiary,
    tertiaryContainer = AptitudeTertiaryContainer,
    onTertiaryContainer = AptitudeOnTertiaryContainer,

    // Error colors
    error = AptitudeError,
    onError = AptitudeOnError,
    errorContainer = AptitudeErrorContainer,
    onErrorContainer = AptitudeOnErrorContainer,

    // Surface colors
    surface = AptitudeSurface,
    onSurface = AptitudeOnSurface,
    surfaceVariant = AptitudeSurfaceVariant,
    onSurfaceVariant = AptitudeOnSurfaceVariant,

    // Background colors
    background = AptitudeBackground,
    onBackground = AptitudeOnBackground,

    // Outline colors
    outline = AptitudeOutline,
    outlineVariant = AptitudeOutlineVariant,

    // Inverse colors
    inverseSurface = AptitudeInverseSurface,
    inverseOnSurface = AptitudeInverseOnSurface,
    inversePrimary = AptitudeInversePrimary,

    // Scrim
    scrim = AptitudeScrim
)

// Legacy color schemes for backward compatibility
@Deprecated("Use AptitudeDarkColorScheme instead", ReplaceWith("AptitudeDarkColorScheme"))
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

@Deprecated("Use AptitudeLightColorScheme instead", ReplaceWith("AptitudeLightColorScheme"))
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun AptitudeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Set to false by default to maintain Aptitude branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Only use dynamic colors if explicitly requested and supported
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use Aptitude branded themes by default
        darkTheme -> AptitudeDarkColorScheme
        else -> AptitudeLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AptitudeTypography,
        content = content
    )
}

// Backward compatibility alias
@Deprecated(
    "Use AptitudeTheme instead",
    ReplaceWith("AptitudeTheme(darkTheme, dynamicColor, content)")
)
@Composable
fun ChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Changed default to maintain branding
    content: @Composable () -> Unit
) {
    AptitudeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

// Theme utilities for accessing colors programmatically
object AptitudeColors {
    val Primary = AptitudePrimary
    val PrimaryVariant = AptitudePrimaryVariant
    val Secondary = AptitudeSecondary
    val SecondaryVariant = AptitudeSecondaryVariant
    val Accent = AptitudeAccent
    val Tertiary = AptitudeTertiary
    val TertiaryVariant = AptitudeTertiaryVariant
    val Surface = AptitudeSurface
    val SurfaceVariant = AptitudeSurfaceVariant
    val Background = AptitudeBackground
    val OnPrimary = AptitudeOnPrimary
    val OnSurface = AptitudeOnSurface
    val OnBackground = AptitudeOnBackground
    val OnSurfaceVariant = AptitudeOnSurfaceVariant
    val Success = AptitudeSuccess
    val Warning = AptitudeWarning
    val Info = AptitudeInfo
    val UserMessage = AptitudeUserMessage
    val AIMessage = AptitudeAIMessage
    val SystemMessage = AptitudeSystemMessage
    val Processing = AptitudeProcessing
    val FileSuccess = AptitudeFileSuccess
    val FileError = AptitudeFileError
    val GradientStart = AptitudeGradientStart
    val GradientEnd = AptitudeGradientEnd
}