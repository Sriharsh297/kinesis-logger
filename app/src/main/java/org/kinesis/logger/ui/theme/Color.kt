package org.kinesis.logger.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Fitness-Tech & Research Dark Palette
val BgDark = Color(0xFF101827)          // Deep slate navy background
val SurfaceDark = Color(0xFF202D40)     // Main card surface
val SurfaceElevated = Color(0xFF29384D) // Elevated cards & panels
val SurfaceHighlight = Color(0xFF33445C)// Active / highlighted items
val CardBorder = Color(0xFF2B3C52)      // Subtle, clean border
val CardBorderHighlight = Color(0xFF3E5474)

// Accents
val CyanPrimary = Color(0xFF22B8CF)    // Primary cyan accent
val CyanBright = Color(0xFF38D9E9)     // Bright readable cyan
val CyanGlow = Color(0x2622B8CF)       // Restrained soft cyan tint

val BlueAccent = Color(0xFF4285F4)     // Secondary blue accent
val BlueBright = Color(0xFF66A0FF)
val BlueAccentBg = Color(0x244285F4)

val VioletAccent = Color(0xFF7C6BF6)

// Semantic status colors
val GreenSuccess = Color(0xFF20B486)   // PASS / success mint green
val GreenSuccessBg = Color(0x2420B486)

val AmberWarning = Color(0xFFE9A23B)   // WARN / caution warm amber
val AmberWarningBg = Color(0x26E9A23B)

val RedAlert = Color(0xFFE46B72)       // FAIL / error coral red
val RedAlertBg = Color(0x26E46B72)

// Typography colors
val TextPrimary = Color(0xFFF4F7FB)    // Crisp high-contrast text
val TextSecondary = Color(0xFFB0BDCC)  // Readable secondary text
val TextMuted = Color(0xFF7E8E9F)      // Supporting captions & labels

// Subtle, elegant gradients
val GradientCyanBlue = Brush.horizontalGradient(
    colors = listOf(CyanPrimary, BlueAccent)
)

val GradientPrimaryAction = Brush.horizontalGradient(
    colors = listOf(Color(0xFF22B8CF), Color(0xFF3B82F6))
)

val GradientCardHeader = Brush.horizontalGradient(
    colors = listOf(SurfaceElevated, SurfaceDark)
)
