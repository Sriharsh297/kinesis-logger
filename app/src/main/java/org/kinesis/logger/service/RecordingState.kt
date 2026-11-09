package org.kinesis.logger.service

enum class RecordingPhase {
    IDLE,
    COUNTDOWN,
    STILL_INITIAL,
    BOUT,
    STILL_FINAL,
    COMPLETED
}

data class LiveRecordingHud(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val phaseRemainingSeconds: Int = 0,
    val boutElapsedSeconds: Int = 0,
    val plannedBoutSeconds: Int = 480,
    val accelSamplesReceived: Long = 0,
    val gyroSamplesReceived: Long = 0,
    val accelLiveHz: Float = 0f,
    val gyroLiveHz: Float = 0f,
    val gpsAccuracyMeters: Float = -1f,
    val gpsFixCount: Long = 0L,
    val lastSensorUpdateMs: Long = 0L,
    val osStepsReference: Long = 0,
    val isScreenOff: Boolean = false,
    val sessionId: String = "",
    val isStopping: Boolean = false,
    val lastMarkerNote: String? = null,
    val lastMarkerTimeSec: Int? = null,
    val errorMessage: String? = null
)
