package org.kinesis.logger.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.kinesis.logger.ui.theme.CardBorder
import org.kinesis.logger.ui.theme.GradientCyanBlue
import org.kinesis.logger.ui.theme.RedAlert
import org.kinesis.logger.ui.theme.RedAlertBg
import org.kinesis.logger.ui.theme.Shapes
import org.kinesis.logger.ui.theme.SurfaceDark
import org.kinesis.logger.ui.theme.SurfaceElevated
import org.kinesis.logger.ui.theme.TextMuted
import org.kinesis.logger.ui.theme.TextPrimary

@Composable
fun KinesisPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    brush: Brush = GradientCyanBlue
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = Shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = SurfaceElevated
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(if (enabled) brush else Brush.linearGradient(listOf(SurfaceElevated, SurfaceElevated)))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) Color(0xFF0F172A) else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun KinesisSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = Shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            disabledContainerColor = SurfaceDark.copy(alpha = 0.5f),
            disabledContentColor = TextMuted
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun KinesisDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = Shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = RedAlertBg,
            contentColor = RedAlert
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun KinesisHoldToStopButton(
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 2000L,
    label: String = "HOLD 2s TO STOP BOUT",
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            val startTime = System.currentTimeMillis()
            while (isPressed && enabled) {
                val elapsed = System.currentTimeMillis() - startTime
                val currentProgress = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                progress = currentProgress
                if (currentProgress >= 1f) {
                    onHoldComplete()
                    isPressed = false
                    break
                }
                delay(16)
            }
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(Shapes.medium)
            .background(if (enabled) Color(0xFF2B1D24) else SurfaceDark)
            .border(1.dp, if (enabled) RedAlert.copy(alpha = 0.4f) else CardBorder, Shapes.medium)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                val released = tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress fill
        if (enabled && progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(56.dp)
                    .background(RedAlert.copy(alpha = 0.5f))
            )
        }
        // Button text
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    !enabled -> "FINALIZING & COMPUTING..."
                    isPressed -> "HOLDING... ${(progress * 100).toInt()}%"
                    else -> label
                },
                color = when {
                    !enabled -> TextMuted
                    isPressed -> Color.White
                    else -> RedAlert
                },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
