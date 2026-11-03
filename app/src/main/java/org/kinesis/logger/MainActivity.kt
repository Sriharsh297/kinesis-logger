package org.kinesis.logger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import org.kinesis.logger.ui.dashboard.DashboardScreen
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.KinesisLoggerTheme

class MainActivity : ComponentActivity() {

    private lateinit var taskRepository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskRepository = TaskRepository(applicationContext)

        setContent {
            KinesisLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    KinesisAppNavigation(taskRepository = taskRepository)
                }
            }
        }
    }
}

@Composable
fun KinesisAppNavigation(
    taskRepository: TaskRepository
) {
    val navController = rememberNavController()
    val tasks = remember { taskRepository.getTasks() }
    var currentTaskIndex by remember { mutableIntStateOf(0) }
    var currentSubjectId by remember { mutableStateOf("S01") }

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                tasks = tasks,
                currentTaskIndex = currentTaskIndex,
                onTaskIndexChange = { currentTaskIndex = it },
                currentSubjectId = currentSubjectId,
                onSubjectIdChange = { currentSubjectId = it },
                onNavigateToPreFlight = { navController.navigate("preflight") },
                onNavigateToSessions = { navController.navigate("sessions") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("preflight") {
            // Pre-flight placeholder for navigation
            org.kinesis.logger.ui.preflight.PreFlightPlaceholder(
                task = tasks.getOrNull(currentTaskIndex) ?: tasks.first(),
                subjectId = currentSubjectId,
                onBack = { navController.popBackStack() },
                onStartRecording = { navController.navigate("recording") }
            )
        }
        composable("recording") {
            org.kinesis.logger.ui.recording.RecordingPlaceholder(
                task = tasks.getOrNull(currentTaskIndex) ?: tasks.first(),
                subjectId = currentSubjectId,
                onFinishRecording = { navController.navigate("post_session") }
            )
        }
        composable("post_session") {
            org.kinesis.logger.ui.summary.PostSessionPlaceholder(
                onDone = {
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }
        composable("sessions") {
            org.kinesis.logger.ui.sessions.SessionsPlaceholder(
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            org.kinesis.logger.ui.settings.SettingsPlaceholder(
                onBack = { navController.popBackStack() },
                onNavigateToCoverage = { navController.navigate("coverage") }
            )
        }
        composable("coverage") {
            org.kinesis.logger.ui.coverage.CoveragePlaceholder(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
