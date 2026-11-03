package org.kinesis.logger.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF090D16)
val SurfaceDark = Color(0xFF111827)
val SurfaceElevated = Color(0xFF1A2234)
val SurfaceHighlight = Color(0xFF222D42)
val CardBorder = Color(0xFF1E293B)
val CardBorderHighlight = Color(0xFF334155)

val CyanPrimary = Color(0xFF06B6D4)
val CyanBright = Color(0xFF22D3EE)
val CyanGlow = Color(0x3306B6D4)

val BlueAccent = Color(0xFF3B82F6)
val BlueBright = Color(0xFF60A5FA)

val VioletAccent = Color(0xFF8B5CF6)
val VioletDeep = Color(0xFF6D28D9)

val GreenSuccess = Color(0xFF10B981)
val GreenSuccessBg = Color(0x2210B981)

val AmberWarning = Color(0xFFF59E0B)
val AmberWarningBg = Color(0x22F59E0B)

val RedAlert = Color(0xFFEF4444)
val RedAlertBg = Color(0x22EF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val GradientCyanBlue = Brush.horizontalGradient(
    colors = listOf(CyanPrimary, BlueAccent)
)

val GradientCyanViolet = Brush.horizontalGradient(
    colors = listOf(CyanPrimary, VioletAccent)
)

val GradientCardBorder = Brush.linearGradient(
    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF818CF8).copy(alpha = 0.15f))
)
