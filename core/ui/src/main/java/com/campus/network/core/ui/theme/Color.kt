package com.campus.network.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Patriarch Design System - Color Palette
 * Derived directly from Auth, Splash, and Onboarding screens.
 * Constant across the entire application.
 */

// Signature Patriarch Blues
val BrandBluePrimary = Color(0xFF014784)    // Splash background, Onboarding gradient top, Primary Brand
val BrandBlueMid = Color(0xFF2561A1)        // Onboarding gradient mid, Accent Blue
val BrandBlueAuth = Color(0xFF164783)       // Auth background
val BrandBlueAction = Color(0xFF2658B7)     // Onboarding Next button, interactive CTA
val BrandBlueInteractive = Color(0xFF0284C7)// High-contrast link & active badge blue
val BrandBlueLight = Color(0xFFE0EDFA)      // Light active tab/pill highlight

// Patriarch Heritage Golds & Accents
val BrandGold = Color(0xFFEAB308)           // Patriarch Gold accent strip & icons
val BrandGoldDark = Color(0xFFD97706)       // Dark Gold badge
val BrandGoldSoft = Color(0xFFFEF08A)       // Soft yellow pill background ("See new people in 18 hours")
val BrandGoldText = Color(0xFF854D0E)       // Dark yellow pill text

// Verification & Success
val BrandGreen = Color(0xFF16A34A)          // Onboarding Checkmark green, verified live indicator
val BrandGreenSoft = Color(0xFFF0FDF4)      // Soft green background

// Urgent / Alert
val AlertRed = Color(0xFFEF4444)            // Expiry / notification badge coral
val AlertRedSoft = Color(0xFFFEE2E2)        // Soft red background

// Neutrals & Slate Hierarchy
val TextPrimary = Color(0xFF0F172A)         // Slate 900 (High-contrast deep navy ink from Auth modal & Onboarding sheet)
val TextSecondary = Color(0xFF64748B)       // Slate 500 (Clean secondary labels & subtitles)
val TextMuted = Color(0xFF94A3B8)           // Slate 400 (Placeholder & disabled state)
val TextWhite = Color(0xFFFFFFFF)           // Pure white text on brand dark backgrounds

// Surfaces & Backgrounds
val SurfaceWhite = Color(0xFFFFFFFF)        // Card & screen white surface
val SurfaceSubtle = Color(0xFFF8FAFC)       // Slate 50 subtle background
val SurfaceElevated = Color(0xFFF1F5F9)     // Slate 100 pill & search surfaces
val SurfaceSheet = Color(0xF4F0F5FA)        // Onboarding Frosted Bottom Sheet background
val BorderSubtle = Color(0xFFE2E8F0)        // Slate 200 hairline border
val BorderStrong = Color(0xFFCBD5E1)        // Slate 300 input border

// Compatibility Aliases
val CampusPrimary = BrandBluePrimary
val CampusPrimaryDark = Color(0xFF0A335C)
val CampusSecondary = BrandBlueMid
val CampusAccentGold = BrandGold
val CampusAlertRed = AlertRed

val DarkBackground = Color(0xFF0B1320)
val DarkSurface = Color(0xFF111D30)
val DarkSurfaceElevated = Color(0xFF192B45)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

val LightBackground = SurfaceSubtle
val LightSurface = SurfaceWhite
val LightSurfaceElevated = SurfaceElevated
val LightTextPrimary = TextPrimary
val LightTextSecondary = TextSecondary
