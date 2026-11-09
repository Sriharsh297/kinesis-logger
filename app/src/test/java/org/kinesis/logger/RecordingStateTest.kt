package org.kinesis.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kinesis.logger.service.LiveRecordingHud
import org.kinesis.logger.service.RecordingPhase

class RecordingStateTest {

    @Test
    fun testLiveRecordingHudDefaults() {
        val hud = LiveRecordingHud()
        assertEquals(RecordingPhase.IDLE, hud.phase)
        assertEquals(0, hud.boutElapsedSeconds)
        assertEquals(0L, hud.accelSamplesReceived)
        assertEquals(0L, hud.gyroSamplesReceived)
        assertEquals(-1f, hud.gpsAccuracyMeters, 0.001f)
        assertFalse(hud.isStopping)
    }

    @Test
    fun testBoutPhaseAndRateCalculations() {
        val hud = LiveRecordingHud(
            phase = RecordingPhase.BOUT,
            boutElapsedSeconds = 120,
            plannedBoutSeconds = 360,
            accelSamplesReceived = 12340L,
            gyroSamplesReceived = 24680L,
            accelLiveHz = 102.8f,
            gyroLiveHz = 205.6f,
            gpsAccuracyMeters = 3.5f,
            gpsFixCount = 120L
        )

        assertEquals(RecordingPhase.BOUT, hud.phase)
        assertEquals(120, hud.boutElapsedSeconds)
        assertTrue(hud.accelLiveHz >= 100f)
        assertTrue(hud.gyroLiveHz >= 200f)
        assertTrue(hud.gpsAccuracyMeters <= 5f)
        assertEquals(120L, hud.gpsFixCount)
    }

    @Test
    fun testStoppingTransition() {
        val hud = LiveRecordingHud(
            phase = RecordingPhase.BOUT,
            isStopping = false
        )
        val stoppingHud = hud.copy(
            phase = RecordingPhase.STILL_FINAL,
            isStopping = true,
            phaseRemainingSeconds = 5
        )
        assertEquals(RecordingPhase.STILL_FINAL, stoppingHud.phase)
        assertTrue(stoppingHud.isStopping)
        assertEquals(5, stoppingHud.phaseRemainingSeconds)
    }
}
