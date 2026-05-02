package com.griffith.chatbot.ui.theme

import androidx.compose.ui.graphics.Color

// Aptitude App Color Palette - Light Theme
val AptitudePrimary = Color(0xFF00BFFF) // Bright cyan blue from logo
val AptitudePrimaryVariant = Color(0xFF0099CC) // Darker blue variant
val AptitudeSecondary = Color(0xFF1E88E5) // Secondary blue
val AptitudeSecondaryVariant = Color(0xFF1565C0) // Darker secondary

// Accent and Supporting Colors
val AptitudeAccent = Color(0xFF00D4FF) // Lighter accent blue
val AptitudeTertiary = Color(0xFF42A5F5) // Tertiary blue
val AptitudeTertiaryVariant = Color(0xFF1976D2) // Darker tertiary

// Surface and Background Colors - Light Theme
val AptitudeSurface = Color(0xFFF8FEFF) // Very light blue tint
val AptitudeSurfaceVariant = Color(0xFFE3F2FD) // Light blue surface
val AptitudeBackground = Color(0xFFFFFFFF) // Pure white
val AptitudeInverseSurface = Color(0xFF1A1A1A) // Dark surface for contrast

// On-Color Text Colors - Light Theme
val AptitudeOnPrimary = Color(0xFFFFFFFF) // White text on primary
val AptitudeOnSecondary = Color(0xFFFFFFFF) // White text on secondary
val AptitudeOnTertiary = Color(0xFFFFFFFF) // White text on tertiary
val AptitudeOnSurface = Color(0xFF1A1A1A) // Dark text on surface
val AptitudeOnSurfaceVariant = Color(0xFF2C2C2C) // Slightly lighter dark text
val AptitudeOnBackground = Color(0xFF1A1A1A) // Dark text on background

// Container Colors - Light Theme
val AptitudePrimaryContainer = Color(0xFFE1F5FE) // Very light blue container
val AptitudeSecondaryContainer = Color(0xFFE3F2FD) // Light blue container
val AptitudeTertiaryContainer = Color(0xFFE8F4FD) // Tertiary container

val AptitudeOnPrimaryContainer = Color(0xFF001F2A) // Dark text on primary container
val AptitudeOnSecondaryContainer = Color(0xFF0D1B2C) // Dark text on secondary container
val AptitudeOnTertiaryContainer = Color(0xFF0A1929) // Dark text on tertiary container

// Error Colors
val AptitudeError = Color(0xFFD32F2F) // Error red
val AptitudeOnError = Color(0xFFFFFFFF) // White text on error
val AptitudeErrorContainer = Color(0xFFFFEBEE) // Light error container
val AptitudeOnErrorContainer = Color(0xFFB71C1C) // Dark text on error container

// Outline and Border Colors
val AptitudeOutline = Color(0xFFBDBDBD) // Light gray outline
val AptitudeOutlineVariant = Color(0xFFE0E0E0) // Very light gray outline

// Inverse Colors for dark elements
val AptitudeInverseOnSurface = Color(0xFFE0E0E0) // Light text on dark surface
val AptitudeInversePrimary = Color(0xFF80D8FF) // Light primary for dark backgrounds

// Scrim
val AptitudeScrim = Color(0xFF000000) // Black scrim

// Dark Theme Colors
val AptitudePrimaryDark = Color(0xFF80D8FF) // Light blue for dark theme
val AptitudePrimaryVariantDark = Color(0xFF40C4FF) // Lighter blue variant for dark
val AptitudeSecondaryDark = Color(0xFF82B1FF) // Light secondary for dark theme
val AptitudeSecondaryVariantDark = Color(0xFF448AFF) // Lighter secondary variant for dark

// Dark Theme Surface Colors
val AptitudeSurfaceDark = Color(0xFF121212) // Dark surface
val AptitudeSurfaceVariantDark = Color(0xFF1E1E1E) // Dark surface variant
val AptitudeBackgroundDark = Color(0xFF0A0A0A) // Very dark background

// Dark Theme On-Colors
val AptitudeOnPrimaryDark = Color(0xFF000000) // Black text on light primary
val AptitudeOnSecondaryDark = Color(0xFF000000) // Black text on light secondary
val AptitudeOnSurfaceDark = Color(0xFFE0E0E0) // Light text on dark surface
val AptitudeOnBackgroundDark = Color(0xFFE0E0E0) // Light text on dark background

// Dark Theme Container Colors
val AptitudePrimaryContainerDark = Color(0xFF004658) // Dark blue container
val AptitudeSecondaryContainerDark = Color(0xFF1A2332) // Dark secondary container
val AptitudeTertiaryContainerDark = Color(0xFF0D1B2A) // Dark tertiary container

val AptitudeOnPrimaryContainerDark = Color(0xFFB3E5FC) // Light text on dark primary container
val AptitudeOnSecondaryContainerDark = Color(0xFFBBDEFB) // Light text on dark secondary container
val AptitudeOnTertiaryContainerDark = Color(0xFFC8E6FF) // Light text on dark tertiary container

// Special UI Colors
val AptitudeSuccess = Color(0xFF4CAF50) // Success green
val AptitudeWarning = Color(0xFFFF9800) // Warning orange
val AptitudeInfo = Color(0xFF2196F3) // Info blue (similar to secondary)

// Chat-specific colors
val AptitudeUserMessage = AptitudePrimary // User message bubble
val AptitudeAIMessage = AptitudeSurfaceVariant // AI message bubble
val AptitudeSystemMessage = AptitudeTertiaryContainer // System message bubble

// File processing colors
val AptitudeProcessing = AptitudeAccent // File processing indicator
val AptitudeFileSuccess = AptitudeSuccess // File upload success
val AptitudeFileError = AptitudeError // File upload error

// Gradient colors for enhanced UI
val AptitudeGradientStart = AptitudePrimary
val AptitudeGradientEnd = AptitudeAccent

// Legacy colors (for backward compatibility - can be removed later)
@Deprecated("Use AptitudePrimary instead", ReplaceWith("AptitudePrimary"))
val Purple80 = Color(0xFFD0BCFF)
@Deprecated("Use AptitudeSurfaceVariant instead", ReplaceWith("AptitudeSurfaceVariant"))
val PurpleGrey80 = Color(0xFFCCC2DC)
@Deprecated("Use AptitudeError instead", ReplaceWith("AptitudeError"))
val Pink80 = Color(0xFFEFB8C8)

@Deprecated("Use AptitudePrimaryVariant instead", ReplaceWith("AptitudePrimaryVariant"))
val Purple40 = Color(0xFF6650a4)
@Deprecated("Use AptitudeOnSurfaceVariant instead", ReplaceWith("AptitudeOnSurfaceVariant"))
val PurpleGrey40 = Color(0xFF625b71)
@Deprecated("Use AptitudeError instead", ReplaceWith("AptitudeError"))
val Pink40 = Color(0xFF7D5260)