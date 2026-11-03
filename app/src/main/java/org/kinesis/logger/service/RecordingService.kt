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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    private val _hudState = MutableStateFlow(LiveRecordingHud())
    val hudState: StateFlow<LiveRecordingHud> = _hudState.asStateFlow()

    private var currentSessionId: String = ""
    private var currentSessionDir: File? = null
    private var plannedDurationSeconds: Int = 480

    companion object {
        const val CHANNEL_ID = "kinesis_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "org.kinesis.logger.action.START"
        const val ACTION_STOP_BOUT = "org.kinesis.logger.action.STOP_BOUT"
        const val ACTION_MARK_EVENT = "org.kinesis.logger.action.MARK_EVENT"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_PLANNED_DURATION_S = "extra_planned_duration_s"
        const val EXTRA_MARKER_NOTE = "extra_marker_note"

        // Singleton instance accessible by UI
        var instance: RecordingService? = null
            private set
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
                plannedDurationSeconds = intent.getIntExtra(EXTRA_PLANNED_DURATION_S, 480)
                initAndStartRecording(taskId, subjectId, plannedDurationSeconds)
            }
            ACTION_STOP_BOUT -> {
                stopBoutEarly()
            }
            ACTION_MARK_EVENT -> {
                val note = intent.getStringExtra(EXTRA_MARKER_NOTE) ?: "user_marker"
                markUserEvent(note)
            }
        }
        return START_NOT_STICKY
    }

    private fun initAndStartRecording(taskId: String, subjectId: String, plannedS: Int) {
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
                _hudState.value = _hudState.value.copy(phaseRemainingSeconds = sec)
                if (sec in 1..3) {
                    vibrateTick()
                }
                updateNotification("Position phone... Countdown: ${sec}s")
                delay(1000L)
            }
            vibrateCue()
            sensorRecorder?.logEvent("countdown_end")

            // Phase 2: 5s Still Period
            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.STILL_INITIAL,
                phaseRemainingSeconds = 5
            )
            sensorRecorder?.logEvent("still_start", "phase=initial")
            for (sec in 5 downTo 1) {
                _hudState.value = _hudState.value.copy(phaseRemainingSeconds = sec)
                updateNotification("Stand still: ${sec}s")
                delay(1000L)
            }
            sensorRecorder?.logEvent("still_end", "phase=initial")
            vibrateCue()

            // Phase 3: Active Bout
            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.BOUT,
                boutElapsedSeconds = 0,
                phaseRemainingSeconds = plannedS
            )
            sensorRecorder?.logEvent("bout_start", "planned_s=$plannedS")

            var elapsed = 0
            while (elapsed < plannedS && _hudState.value.phase == RecordingPhase.BOUT) {
                delay(1000L)
                elapsed++
                _hudState.value = _hudState.value.copy(
                    boutElapsedSeconds = elapsed,
                    phaseRemainingSeconds = (plannedS - elapsed).coerceAtLeast(0),
                    accelSamplesReceived = sessionWriter?.accelCount?.get() ?: 0,
                    gyroSamplesReceived = sessionWriter?.gyroCount?.get() ?: 0,
                    osStepsReference = sensorRecorder?.lastOsStepCumulative?.get() ?: 0
                )
                val elapsedStr = String.format("%02d:%02d / %02d:%02d", elapsed / 60, elapsed % 60, plannedS / 60, plannedS % 60)
                updateNotification("Recording Bout: $elapsedStr")
            }

            // Phase 4: Final 5s Still Period
            sensorRecorder?.logEvent("bout_end", "elapsed_s=$elapsed")
            vibrateCue()

            _hudState.value = _hudState.value.copy(
                phase = RecordingPhase.STILL_FINAL,
                phaseRemainingSeconds = 5
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

            // Phase 5: Completed
            _hudState.value = _hudState.value.copy(phase = RecordingPhase.COMPLETED)
            sensorRecorder?.stopRecording()

            currentSessionDir?.let {
                crashRecoveryManager.markSessionClosed(currentSessionId)
            }

            updateNotification("Session finished. Review summary.")
        }
    }

    fun stopBoutEarly() {
        if (_hudState.value.phase == RecordingPhase.BOUT) {
            // Signal transition to next phase
            _hudState.value = _hudState.value.copy(phase = RecordingPhase.STILL_FINAL)
        }
    }

    fun markUserEvent(note: String) {
        sensorRecorder?.logEvent("user_marker", note)
    }

    private fun vibrateTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }

    private fun vibrateCue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(350)
        }
    }

    private fun vibrateTerminal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 300, 150, 600)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(700)
        }
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
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        sequenceJob?.cancel()

        try {
            unregisterReceiver(screenReceiver)
        } catch (ignored: Exception) {
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        sensorRecorder?.stopRecording()
    }
}
