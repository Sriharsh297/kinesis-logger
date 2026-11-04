package org.kinesis.logger.ui.recording

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.MutableStateFlow
import org.kinesis.logger.model.Task
import org.kinesis.logger.service.LiveRecordingHud
import org.kinesis.logger.service.RecordingPhase
import org.kinesis.logger.service.RecordingService
import org.kinesis.logger.ui.components.KinesisCard
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
    val service = RecordingService.instance
    val flow = remember(service) { service?.hudState ?: MutableStateFlow(LiveRecordingHud()) }
    val hudState by flow.collectAsState()

    var showMarkerDialog by remember { mutableStateOf(false) }
    var customMarkerText by remember { mutableStateOf("") }

    val quickMarkers = listOf("turn", "crossing", "stop_pause", "stairs", "shoe_adjust", "phone_bump")

    val scrollState = rememberScrollState()

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
            RecordingPhase.STILL_FINAL -> "FINAL STILLNESS (${hudState.phaseRemainingSeconds}s)" to StatusLevel.INFO
            RecordingPhase.COMPLETED -> "RECORDING COMPLETE" to StatusLevel.PASS
            RecordingPhase.IDLE -> "INITIALIZING" to StatusLevel.INFO
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

        Spacer(modifier = Modifier.height(20.dp))

        // Large Elapsed Digital Timer
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(
                    width = 2.dp,
                    color = if (hudState.phase == RecordingPhase.BOUT) CyanPrimary else CardBorder,
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
                        text = if (hudState.phase == RecordingPhase.COUNTDOWN) "GET READY" else "HOLD STILL",
                        fontSize = 13.sp,
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

                    Text(
                        text = timerString,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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

        Spacer(modifier = Modifier.height(20.dp))

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
            MetricChip(
                label = "Accel Rate",
                value = if (hudState.accelSamplesReceived > 0) "100 Hz" else "Waiting",
                unit = null,
                valueColor = if (hudState.accelSamplesReceived > 0) GreenSuccess else AmberWarning,
                modifier = Modifier.weight(1f)
            )
            MetricChip(
                label = "Gyro Rate",
                value = if (hudState.gyroSamplesReceived > 0) "100 Hz" else "Waiting",
                unit = null,
                valueColor = if (hudState.gyroSamplesReceived > 0) GreenSuccess else AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricChip(
                label = "GPS Accuracy",
                value = if (hudState.gpsAccuracyMeters > 0) String.format("%.1fm", hudState.gpsAccuracyMeters) else "Fixing...",
                valueColor = if (hudState.gpsAccuracyMeters in 0f..20f) GreenSuccess else AmberWarning,
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
                    service?.stopBoutEarly()
                },
                label = "HOLD 2s TO STOP BOUT"
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
                                    service?.markUserEvent(marker)
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
                                    service?.markUserEvent(customMarkerText.trim())
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
