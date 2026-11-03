package org.kinesis.logger.service

enum class RecordingPhase {
    IDLE,
    COUNTDOWN,       // 10s countdown to position phone
    STILL_INITIAL,   // 5s stand still
    BOUT,            // Active recording bout
    STILL_FINAL,     // 5s stand still after bout
    COMPLETED        // Finished, ready for post-session review
}

data class LiveRecordingHud(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val phaseRemainingSeconds: Int = 0,
    val boutElapsedSeconds: Int = 0,
    val plannedBoutSeconds: Int = 0,
    val accelSamplesReceived: Long = 0,
    val gyroSamplesReceived: Long = 0,
    val gpsAccuracyMeters: Float = -1f,
    val osStepsReference: Long = 0,
    val isMockGps: Boolean = false,
    val isScreenOff: Boolean = false,
    val sessionId: String = ""
)
