package org.kinesis.logger.ui.summary

import android.content.Context
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kinesis.logger.KinesisApplication
import org.kinesis.logger.model.Task
import org.kinesis.logger.quality.ManifestGenerator
import org.kinesis.logger.quality.QualityCalculator
import org.kinesis.logger.quality.QualityReport
import org.kinesis.logger.quality.SessionJsonBuilder
import org.kinesis.logger.quality.SessionSaveParams
import org.kinesis.logger.service.RecordingService
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisDangerButton
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.components.MetricChip
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
import java.io.File
import java.util.UUID

@Composable
fun PostSessionScreen(
    task: Task,
    subjectId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val service = RecordingService.instance

    val sessionWriter = service?.sessionWriter
    val sessionDir = sessionWriter?.sessionDir ?: run {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        File(File(baseDir, "raw"), "latest")
    }

    var qualityReport by remember {
        mutableStateOf(
            QualityReport(
                durationS = 0.0,
                accelSamples = 0,
                gyroSamples = 0,
                stepDetectorEvents = 0,
                stepCounterDelta = 0,
                locationSamples = 0,
                accelAchievedRateHz = 0.0,
                gyroAchievedRateHz = 0.0,
                accelGapsOver100ms = 0,
                gyroGapsOver100ms = 0,
                accelP99GapMs = 0.0,
                gyroP99GapMs = 0.0,
                gpsFixPercent = 0.0,
                gpsMedianAccuracyM = -1f,
                mockFlagPercent = 0.0,
                screenOffSeconds = 0.0
            )
        )
    }

    var isLoadingQuality by remember { mutableStateOf(true) }
    var sessionOk by remember { mutableStateOf(true) }
    var discardReason by remember { mutableStateOf("phone fell") }
    var discardDropdownExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var manualStepCountText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val discardReasons = listOf(
        "phone fell",
        "phone call / app interrupted",
        "stopped early",
        "wrong placement / orientation",
        "operator error",
        "hardware defect"
    )

    // Calculate quality on background dispatcher
    LaunchedEffect(sessionDir) {
        withContext(Dispatchers.IO) {
            if (sessionDir.exists()) {
                qualityReport = QualityCalculator.calculate(sessionDir)
            }
            isLoadingQuality = false
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "SESSION SUMMARY",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sessionDir.name,
                fontSize = 12.sp,
                color = CyanPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quality Metrics Grid
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = CardBorder
        ) {
            Column {
                Text(
                    text = "DATA QUALITY AUDIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricChip(
                        label = "Duration",
                        value = "${qualityReport.durationS.toInt()}s",
                        modifier = Modifier.weight(1f),
                        valueColor = CyanBright
                    )
                    MetricChip(
                        label = "Gaps > 100ms",
                        value = "${qualityReport.accelGapsOver100ms}",
                        modifier = Modifier.weight(1f),
                        valueColor = if (qualityReport.accelGapsOver100ms == 0) GreenSuccess else AmberWarning
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricChip(
                        label = "Accel Rate",
                        value = "${qualityReport.accelAchievedRateHz}",
                        unit = "Hz",
                        modifier = Modifier.weight(1f),
                        valueColor = if (qualityReport.accelAchievedRateHz in 95.0..105.0) GreenSuccess else AmberWarning
                    )
                    MetricChip(
                        label = "Gyro Rate",
                        value = "${qualityReport.gyroAchievedRateHz}",
                        unit = "Hz",
                        modifier = Modifier.weight(1f),
                        valueColor = if (qualityReport.gyroAchievedRateHz in 95.0..105.0) GreenSuccess else AmberWarning
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricChip(
                        label = "GPS Fix Pct",
                        value = "${qualityReport.gpsFixPercent}",
                        unit = "%",
                        modifier = Modifier.weight(1f),
                        valueColor = if (qualityReport.gpsFixPercent >= 90.0) GreenSuccess else AmberWarning
                    )
                    MetricChip(
                        label = "OS Step Delta",
                        value = "${qualityReport.stepCounterDelta}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Counted Bout Manual Step Input (Rule 5 & Wireframe D)
        if (task.boutType == "counted") {
            KinesisCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = CyanPrimary
            ) {
                Column {
                    Text(
                        text = "MANUAL STEP AUDIT (COUNTED BOUT)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target was ${task.targetSteps ?: 100} steps. Enter manually counted steps by partner/video.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualStepCountText,
                        onValueChange = { manualStepCountText = it },
                        label = { Text("Actual steps walked") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Session OK Question
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = CardBorder
        ) {
            Column {
                Text(
                    text = "Was this session performed as written?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sessionOk = true }
                    ) {
                        RadioButton(
                            selected = sessionOk,
                            onClick = { sessionOk = true },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yes (Session OK)", color = TextPrimary, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sessionOk = false }
                    ) {
                        RadioButton(
                            selected = !sessionOk,
                            onClick = { sessionOk = false },
                            colors = RadioButtonDefaults.colors(selectedColor = RedAlert)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No (Discard)", color = TextPrimary, fontSize = 14.sp)
                    }
                }

                if (!sessionOk) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Discard Reason:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RedAlert
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.small)
                                .background(SurfaceElevated)
                                .border(1.dp, RedAlert.copy(alpha = 0.4f), Shapes.small)
                                .clickable { discardDropdownExpanded = true }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "$discardReason ▼",
                                color = RedAlert,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        DropdownMenu(
                            expanded = discardDropdownExpanded,
                            onDismissRequest = { discardDropdownExpanded = false }
                        ) {
                            discardReasons.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason) },
                                    onClick = {
                                        discardReason = reason
                                        discardDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Session Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Operator Notes (optional)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyanPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Save & Export Action
        KinesisPrimaryButton(
            text = if (isSaving) "SAVING & VERIFYING..." else "SAVE SESSION & COMPLETE",
            onClick = {
                isSaving = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val sessionWriter = service?.sessionWriter
                        val sensorRecorder = service?.sensorRecorder

                        // Compress and verify CSV files
                        sessionWriter?.compressAndVerify()

                        // Calculate final hashes
                        val hashes = ManifestGenerator.generateManifest(sessionDir)

                        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                        val endBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                        val params = SessionSaveParams(
                            sessionId = sessionDir.name,
                            subjectId = subjectId,
                            task = task,
                            startUtcMs = sensorRecorder?.startUtcMs ?: System.currentTimeMillis(),
                            startElapsedNs = sensorRecorder?.startElapsedNs ?: System.nanoTime(),
                            bootOffsetMs = sensorRecorder?.bootOffsetMs ?: 0L,
                            sessionOk = sessionOk,
                            discardReason = if (!sessionOk) discardReason else null,
                            notes = notes,
                            manualStepCount = manualStepCountText.toIntOrNull(),
                            countMethod = if (task.boutType == "counted") "partner" else null,
                            batteryStartPct = 80,
                            batteryEndPct = endBattery,
                            installId = UUID.randomUUID().toString().substring(0, 8)
                        )

                        val sessionJson = SessionJsonBuilder.build(params, qualityReport, hashes)
                        File(sessionDir, "session.json").writeText(sessionJson.toString(2))

                        // Refresh manifest including session.json
                        ManifestGenerator.generateManifest(sessionDir)

                        // Enqueue Wi-Fi upload (non-blocking, background coroutine)
                        val app = context.applicationContext as KinesisApplication
                        app.uploadQueue.enqueue(sessionDir)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Session saved — uploading on Wi-Fi", Toast.LENGTH_SHORT).show()
                            onDone()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                            isSaving = false
                        }
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Discard Button
        KinesisDangerButton(
            text = "DISCARD AND DELETE FILES",
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    sessionDir.deleteRecursively()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Session discarded", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
