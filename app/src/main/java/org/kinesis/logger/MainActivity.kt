package org.kinesis.logger

import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

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

        setContent {
            KinesisLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    KinesisAppNavigation(
                        taskRepository = taskRepository,
                        onStartService = { taskId, subjectId, plannedDurationS ->

                            val intent = Intent(
                                this,
                                RecordingService::class.java
                            ).apply {
                                action = RecordingService.ACTION_START

                                putExtra(
                                    RecordingService.EXTRA_TASK_ID,
                                    taskId
                                )

                                putExtra(
                                    RecordingService.EXTRA_SUBJECT_ID,
                                    subjectId
                                )

                                putExtra(
                                    RecordingService.EXTRA_PLANNED_DURATION_S,
                                    plannedDurationS
                                )
                            }

                            startService(intent)
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