package org.kinesis.logger

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kinesis.logger.quality.QualityCalculator
import java.io.File
import java.nio.file.Files

class QualityCalculatorTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("kinesis_quality_test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testQualityMetricsFromSyntheticSession() {
        // Create synthetic accel.csv (100 samples at 100 Hz = 1.0 second)
        val accelFile = File(tempDir, "accel.csv")
        accelFile.writeText("t_ns,x,y,z,accuracy\n")
        val baseNs = 1_000_000_000L
        for (i in 0 until 100) {
            val tNs = baseNs + (i * 10_000_000L) // exactly 10ms apart
            accelFile.appendText("$tNs,0.1,9.8,0.2,3\n")
        }

        // Create synthetic gyro.csv (with one gap > 100ms)
        val gyroFile = File(tempDir, "gyro.csv")
        gyroFile.writeText("t_ns,x,y,z,accuracy\n")
        var gyroNs = baseNs
        for (i in 0 until 50) {
            gyroNs += 10_000_000L
            gyroFile.appendText("$gyroNs,0.0,0.0,0.0,3\n")
        }
        // inject 150 ms gap
        gyroNs += 150_000_000L
        for (i in 0 until 50) {
            gyroNs += 10_000_000L
            gyroFile.appendText("$gyroNs,0.0,0.0,0.0,3\n")
        }

        // Create location.csv (5 samples at 1 Hz)
        val locFile = File(tempDir, "location.csv")
        locFile.writeText("t_ns,utc_ms,provider,lat,lon,alt,accuracy,speed,speed_acc,bearing,bearing_acc,is_mock\n")
        for (i in 0 until 5) {
            val locNs = baseNs + (i * 1_000_000_000L)
            locFile.appendText("$locNs,1730000000000,fused,12.97,77.59,900.0,8.5,1.2,0.1,90.0,1.0,0\n")
        }

        // Create step_detector.csv and step_counter.csv
        val stepDetFile = File(tempDir, "step_detector.csv")
        stepDetFile.writeText("t_ns\n")
        stepDetFile.appendText("${baseNs + 500_000_000L}\n")
        stepDetFile.appendText("${baseNs + 900_000_000L}\n")

        val stepCounterFile = File(tempDir, "step_counter.csv")
        stepCounterFile.writeText("t_ns,cumulative\n")
        stepCounterFile.appendText("${baseNs + 100_000_000L},100\n")
        stepCounterFile.appendText("${baseNs + 900_000_000L},105\n")

        // Create events.csv with a screen_off interval
        val eventsFile = File(tempDir, "events.csv")
        eventsFile.writeText("t_ns,type,note\n")
        eventsFile.appendText("${baseNs + 100_000_000L},screen_off,\n")
        eventsFile.appendText("${baseNs + 600_000_000L},screen_on,\n")

        val report = QualityCalculator.calculate(tempDir)

        assertEquals(100L, report.accelSamples)
        assertEquals(100L, report.gyroSamples)
        assertEquals(0, report.accelGapsOver100ms)
        assertEquals(1, report.gyroGapsOver100ms)
        assertEquals(2L, report.stepDetectorEvents)
        assertEquals(5L, report.stepCounterDelta)
        assertEquals(5L, report.locationSamples)
        assertEquals(8.5f, report.gpsMedianAccuracyM, 0.1f)
        assertEquals(0.5, report.screenOffSeconds, 0.05)
    }
}
