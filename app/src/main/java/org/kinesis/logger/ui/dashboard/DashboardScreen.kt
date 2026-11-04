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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
        mutableListOf("S01", "S02", "S03", "S04", "S05", "S06")
    }

    val currentTask = tasks.getOrNull(currentTaskIndex) ?: tasks.firstOrNull()

    val phoneModel = remember {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        "$manufacturer $model (Android ${Build.VERSION.RELEASE})"
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
        // App Bar / Clean Branding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KINESIS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOGGER",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Research Instrument • Protocol v0.1",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }

            // Top action buttons with accessible 48dp touch targets
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onNavigateToSessions,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Sessions",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject & Device Card
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Subject ID:",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(Shapes.small)
                                    .background(SurfaceElevated)
                                    .border(1.dp, CardBorder, Shapes.small)
                                    .clickable { subjectDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "$currentSubjectId  ▼",
                                    color = CyanPrimary,
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
                                        text = { Text(subId, fontWeight = FontWeight.SemiBold) },
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
                        modifier = Modifier.height(44.dp)
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
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = phoneModel,
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Task Card
        if (currentTask != null) {
            val (familyBadgeText, familyLevel) = when (currentTask.labelFamily) {
                "GENUINE_MOVEMENT" -> "GENUINE MOVEMENT" to StatusLevel.PASS
                "BENIGN_NON_WALK" -> "BENIGN NON-WALK" to StatusLevel.INFO
                "FRAUD" -> "FRAUD ATTACK" to StatusLevel.FAIL
                else -> currentTask.labelFamily to StatusLevel.WARN
            }

            KinesisCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = CardBorder
            ) {
                Column {
                    // Category Badge and Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = familyBadgeText,
                            level = familyLevel
                        )

                        Text(
                            text = "${currentTaskIndex + 1} of ${tasks.size}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Task ID + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentTask.taskId,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                        Text(
                            text = "—",
                            fontSize = 16.sp,
                            color = TextMuted
                        )
                        Text(
                            text = currentTask.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentTask.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            .border(1.dp, CardBorder, Shapes.small)
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
                                .background(AmberWarning.copy(alpha = 0.15f))
                                .border(1.dp, AmberWarning.copy(alpha = 0.35f), Shapes.small)
                                .padding(12.dp),
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

                    // Prev / Next Navigation Controls with 48dp minimum height
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KinesisSecondaryButton(
                            text = "◀ Previous",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Pre-Flight Check Action Button with restrained cyan-to-blue gradient
        KinesisPrimaryButton(
            text = "PROCEED TO PRE-FLIGHT CHECK",
            onClick = onNavigateToPreFlight,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
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
                            onClick = { showNewSubjectDialog = false },
                            modifier = Modifier.height(44.dp)
                        ) {
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
                            },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Add", color = CyanPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
