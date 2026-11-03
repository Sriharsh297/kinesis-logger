package org.kinesis.logger.ui.coverage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.StatusBadge
import org.kinesis.logger.ui.components.StatusLevel
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

data class CoverageItem(
    val code: String,
    val name: String,
    val targetHours: Double,
    val collectedHours: Double
)

@Composable
fun CoverageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val genuineItems = remember {
        listOf(
            CoverageItem("G1", "Walk (slow, normal, fast)", 20.0, 1.2),
            CoverageItem("G2", "Running or jogging", 4.0, 0.4),
            CoverageItem("G3", "Stairs up and down", 2.0, 0.1),
            CoverageItem("G4", "Treadmill walk or run", 3.0, 0.0),
            CoverageItem("G5", "Stop-and-go walking", 3.0, 0.3)
        )
    }

    val benignItems = remember {
        listOf(
            CoverageItem("B1", "Sitting / Standing / Typing", 3.0, 0.5),
            CoverageItem("B2", "Vehicle (Bus, Car, Auto)", 3.0, 0.0),
            CoverageItem("B3", "Cycling", 1.5, 0.0),
            CoverageItem("B4", "Household movement", 1.5, 0.2)
        )
    }

    val fraudItems = remember {
        listOf(
            CoverageItem("F1", "Hand shake, slow", 1.5, 0.3),
            CoverageItem("F2", "Hand shake, fast", 1.5, 0.2),
            CoverageItem("F3", "Pendulum or arm swing", 2.0, 0.4),
            CoverageItem("F4", "Motorised auto-walker", 2.0, 0.0),
            CoverageItem("F5", "Mechanical rotating mount", 1.5, 0.0),
            CoverageItem("F6", "Mock GPS stationary", 2.0, 0.2),
            CoverageItem("F7", "Mock GPS + physical fake", 2.5, 0.3)
        )
    }

    val totalTarget = 55.0
    val totalCollected = 3.6 // Pilot progress
    val overallProgress = (totalCollected / totalTarget).toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "DATASET COVERAGE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "TARGET: 55.0 HOURS (v0.5 MILESTONE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanBright
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overall Progress Hero
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
                    Text(
                        text = "TOTAL DATA COLLECTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$totalCollected / $totalTarget hrs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(Shapes.small),
                    color = CyanPrimary,
                    trackColor = CardBorder
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Rule: Zero wrong cards. Every subject records genuine and fraud data on 2 different days.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Family Breakdown: GENUINE
        CoverageFamilySection(
            familyName = "GENUINE MOVEMENT (32.0 h)",
            badgeColor = GreenSuccess,
            items = genuineItems
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Family Breakdown: BENIGN NON-WALK
        CoverageFamilySection(
            familyName = "BENIGN NON-WALK (9.0 h)",
            badgeColor = BlueAccent,
            items = benignItems
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Family Breakdown: FRAUD
        CoverageFamilySection(
            familyName = "FRAUD ATTACK PACK (13.0 h)",
            badgeColor = RedAlert,
            items = fraudItems
        )
    }
}

@Composable
private fun CoverageFamilySection(
    familyName: String,
    badgeColor: Color,
    items: List<CoverageItem>
) {
    Column {
        Text(
            text = familyName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    val progress = (item.collectedHours / item.targetHours).toFloat().coerceIn(0f, 1f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.code}: ${item.name}",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${item.collectedHours} / ${item.targetHours} h",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(Shapes.small),
                            color = badgeColor,
                            trackColor = CardBorder
                        )
                    }
                }
            }
        }
    }
}
