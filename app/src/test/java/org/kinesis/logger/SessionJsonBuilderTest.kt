package org.kinesis.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kinesis.logger.model.Task
import org.kinesis.logger.quality.QualityReport
import org.kinesis.logger.quality.SessionJsonBuilder
import org.kinesis.logger.quality.SessionSaveParams

class SessionJsonBuilderTest {

    @Test
    fun testSessionJsonFullSpecConformity() {
        val task = Task(
            taskId = "T01",
            title = "Walk Normal",
            labelFamily = "GENUINE_MOVEMENT",
            labelType = "G1",
            labelBinaryFraud = 0,
            rewardEligible = 1,
            speedLabel = "normal",
            placement = "front pocket",
            orientationInstruction = "Screen to body",
            surface = "pavement",
            environment = "outdoor open",
            boutPlannedS = 480,
            boutType = "free",
            attackMode = "none",
            targetSteps = null,
            description = "Test walk"
        )

        val params = SessionSaveParams(
            sessionId = "KIN-20261103-S01-T01-abc123",
            subjectId = "S01",
            task = task,
            startUtcMs = 1730000000000L,
            startElapsedNs = 5000000000L,
            bootOffsetMs = 1729995000000L,
            sessionOk = true,
            discardReason = null,
            notes = "Clean session",
            manualStepCount = null,
            countMethod = null,
            batteryStartPct = 90,
            batteryEndPct = 88,
            installId = "inst-001"
        )

        val quality = QualityReport(
            durationS = 480.0,
            accelSamples = 48000,
            gyroSamples = 48000,
            stepDetectorEvents = 520,
            stepCounterDelta = 518,
            locationSamples = 480,
            accelAchievedRateHz = 100.0,
            gyroAchievedRateHz = 100.0,
            accelGapsOver100ms = 0,
            gyroGapsOver100ms = 0,
            accelP99GapMs = 10.1,
            gyroP99GapMs = 10.1,
            gpsFixPercent = 99.0,
            gpsMedianAccuracyM = 6.5f,
            mockFlagPercent = 0.0,
            screenOffSeconds = 470.0
        )

        val fileHashes = mapOf(
            "accel.csv.gz" to "hash_accel",
            "gyro.csv.gz" to "hash_gyro",
            "events.csv" to "hash_events"
        )

        val json = SessionJsonBuilder.build(params, quality, fileHashes)

        // Validate top-level keys per spec section 4
        assertTrue(json.has("identity"))
        assertTrue(json.has("device"))
        assertTrue(json.has("time"))
        assertTrue(json.has("labels"))
        assertTrue(json.has("context"))
        assertTrue(json.has("fraud"))
        assertTrue(json.has("ground_truth"))
        assertTrue(json.has("quality"))
        assertTrue(json.has("admin"))

        // Identity
        val identity = json.getJSONObject("identity")
        assertEquals("KIN-20261103-S01-T01-abc123", identity.getString("session_id"))
        assertEquals("v0.1", identity.getString("protocol_version"))
        assertEquals("0.1.0", identity.getString("app_version"))

        // Labels
        val labels = json.getJSONObject("labels")
        assertEquals("GENUINE_MOVEMENT", labels.getString("label_family"))
        assertEquals("G1", labels.getString("label_type"))
        assertEquals(0, labels.getInt("label_binary_fraud"))
        assertEquals(1, labels.getInt("reward_eligible"))

        // Quality
        val qualityJson = json.getJSONObject("quality")
        assertEquals(480.0, qualityJson.getDouble("duration_s"), 0.01)
        assertEquals(0, qualityJson.getInt("gap_count_over_100ms"))
        assertEquals(99.0, qualityJson.getDouble("gps_fix_pct"), 0.01)
        val hashesJson = qualityJson.getJSONObject("files_sha256")
        assertEquals("hash_accel", hashesJson.getString("accel.csv.gz"))

        // Admin
        val admin = json.getJSONObject("admin")
        assertTrue(admin.getBoolean("session_ok"))
        assertEquals("Clean session", admin.getString("notes"))
    }
}
