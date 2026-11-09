package org.kinesis.logger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kinesis.logger.MainActivity
import org.kinesis.logger.R
import org.kinesis.logger.data.TaskRepository
import org.kinesis.logger.quality.SessionFinalizer
import org.kinesis.logger.recorder.SensorRecorder
import org.kinesis.logger.recorder.SessionWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RecordingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var sequenceJob: Job? = null

    private lateinit var wakeLock: PowerManager.WakeLock
    private var vibrator: Vibrator? = null

    var sessionWriter: SessionWriter? = null
        private set
    var sensorRecorder: SensorRecorder? = null
        private set

    private lateinit var crashRecoveryManager: CrashRecoveryManager
    private lateinit var taskRepository: TaskRepository

    private var currentSessionId: String = ""
    private var currentSessionDir: File? = null
    private var currentTaskId: String = "T01"
    private var currentSubjectId: String = "S01"
    private var plannedDurationSeconds: Int = 480

    companion object {
        private const val TAG = "RecordingService"
        const val CHANNEL_ID = "kinesis_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "org.kinesis.logger.action.START"
        const val ACTION_STOP_BOUT = "org.kinesis.logger.action.STOP_BOUT"
        const val ACTION_MARK_EVENT = "org.kinesis.logger.action.MARK_EVENT"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_PLANNED_DURATION_S = "extra_planned_duration_s"
        const val EXTRA_MARKER_NOTE = "extra_marker_note"

        // Singleton StateFlow observed by UI components
        private val _hudState = MutableStateFlow(LiveRecordingHud())
        val hudState: StateFlow<LiveRecordingHud> = _hudState.asStateFlow()

        var instance: RecordingService? = null
            private set

        fun start(context: Context, taskId: String, subjectId: String, plannedDurationS: Int) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_SUBJECT_ID, subjectId)
                putExtra(EXTRA_PLANNED_DURATION_S, plannedDurationS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopBoutEarly(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_BOUT
            }
            context.startService(intent)
        }

        fun markEvent(context: Context, note: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_MARK_EVENT
                putExtra(EXTRA_MARKER_NOTE, note)
            }
            context.startService(intent)
        }

        fun updateHudGps(accuracyMeters: Float, provider: String) {
            _hudState.value = _hudState.value.copy(
                gpsAccuracyMeters = accuracyMeters,
                gpsFixCount = _hudState.value.gpsFixCount + 1,
                lastSensorUpdateMs = System.currentTimeMillis()
            )
        }

        fun resetHud() {
            _hudState.value = LiveRecordingHud()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    sensorRecorder?.logEvent("screen_off")
                    _hudState.value = _hudState.value.copy(isScreenOff = true)
                }
                Intent.ACTION_SCREEN_ON -> {
                    sensorRecorder?.logEvent("screen_on")
                    _hudState.value = _hudState.value.copy(isScreenOff = false)
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        crashRecoveryManager = CrashRecoveryManager(this)
        taskRepository = TaskRepository(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "kinesis:recording_wakelock"
        ).apply {
            setReferenceCounted(false)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: "T01"
                val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: "S01"
                val plannedDuration = intent.getIntExtra(EXTRA_PLANNED_DURATION_S, 480)
                initAndStartRecording(taskId, subjectId, plannedDuration)
            }
            ACTION_STOP_BOUT -> {
                stopBoutInternal()
            }
            ACTION_MARK_EVENT -> {
                val note = intent.getStringExtra(EXTRA_MARKER_NOTE) ?: "user_marker"
                markUserEvent(note)
            }
        }
        return START_NOT_STICKY
    }

    private fun initAndStartRecording(taskId: String, subjectId: String, plannedS: Int) {
        currentTaskId = taskId
        currentSubjectId = subjectId
        plannedDurationSeconds = plannedS

        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        val shortUuid = UUID.randomUUID().toString().substring(0, 6)
        currentSessionId = "KIN-$timestamp-$subjectId-$taskId-$shortUuid"

        val baseDir = getExternalFilesDir(null) ?: filesDir
        val rawDir = File(baseDir, "raw")
        val sessionDir = File(rawDir, currentSessionId)
        currentSessionDir = sessionDir

        val writer = SessionWriter(sessionDir)
        sessionWriter = writer
        sensorRecorder = SensorRecorder(this, writer)

        crashRecoveryManager.markSessionActive(currentSessionId, sessionDir.absolutePath)

        if (!wakeLock.isHeld) {
            wakeLock.acquire(12 * 60 * 60 * 1000L) // 12 hours max safety
        }

        val notification = buildNotification("Starting protocol sequence...", taskId, subjectId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _hudState.value = LiveRecordingHud(
            phase = RecordingPhase.IDLE,
            sessionId = currentSessionId,
            plannedBoutSeconds = plannedS
        )

        sensorRecorder?.startRecording()
        runProtocolSequence(plannedS)
    }

    private fun runProtocolSequence(plannedS: Int) {
        sequenceJob?.cancel()
        sequenceJob = serviceScope.launch {
            // Phase 1: 10s Countdown
            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.COUNTDOWN,
                phaseRemainingSeconds = 10,
                plannedBoutSeconds = plannedS,
                sessionId = currentSessionId
            )

            for (sec in 10 downTo 1) {
                if (_hudState.value.phase != RecordingPhase.COUNTDOWN) break
                _hudState.value = _hudState.value.copy(phaseRemainingSeconds = sec)
                if (sec in 1..3) {
                    vibrateTick()
                }
                updateNotification("Position phone... Countdown: ${sec}s")
                delay(1000L)
            }
            vibrateCue()
            sensorRecorder?.logEvent("countdown_end")

            // Phase 2: 5s Initial Stillness
            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.STILL_INITIAL,
                phaseRemainingSeconds = 5
            )
            sensorRecorder?.logEvent("still_start", "phase=initial")
            for (sec in 5 downTo 1) {
                if (_hudState.value.phase != RecordingPhase.STILL_INITIAL) break
                _hudState.value = _hudState.value.copy(phaseRemainingSeconds = sec)
                updateNotification("Stand still: ${sec}s")
                delay(1000L)
            }
            sensorRecorder?.logEvent("still_end", "phase=initial")
            vibrateCue()

            // Phase 3: Active Bout
            val boutStartMonoMs = SystemClock.elapsedRealtime()
            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.BOUT,
                boutElapsedSeconds = 0,
                phaseRemainingSeconds = plannedS
            )
            sensorRecorder?.logEvent("bout_start", "planned_s=$plannedS")

            var lastSampleCountTime = boutStartMonoMs
            var lastAccelCount = 0L
            var lastGyroCount = 0L
            var elapsedSec = 0

            while (elapsedSec < plannedS && _hudState.value.phase == RecordingPhase.BOUT) {
                delay(500L)
                val nowMono = SystemClock.elapsedRealtime()
                elapsedSec = ((nowMono - boutStartMonoMs) / 1000L).toInt()

                val currentAccel = sessionWriter?.accelCount?.get() ?: 0L
                val currentGyro = sessionWriter?.gyroCount?.get() ?: 0L
                val timeDeltaSec = (nowMono - lastSampleCountTime) / 1000.0

                val liveAccelHz = if (timeDeltaSec >= 0.5) {
                    ((currentAccel - lastAccelCount) / timeDeltaSec).toFloat()
                } else _hudState.value.accelLiveHz

                val liveGyroHz = if (timeDeltaSec >= 0.5) {
                    ((currentGyro - lastGyroCount) / timeDeltaSec).toFloat()
                } else _hudState.value.gyroLiveHz

                if (timeDeltaSec >= 0.5) {
                    lastAccelCount = currentAccel
                    lastGyroCount = currentGyro
                    lastSampleCountTime = nowMono
                }

                _hudState.value = _hudState.value.copy(
                    boutElapsedSeconds = elapsedSec,
                    phaseRemainingSeconds = (plannedS - elapsedSec).coerceAtLeast(0),
                    accelSamplesReceived = currentAccel,
                    gyroSamplesReceived = currentGyro,
                    accelLiveHz = liveAccelHz,
                    gyroLiveHz = liveGyroHz,
                    osStepsReference = sensorRecorder?.lastOsStepCumulative?.get() ?: 0L,
                    lastSensorUpdateMs = sensorRecorder?.lastSensorEventMs?.get() ?: System.currentTimeMillis()
                )

                val elapsedStr = String.format("%02d:%02d / %02d:%02d", elapsedSec / 60, elapsedSec % 60, plannedS / 60, plannedS % 60)
                updateNotification("Recording Bout: $elapsedStr")
            }

            // Phase 4: Final 5s Stillness
            sensorRecorder?.logEvent("bout_end", "elapsed_s=$elapsedSec")
            vibrateCue()

            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.STILL_FINAL,
                phaseRemainingSeconds = 5,
                isStopping = true
            )
            sensorRecorder?.logEvent("still_start", "phase=final")
            for (sec in 5 downTo 1) {
                _hudState.value = _hudState.value.copy(phaseRemainingSeconds = sec)
                updateNotification("Final stillness: ${sec}s")
                delay(1000L)
            }
            sensorRecorder?.logEvent("still_end", "phase=final")

            // Terminal vibration
            vibrateTerminal()

            // Phase 5: Flush, Compress, and Finalize
            updateNotification("Finalizing data and checksums...")
            finalizeActiveSession()

            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.COMPLETED,
                isStopping = false
            )
            updateNotification("Session finished. Review summary.")
        }
    }

    private fun stopBoutInternal() {
        val currentPhase = _hudState.value.phase
        when (currentPhase) {
            RecordingPhase.BOUT -> {
                // Signal transition to final stillness
                _hudState.value = _hudState.value.copy(phase = RecordingPhase.STILL_FINAL, isStopping = true)
            }
            RecordingPhase.COUNTDOWN, RecordingPhase.STILL_INITIAL -> {
                // User aborted before bout started: finalize immediately
                sequenceJob?.cancel()
                finalizeActiveSession()
                _hudState.value = _hudState.value.copy(phase = RecordingPhase.COMPLETED, isStopping = false)
            }
            else -> {}
        }
    }

    private fun finalizeActiveSession() {
        try {
            sensorRecorder?.stopRecording()
            sessionWriter?.stopAndClose()

            val dir = currentSessionDir
            if (dir != null && dir.exists()) {
                val task = taskRepository.getTasks().firstOrNull { it.taskId == currentTaskId }
                    ?: taskRepository.getTasks().first()

                SessionFinalizer.finalizeSession(
                    context = applicationContext,
                    sessionDir = dir,
                    task = task,
                    subjectId = currentSubjectId,
                    sensorRecorder = sensorRecorder
                )

                crashRecoveryManager.markSessionClosed(currentSessionId)
                Log.i(TAG, "Successfully finalized session: $currentSessionId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing session: ${e.message}", e)
        }
    }

    fun markUserEvent(note: String) {
        sensorRecorder?.logEvent("user_marker", note)
        val elapsed = _hudState.value.boutElapsedSeconds
        _hudState.value = _hudState.value.copy(
            lastMarkerNote = note,
            lastMarkerTimeSec = elapsed
        )
    }

    private fun vibrateTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateCue() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(350)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateTerminal() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 300, 150, 600)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(700)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kinesis Data Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live recording status for sensor data collection"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String, taskId: String = "", subjectId: String = ""): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kinesis Logger: $taskId ($subjectId)")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status, currentTaskId, currentSubjectId))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        sequenceJob?.cancel()

        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        sensorRecorder?.stopRecording()
    }
}
