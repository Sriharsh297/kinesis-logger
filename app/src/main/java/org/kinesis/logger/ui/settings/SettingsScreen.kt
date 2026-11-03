package org.kinesis.logger.ui.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.components.KinesisSecondaryButton
import org.kinesis.logger.ui.components.StatusBadge
import org.kinesis.logger.ui.components.StatusLevel
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.CyanBright
import org.kinesis.logger.ui.theme.CyanPrimary
import org.kinesis.logger.ui.theme.SurfaceElevated
import org.kinesis.logger.ui.theme.TextMuted
import org.kinesis.logger.ui.theme.TextPrimary
import org.kinesis.logger.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SETTINGS & INFO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Specs Card
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated
        ) {
            Column {
                Text(
                    text = "DEVICE PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingItemRow("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                SettingItemRow("Model", Build.MODEL)
                SettingItemRow("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                SettingItemRow("Hardware Board", Build.BOARD)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions Card
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated
        ) {
            Column {
                Text(
                    text = "RESEARCH PERMISSIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                PermissionRow("Activity Recognition", "Physical cadence detection", true)
                PermissionRow("Fine & Coarse Location", "GPS provider & accuracy audit", true)
                PermissionRow("Foreground Service", "Location & health continuous logging", true)
                PermissionRow("Wake Lock", "Survives screen-off recording", true)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protocol Info Card
        KinesisCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated
        ) {
            Column {
                Text(
                    text = "PROTOCOL SPECIFICATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingItemRow("Protocol Version", "v0.1 (Capstone-I)")
                SettingItemRow("App Version", "0.1.0")
                SettingItemRow("Data Storage Path", "Android/data/org.kinesis.logger/files/raw/")
                SettingItemRow("Target Clock", "ElapsedRealtimeNanos (Un-resampled)")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Coverage Button
        KinesisPrimaryButton(
            text = "VIEW DATA COVERAGE TRACKER (v0.5)",
            onClick = onNavigateToCoverage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = desc, color = TextMuted, fontSize = 11.sp)
        }
        StatusBadge(
            text = if (granted) "GRANTED" else "DENIED",
            level = if (granted) StatusLevel.PASS else StatusLevel.FAIL
        )
    }
}
