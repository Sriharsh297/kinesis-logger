package org.kinesis.logger.ui.recording

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import org.kinesis.logger.model.Task
import org.kinesis.logger.service.RecordingPhase
import org.kinesis.logger.service.RecordingService
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisDangerButton
import org.kinesis.logger.ui.components.KinesisHoldToStopButton
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.components.MetricChip
import org.kinesis.logger.ui.components.StatusBadge
import org.kinesis.logger.ui.components.StatusLevel
import org.kinesis.logger.ui.theme.AmberWarning
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.CardBorder
import org.kinesis.logger.ui.theme.CyanBright
import org.kinesis.logger.ui.theme.CyanPrimary
import org.kinesis.logger.ui.theme.GreenSuccess
import org.kinesis.logger.ui.theme.RedAlert
import org.kinesis.logger.ui.theme.Shapes
import org.kinesis.logger.ui.theme.SurfaceDark
import org.kinesis.logger.ui.theme.SurfaceElevated
import org.kinesis.logger.ui.theme.TextMuted
import org.kinesis.logger.ui.theme.TextPrimary
import org.kinesis.logger.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordingScreen(
    task: Task,
    subjectId: String,
    onFinishRecording: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe global hudState directly from RecordingService companion — avoids race condition
    val hudState by RecordingService.hudState.collectAsState()

    var showMarkerDialog by remember { mutableStateOf(false) }
    var customMarkerText by remember { mutableStateOf("") }
    var eventFeedbackNote by remember { mutableStateOf<String?>(null) }
    var showBackDialog by remember { mutableStateOf(false) }

    val quickMarkers = listOf("turn", "crossing", "stop_pause", "stairs", "shoe_adjust", "phone_bump")
    val scrollState = rememberScrollState()

    // Hide transient event feedback after 3 seconds
    LaunchedEffect(eventFeedbackNote) {
        if (eventFeedbackNote != null) {
            delay(3000L)
            eventFeedbackNote = null
        }
    }

    // Intercept Back button during active recording to prevent accidental data loss or orphaned service
    val isRecordingActive = hudState.phase != RecordingPhase.IDLE && hudState.phase != RecordingPhase.COMPLETED
    BackHandler(enabled = isRecordingActive) {
        showBackDialog = true
    }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = {
                Text(
                    text = "Recording In Progress",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "A sensor recording bout is actively running. Terminating now will stop data collection.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackDialog = false
                        RecordingService.stopBoutEarly(context)
                    }
                ) {
                    Text("Stop & Finalize", color = CyanPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackDialog = false }
                ) {
                    Text("Keep Recording", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phase Banner
        val (phaseText, phaseLevel) = when (hudState.phase) {
            RecordingPhase.COUNTDOWN -> "COUNTDOWN (${hudState.phaseRemainingSeconds}s)" to StatusLevel.WARN
            RecordingPhase.STILL_INITIAL -> "INITIAL STILLNESS (${hudState.phaseRemainingSeconds}s)" to StatusLevel.INFO
            RecordingPhase.BOUT -> "RECORDING BOUT" to StatusLevel.PASS
            RecordingPhase.STILL_FINAL -> "FINAL STILLNESS (${hudState.phaseRemainingSeconds}s)" to StatusLevel.WARN
            RecordingPhase.COMPLETED -> "RECORDING COMPLETE" to StatusLevel.PASS
            RecordingPhase.IDLE -> "INITIALIZING..." to StatusLevel.INFO
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(text = phaseText, level = phaseLevel)
            Text(
                text = "$subjectId • ${task.taskId}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Elapsed Digital Timer
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(
                    width = 2.dp,
                    color = when (hudState.phase) {
                        RecordingPhase.BOUT -> RedAlert
                        RecordingPhase.COUNTDOWN, RecordingPhase.STILL_INITIAL, RecordingPhase.STILL_FINAL -> AmberWarning
                        RecordingPhase.COMPLETED -> GreenSuccess
                        else -> CardBorder
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hudState.phase == RecordingPhase.COUNTDOWN ||
                    hudState.phase == RecordingPhase.STILL_INITIAL ||
                    hudState.phase == RecordingPhase.STILL_FINAL
                ) {
                    Text(
                        text = "${hudState.phaseRemainingSeconds}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (hudState.phase) {
                            RecordingPhase.COUNTDOWN -> "GET READY"
                            RecordingPhase.STILL_INITIAL -> "HOLD STILL (CALIBRATING)"
                            else -> "HOLD STILL (FINALIZING)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        letterSpacing = 0.8.sp
                    )
                } else {
                    val minutes = hudState.boutElapsedSeconds / 60
                    val seconds = hudState.boutElapsedSeconds % 60
                    val timerString = String.format("%02d:%02d", minutes, seconds)

                    val plannedMin = task.boutPlannedS / 60
                    val plannedSec = task.boutPlannedS % 60
                    val plannedString = String.format("%02d:%02d", plannedMin, plannedSec)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hudState.phase == RecordingPhase.BOUT) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(RedAlert)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = timerString,
                            fontSize = 44.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PLANNED: $plannedString",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Event Feedback Toast Banner
        AnimatedVisibility(
            visible = eventFeedbackNote != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(Shapes.small)
                    .background(CyanPrimary.copy(alpha = 0.2f))
                    .border(1.dp, CyanPrimary, Shapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓ Event Marked: $eventFeedbackNote",
                    color = CyanBright,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Task Summary Pill
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = CardBorder
        ) {
            Column {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Placement: ${task.placement} • Speed: ${task.speedLabel}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Telemetry Health Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val accelDisplay = when {
                hudState.accelLiveHz > 0f -> String.format("%.1f Hz", hudState.accelLiveHz)
                hudState.accelSamplesReceived > 0 -> "${hudState.accelSamplesReceived} pts"
                else -> "Waiting"
            }
            val accelOk = hudState.accelSamplesReceived > 0

            MetricChip(
                label = "Accel Sample Rate",
                value = accelDisplay,
                unit = null,
                valueColor = if (accelOk) GreenSuccess else AmberWarning,
                modifier = Modifier.weight(1f)
            )

            val gyroDisplay = when {
                hudState.gyroLiveHz > 0f -> String.format("%.1f Hz", hudState.gyroLiveHz)
                hudState.gyroSamplesReceived > 0 -> "${hudState.gyroSamplesReceived} pts"
                else -> "Waiting"
            }
            val gyroOk = hudState.gyroSamplesReceived > 0

            MetricChip(
                label = "Gyro Sample Rate",
                value = gyroDisplay,
                unit = null,
                valueColor = if (gyroOk) GreenSuccess else AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val gpsText = if (hudState.gpsAccuracyMeters > 0f) {
                String.format("%.1fm (#%d)", hudState.gpsAccuracyMeters, hudState.gpsFixCount)
            } else {
                "Fixing (Indoor)"
            }
            val gpsColor = when {
                hudState.gpsAccuracyMeters in 0.1f..20f -> GreenSuccess
                hudState.gpsAccuracyMeters > 20f -> AmberWarning
                else -> AmberWarning
            }

            MetricChip(
                label = "GPS Accuracy",
                value = gpsText,
                valueColor = gpsColor,
                modifier = Modifier.weight(1f)
            )
            MetricChip(
                label = "OS Steps",
                value = "${hudState.osStepsReference}",
                unit = "(weak ref)",
                valueColor = CyanBright,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Event Marker Button
        KinesisPrimaryButton(
            text = "＋ MARK EVENT (Turn, Stop, Crossing...)",
            onClick = { showMarkerDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = hudState.phase == RecordingPhase.BOUT
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hold-to-Stop Button or Completion Button
        if (hudState.phase == RecordingPhase.COMPLETED) {
            KinesisPrimaryButton(
                text = "VIEW SESSION SUMMARY",
                onClick = { onFinishRecording(hudState.sessionId) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            KinesisHoldToStopButton(
                onHoldComplete = {
                    RecordingService.stopBoutEarly(context)
                },
                enabled = !hudState.isStopping && hudState.phase != RecordingPhase.IDLE,
                label = if (hudState.isStopping) "FINALIZING STILLNESS..." else "HOLD 2s TO STOP BOUT"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Mark Event Dialog
    if (showMarkerDialog) {
        Dialog(onDismissRequest = { showMarkerDialog = false }) {
            KinesisCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = CardBorder
            ) {
                Column {
                    Text(
                        text = "Mark Ground-Truth Event",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickMarkers.forEach { marker ->
                            AssistChip(
                                onClick = {
                                    RecordingService.markEvent(context, marker)
                                    eventFeedbackNote = marker
                                    showMarkerDialog = false
                                },
                                label = { Text(marker, fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SurfaceElevated,
                                    labelColor = CyanBright
                                ),
                                border = AssistChipDefaults.assistChipBorder(true, CardBorder)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customMarkerText,
                        onValueChange = { customMarkerText = it },
                        label = { Text("Custom event note") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showMarkerDialog = false },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (customMarkerText.isNotBlank()) {
                                    val note = customMarkerText.trim()
                                    RecordingService.markEvent(context, note)
                                    eventFeedbackNote = note
                                }
                                customMarkerText = ""
                                showMarkerDialog = false
                            },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Submit", color = CyanPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
