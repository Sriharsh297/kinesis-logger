package org.kinesis.logger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.kinesis.logger.data.TaskRepository
import org.kinesis.logger.service.CrashRecoveryManager
import org.kinesis.logger.service.RecordingService
import org.kinesis.logger.ui.coverage.CoverageScreen
import org.kinesis.logger.ui.dashboard.DashboardScreen
import org.kinesis.logger.ui.preflight.PreFlightScreen
import org.kinesis.logger.ui.recording.RecordingScreen
import org.kinesis.logger.ui.sessions.SessionsScreen
import org.kinesis.logger.ui.settings.SettingsScreen
import org.kinesis.logger.ui.summary.PostSessionScreen
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.KinesisLoggerTheme

class MainActivity : ComponentActivity() {

    private lateinit var taskRepository: TaskRepository
    private lateinit var crashRecoveryManager: CrashRecoveryManager

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Evaluated dynamically in PreFlightScreen
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable modern edge-to-edge rendering
        enableEdgeToEdge()

        taskRepository = TaskRepository(applicationContext)
        crashRecoveryManager = CrashRecoveryManager(applicationContext)

        // Crash safety check on app launch per spec section 8
        val recoveredSessionId =
            crashRecoveryManager.checkAndRecoverInterruptedSession()

        if (recoveredSessionId != null) {
            Toast.makeText(
                this,
                "Interrupted session recovered: $recoveredSessionId",
                Toast.LENGTH_LONG
            ).show()
        }

        // Request missing critical permissions on first launch
        val missingPermissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }

        setContent {
            KinesisLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    KinesisAppNavigation(
                        taskRepository = taskRepository,
                        onStartService = { taskId, subjectId, plannedDurationS ->
                            RecordingService.start(
                                this,
                                taskId,
                                subjectId,
                                plannedDurationS
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun KinesisAppNavigation(
    taskRepository: TaskRepository,
    onStartService: (
        taskId: String,
        subjectId: String,
        plannedDurationS: Int
    ) -> Unit
) {

    val navController = rememberNavController()

    val tasks = remember {
        taskRepository.getTasks()
    }

    var currentTaskIndex by remember {
        mutableIntStateOf(0)
    }

    var currentSubjectId by remember {
        mutableStateOf("S01")
    }

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {

        // Dashboard
        composable("dashboard") {
            DashboardScreen(
                tasks = tasks,
                currentTaskIndex = currentTaskIndex,
                onTaskIndexChange = {
                    currentTaskIndex = it
                },
                currentSubjectId = currentSubjectId,
                onSubjectIdChange = {
                    currentSubjectId = it
                },
                onNavigateToPreFlight = {
                    navController.navigate("preflight")
                },
                onNavigateToSessions = {
                    navController.navigate("sessions")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // Pre-flight
        composable("preflight") {

            val task = tasks.getOrNull(currentTaskIndex)
                ?: tasks.first()

            PreFlightScreen(
                task = task,
                subjectId = currentSubjectId,
                onBack = {
                    navController.popBackStack()
                },
                onStartRecording = {

                    onStartService(
                        task.taskId,
                        currentSubjectId,
                        task.boutPlannedS
                    )

                    navController.navigate("recording")
                }
            )
        }

        // Live recording
        composable("recording") {

            val task = tasks.getOrNull(currentTaskIndex)
                ?: tasks.first()

            RecordingScreen(
                task = task,
                subjectId = currentSubjectId,
                onFinishRecording = { sessionId ->
                    navController.navigate("post_session")
                }
            )
        }

        // Post-session summary
        composable("post_session") {

            val task = tasks.getOrNull(currentTaskIndex)
                ?: tasks.first()

            PostSessionScreen(
                task = task,
                subjectId = currentSubjectId,
                onDone = {
                    navController.popBackStack(
                        "dashboard",
                        inclusive = false
                    )
                }
            )
        }

        // Recorded sessions
        composable("sessions") {
            SessionsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToCoverage = {
                    navController.navigate("coverage")
                }
            )
        }

        // Dataset coverage
        composable("coverage") {
            CoverageScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}