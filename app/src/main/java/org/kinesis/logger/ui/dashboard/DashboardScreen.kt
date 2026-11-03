package org.kinesis.logger.ui.dashboard

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.kinesis.logger.model.Task
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.components.KinesisSecondaryButton
import org.kinesis.logger.ui.components.MetricChip
import org.kinesis.logger.ui.components.StatusBadge
import org.kinesis.logger.ui.components.StatusLevel
import org.kinesis.logger.ui.theme.AmberWarning
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.BlueAccent
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

@Composable
fun DashboardScreen(
    tasks: List<Task>,
    currentTaskIndex: Int,
    onTaskIndexChange: (Int) -> Unit,
    currentSubjectId: String,
    onSubjectIdChange: (String) -> Unit,
    onNavigateToPreFlight: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var showNewSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    val subjects = remember {
        mutableListOf("S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S10", "S11", "S12")
    }

    val currentTask = tasks.getOrNull(currentTaskIndex) ?: tasks.firstOrNull()

    val phoneModel = remember {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        "$manufacturer $model (API ${Build.VERSION.SDK_INT})"
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // App Bar / Branding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KINESIS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOGGER",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "RESEARCH INSTRUMENT v0.1",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
            }

            Row {
                IconButton(onClick = onNavigateToSessions) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Sessions",
                        tint = CyanBright
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device & Subject Card
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Subject:",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(Shapes.small)
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, Shapes.small)
                                    .clickable { subjectDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$currentSubjectId ▼",
                                    color = CyanBright,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = subjectDropdownExpanded,
                                onDismissRequest = { subjectDropdownExpanded = false }
                            ) {
                                subjects.forEach { subId ->
                                    DropdownMenuItem(
                                        text = { Text(subId) },
                                        onClick = {
                                            onSubjectIdChange(subId)
                                            subjectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showNewSubjectDialog = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Subject",
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New ID",
                            color = CyanPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = phoneModel,
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Task Card
        if (currentTask != null) {
            val familyLevel = when (currentTask.labelFamily) {
                "GENUINE_MOVEMENT" -> StatusLevel.PASS
                "BENIGN_NON_WALK" -> StatusLevel.INFO
                "FRAUD" -> StatusLevel.FAIL
                else -> StatusLevel.WARN
            }

            KinesisCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = when (currentTask.labelFamily) {
                    "FRAUD" -> RedAlert.copy(alpha = 0.5f)
                    "GENUINE_MOVEMENT" -> CyanPrimary.copy(alpha = 0.4f)
                    else -> CardBorder
                }
            ) {
                Column {
                    // Task ID and Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentTask.taskId,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(
                                text = currentTask.labelFamily.replace("_", " "),
                                level = familyLevel
                            )
                        }

                        Text(
                            text = "(${currentTaskIndex + 1} of ${tasks.size})",
                            fontSize = 13.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentTask.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentTask.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricChip(
                            label = "Placement",
                            value = currentTask.placement,
                            modifier = Modifier.weight(1f)
                        )
                        MetricChip(
                            label = "Speed",
                            value = currentTask.speedLabel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricChip(
                            label = "Environment",
                            value = currentTask.environment,
                            modifier = Modifier.weight(1f)
                        )
                        MetricChip(
                            label = "Duration",
                            value = "${currentTask.boutPlannedS / 60}m ${currentTask.boutPlannedS % 60}s",
                            modifier = Modifier.weight(1f),
                            valueColor = CyanBright
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Orientation instruction callout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.small)
                            .background(SurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "ORIENTATION & POSE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentTask.orientationInstruction,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (currentTask.boutType == "counted") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.small)
                                .background(Color(0x22F59E0B))
                                .border(1.dp, AmberWarning.copy(alpha = 0.4f), Shapes.small)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠ COUNTED BOUT: Exactly ${currentTask.targetSteps ?: 100} steps. Must be counted by partner or video.",
                                color = AmberWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Prev / Next Navigation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KinesisSecondaryButton(
                            text = "◀ Prev",
                            onClick = {
                                if (currentTaskIndex > 0) {
                                    onTaskIndexChange(currentTaskIndex - 1)
                                }
                            },
                            enabled = currentTaskIndex > 0,
                            modifier = Modifier.weight(1f)
                        )

                        KinesisSecondaryButton(
                            text = "Next ▶",
                            onClick = {
                                if (currentTaskIndex < tasks.size - 1) {
                                    onTaskIndexChange(currentTaskIndex + 1)
                                }
                            },
                            enabled = currentTaskIndex < tasks.size - 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pre-Flight Check Action Button
        KinesisPrimaryButton(
            text = "PROCEED TO PRE-FLIGHT CHECK",
            onClick = onNavigateToPreFlight,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // New Subject Dialog
    if (showNewSubjectDialog) {
        Dialog(onDismissRequest = { showNewSubjectDialog = false }) {
            KinesisCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark
            ) {
                Column {
                    Text(
                        text = "Add New Subject ID",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newSubjectInput,
                        onValueChange = { newSubjectInput = it.uppercase() },
                        label = { Text("Pseudonymous ID (e.g. S13)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewSubjectDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (newSubjectInput.isNotBlank()) {
                                    if (!subjects.contains(newSubjectInput)) {
                                        subjects.add(newSubjectInput)
                                    }
                                    onSubjectIdChange(newSubjectInput)
                                    newSubjectInput = ""
                                    showNewSubjectDialog = false
                                }
                            }
                        ) {
                            Text("Add", color = CyanPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
