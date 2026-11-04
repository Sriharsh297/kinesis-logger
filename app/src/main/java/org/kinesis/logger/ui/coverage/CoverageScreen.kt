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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // App Bar with 48dp touch targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "DATASET COVERAGE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Target: 55.0 Hours • v0.5 Milestone",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overall Progress Hero
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = CardBorder
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
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "$totalCollected / $totalTarget hrs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(Shapes.small),
                    color = CyanPrimary,
                    trackColor = SurfaceElevated
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Rule: Zero wrong cards. Every subject records genuine and fraud data on 2 different days.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Family Breakdown: GENUINE
        CoverageFamilySection(
            familyName = "GENUINE MOVEMENT (32.0 h)",
            badgeColor = GreenSuccess,
            items = genuineItems
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Family Breakdown: BENIGN NON-WALK
        CoverageFamilySection(
            familyName = "BENIGN NON-WALK (9.0 h)",
            badgeColor = BlueAccent,
            items = benignItems
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Family Breakdown: FRAUD
        CoverageFamilySection(
            familyName = "FRAUD ATTACK PACK (13.0 h)",
            badgeColor = RedAlert,
            items = fraudItems
        )

        Spacer(modifier = Modifier.height(8.dp))
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = CardBorder
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    val progress = (item.collectedHours / item.targetHours).toFloat().coerceIn(0f, 1f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.code}: ${item.name}",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${item.collectedHours} / ${item.targetHours} h",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(Shapes.small),
                            color = badgeColor,
                            trackColor = SurfaceElevated
                        )
                    }
                }
            }
        }
    }
}
