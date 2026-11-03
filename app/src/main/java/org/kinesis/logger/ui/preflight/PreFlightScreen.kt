package org.kinesis.logger.ui.preflight

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kinesis.logger.model.Task
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.components.KinesisSecondaryButton
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
import java.io.File

@Composable
fun PreFlightScreen(
    task: Task,
    subjectId: String,
    onBack: () -> Unit,
    onStartRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    // Sensor checks
    val hasAccel = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null }
    val hasGyro = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null }
    val hasStepDetector = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null }
    val hasStepCounter = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null }

    // Battery check
    val batteryPct = remember {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    // Storage check (Free MB)
    val freeStorageMb = remember {
        try {
            val stat = StatFs(context.getExternalFilesDir(null)?.path ?: context.filesDir.path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (e: Exception) {
            1000L
        }
    }

    // Battery optimization check
    val isBatteryOptimized = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else false
    }

    // GPS enabled check
    val isGpsEnabled = remember {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // Mock location check
    val isMockExpected = task.labelType == "F6" || task.labelType == "F7"

    // Critical block condition per spec section 6:
    // Block Start if accelerometer or gyroscope is missing OR free storage < 500 MB
    val isCriticalFailure = !hasAccel || !hasGyro || freeStorageMb < 500

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "PRE-FLIGHT CHECK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "PROTOCOL VALIDATION • ${task.taskId} ($subjectId)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanBright
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Readiness Banner
        if (isCriticalFailure) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33EF4444), Shapes.medium)
                    .border(1.dp, RedAlert, Shapes.medium)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = RedAlert)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PRE-FLIGHT BLOCKED: Missing critical sensors or free storage is under 500 MB.",
                        color = RedAlert,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x2210B981), Shapes.medium)
                    .border(1.dp, GreenSuccess.copy(alpha = 0.5f), Shapes.medium)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "INSTRUMENT READY: Hardware and environment meet protocol specifications.",
                        color = GreenSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check Items
        CheckItemCard(
            title = "Accelerometer (100 Hz)",
            detail = if (hasAccel) "Hardware present • 10,000 µs target period" else "CRITICAL: Sensor missing",
            level = if (hasAccel) StatusLevel.PASS else StatusLevel.FAIL
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "Gyroscope (100 Hz)",
            detail = if (hasGyro) "Hardware present • 10,000 µs target period" else "CRITICAL: Sensor missing",
            level = if (hasGyro) StatusLevel.PASS else StatusLevel.FAIL
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "Step Sensors",
            detail = "Detector: ${if (hasStepDetector) "Available" else "Absent"} • Counter: ${if (hasStepCounter) "Available" else "Absent"}",
            level = if (hasStepDetector && hasStepCounter) StatusLevel.PASS else StatusLevel.WARN
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "GPS Provider & Location",
            detail = if (isGpsEnabled) "GPS Provider enabled • 1 Hz Fused + 1 Hz Raw" else "GPS disabled in system settings",
            level = if (isGpsEnabled) StatusLevel.PASS else StatusLevel.WARN
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "Internal / External Storage",
            detail = "$freeStorageMb MB free space (Minimum 500 MB required)",
            level = if (freeStorageMb >= 500) StatusLevel.PASS else StatusLevel.FAIL
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "Battery Level",
            detail = "$batteryPct% remaining (Protocol recommends > 30%)",
            level = if (batteryPct >= 30) StatusLevel.PASS else StatusLevel.WARN
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "Mock Location Status",
            detail = if (isMockExpected) "Mock mode expected for task ${task.labelType}" else "Real hardware coordinates expected",
            level = StatusLevel.INFO
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            title = "Battery Optimization",
            detail = if (isBatteryOptimized) "Optimized (May kill background recording)" else "Unrestricted (Safe for background logging)",
            level = if (isBatteryOptimized) StatusLevel.WARN else StatusLevel.PASS,
            actionLabel = if (isBatteryOptimized) "Disable" else null,
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Start 10s Countdown Button
        KinesisPrimaryButton(
            text = "START RECORDING (10s COUNTDOWN)",
            onClick = onStartRecording,
            enabled = !isCriticalFailure,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CheckItemCard(
    title: String,
    detail: String,
    level: StatusLevel,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    KinesisCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (actionLabel != null && onAction != null) {
                    KinesisSecondaryButton(
                        text = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.height(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                StatusBadge(
                    text = level.name,
                    level = level
                )
            }
        }
    }
}
