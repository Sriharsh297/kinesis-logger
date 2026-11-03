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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.BlueAccent
import org.kinesis.logger.ui.theme.CardBorder
import org.kinesis.logger.ui.theme.CyanPrimary
import org.kinesis.logger.ui.theme.GradientCyanBlue
import org.kinesis.logger.ui.theme.RedAlert
import org.kinesis.logger.ui.theme.Shapes
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
            disabledContainerColor = Color(0xFF1E293B)
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B))))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) BgDark else Color(0xFF64748B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
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
        modifier = modifier
            .height(48.dp),
        enabled = enabled,
        shape = Shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF111827),
            contentColor = TextPrimary
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
            containerColor = Color(0xFF2A1519),
            contentColor = RedAlert
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D))
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun KinesisHoldToStopButton(
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 2000L,
    label: String = "HOLD 2s TO STOP"
) {
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            while (isPressed) {
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
            .background(Color(0xFF221115))
            .border(1.dp, Color(0xFF7F1D1D), Shapes.medium)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(56.dp)
                .background(RedAlert.copy(alpha = 0.5f))
        )
        // Button text
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPressed) "HOLDING... ${(progress * 100).toInt()}%" else label,
                color = if (isPressed) Color.White else RedAlert,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
