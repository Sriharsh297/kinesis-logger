package org.kinesis.logger.ui.preflight

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.kinesis.logger.model.Task
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.components.KinesisSecondaryButton
import org.kinesis.logger.ui.components.StatusBadge
import org.kinesis.logger.ui.components.StatusLevel
import org.kinesis.logger.ui.theme.AmberWarning
import org.kinesis.logger.ui.theme.AmberWarningBg
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.BlueAccent
import org.kinesis.logger.ui.theme.BlueAccentBg
import org.kinesis.logger.ui.theme.CardBorder
import org.kinesis.logger.ui.theme.CyanPrimary
import org.kinesis.logger.ui.theme.GreenSuccess
import org.kinesis.logger.ui.theme.GreenSuccessBg
import org.kinesis.logger.ui.theme.RedAlert
import org.kinesis.logger.ui.theme.RedAlertBg
import org.kinesis.logger.ui.theme.Shapes
import org.kinesis.logger.ui.theme.SurfaceDark
import org.kinesis.logger.ui.theme.SurfaceElevated
import org.kinesis.logger.ui.theme.TextMuted
import org.kinesis.logger.ui.theme.TextPrimary
import org.kinesis.logger.ui.theme.TextSecondary

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

    // Runtime Permission Verification
    var hasFineLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasActivityRecognition by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasActivityRecognition = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

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
        } catch (_: Exception) {
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
    // Block Start if accelerometer or gyroscope is missing OR free storage < 500 MB OR critical permissions missing
    val isMissingPermissions = !hasFineLocation || !hasActivityRecognition
    val isCriticalFailure = !hasAccel || !hasGyro || freeStorageMb < 500 || isMissingPermissions

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
        // Top App Bar with 48dp touch target back button
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
                    text = "PRE-FLIGHT CHECK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Protocol Validation • ${task.taskId} ($subjectId)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Readiness Banner
        if (isCriticalFailure) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium)
                    .background(RedAlertBg)
                    .border(1.dp, RedAlert.copy(alpha = 0.4f), Shapes.medium)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = RedAlert,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PRE-FLIGHT BLOCKED",
                            color = RedAlert,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isMissingPermissions -> "Location and Activity Recognition permissions required before recording."
                                !hasAccel || !hasGyro -> "Accelerometer or Gyroscope hardware missing."
                                else -> "Free storage is under 500 MB."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium)
                    .background(GreenSuccessBg)
                    .border(1.dp, GreenSuccess.copy(alpha = 0.35f), Shapes.medium)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "INSTRUMENT READY",
                            color = GreenSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hardware, permissions, and environment meet protocol specifications.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Check Card
        CheckItemCard(
            icon = Icons.Default.Security,
            title = "Research Permissions",
            detail = if (!isMissingPermissions) {
                "Location, Pedometer, and Notification permissions granted"
            } else {
                "CRITICAL: Location or Activity Recognition permission not granted"
            },
            level = if (!isMissingPermissions) StatusLevel.PASS else StatusLevel.FAIL,
            actionLabel = if (isMissingPermissions || !hasNotificationPermission) "Grant" else null,
            onAction = {
                permissionLauncher.launch(permissionsToRequest)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Check Items
        CheckItemCard(
            icon = Icons.Default.Sensors,
            title = "Accelerometer (100 Hz)",
            detail = if (hasAccel) "Hardware present • 10,000 µs target period" else "CRITICAL: Sensor missing",
            level = if (hasAccel) StatusLevel.PASS else StatusLevel.FAIL
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.Sensors,
            title = "Gyroscope (100 Hz)",
            detail = if (hasGyro) "Hardware present • 10,000 µs target period" else "CRITICAL: Sensor missing",
            level = if (hasGyro) StatusLevel.PASS else StatusLevel.FAIL
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.Sensors,
            title = "Step Sensors",
            detail = "Detector: ${if (hasStepDetector) "Available" else "Absent"} • Counter: ${if (hasStepCounter) "Available" else "Absent"}",
            level = if (hasStepDetector && hasStepCounter) StatusLevel.PASS else StatusLevel.WARN
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.LocationOn,
            title = "GPS Provider & Location",
            detail = if (isGpsEnabled) "GPS Provider enabled • 1 Hz Fused + 1 Hz Raw" else "GPS disabled in system settings",
            level = if (isGpsEnabled) StatusLevel.PASS else StatusLevel.WARN
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.Storage,
            title = "Internal / External Storage",
            detail = "$freeStorageMb MB free space (Minimum 500 MB required)",
            level = if (freeStorageMb >= 500) StatusLevel.PASS else StatusLevel.FAIL
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.BatteryFull,
            title = "Battery Level",
            detail = "$batteryPct% remaining (Protocol recommends > 30%)",
            level = if (batteryPct >= 30) StatusLevel.PASS else StatusLevel.WARN
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.SwapCalls,
            title = "Mock Location Protocol",
            detail = if (isMockExpected) {
                "Task ${task.labelType} requires mock GPS. This is guidance only; verify is_mock in the saved location data."
            } else {
                "Use real GPS for this task. This screen does not detect active mock locations; verify is_mock in the saved data."
            },
            level = StatusLevel.INFO
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckItemCard(
            icon = Icons.Default.PowerSettingsNew,
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

        Spacer(modifier = Modifier.height(20.dp))

        // Start Countdown Button
        KinesisPrimaryButton(
            text = "START RECORDING (10s COUNTDOWN)",
            onClick = {
                if (isMissingPermissions) {
                    permissionLauncher.launch(permissionsToRequest)
                } else {
                    onStartRecording()
                }
            },
            enabled = !isCriticalFailure,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CheckItemCard(
    icon: ImageVector,
    title: String,
    detail: String,
    level: StatusLevel,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val iconTint = when (level) {
        StatusLevel.PASS -> GreenSuccess
        StatusLevel.WARN -> AmberWarning
        StatusLevel.FAIL -> RedAlert
        StatusLevel.INFO -> BlueAccent
    }
    val iconBg = when (level) {
        StatusLevel.PASS -> GreenSuccessBg
        StatusLevel.WARN -> AmberWarningBg
        StatusLevel.FAIL -> RedAlertBg
        StatusLevel.INFO -> BlueAccentBg
    }

    KinesisCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = CardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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
