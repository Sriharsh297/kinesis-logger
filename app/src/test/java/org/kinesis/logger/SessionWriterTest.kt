package org.kinesis.logger

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kinesis.logger.recorder.AccelSample
import org.kinesis.logger.recorder.EventSample
import org.kinesis.logger.recorder.GyroSample
import org.kinesis.logger.recorder.LocationSample
import org.kinesis.logger.recorder.SessionWriter
import org.kinesis.logger.recorder.StepCounterSample
import org.kinesis.logger.recorder.StepDetectorSample
import java.io.File
import java.nio.file.Files

class SessionWriterTest {

    private lateinit var tempDir: File
    private lateinit var sessionWriter: SessionWriter

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("kinesis_test_session").toFile()
        sessionWriter = SessionWriter(sessionDir = tempDir, queueCapacity = 100)
    }

    @After
    fun tearDown() {
        sessionWriter.stopAndClose()
        tempDir.deleteRecursively()
    }

    @Test
    fun testHeadersAndWritingWithoutDataLoss() {
        sessionWriter.start()

        // Offer 50 accel samples
        for (i in 0 until 50) {
            val success = sessionWriter.offerAccel(
                AccelSample(
                    tNs = 1_000_000_000L + (i * 10_000_000L),
                    x = 0.1f * i,
                    y = 9.8f,
                    z = 0.2f,
                    accuracy = 3
                )
            )
            assertTrue(success)
        }

        // Offer 50 gyro samples
        for (i in 0 until 50) {
            val success = sessionWriter.offerGyro(
                GyroSample(
                    tNs = 1_000_000_000L + (i * 10_000_000L),
                    x = 0.01f * i,
                    y = -0.02f,
                    z = 0.05f,
                    accuracy = 3
                )
            )
            assertTrue(success)
        }

        // Offer step detector & counter
        sessionWriter.offerStepDetector(StepDetectorSample(tNs = 1_000_500_000L))
        sessionWriter.offerStepCounter(StepCounterSample(tNs = 1_000_500_000L, cumulative = 120))

        // Offer location
        sessionWriter.offerLocation(
            LocationSample(
                tNs = 1_000_000_000L,
                utcMs = 1730000000000L,
                provider = "fused",
                lat = 12.9716,
                lon = 77.5946,
                alt = 920.0,
                accuracy = 4.5f,
                speed = 1.2f,
                speedAcc = 0.1f,
                bearing = 180f,
                bearingAcc = 2f,
                isMock = 0
            )
        )

        // Offer event
        sessionWriter.offerEvent(
            EventSample(
                tNs = 1_000_000_000L,
                type = "bout_start",
                note = "planned_s=480"
            )
        )

        // Stop and close
        sessionWriter.stopAndClose()

        // Verify CSV contents
        val accelLines = sessionWriter.accelFile.readLines()
        assertEquals("t_ns,x,y,z,accuracy", accelLines.first())
        assertEquals(51, accelLines.size) // header + 50 items

        val gyroLines = sessionWriter.gyroFile.readLines()
        assertEquals("t_ns,x,y,z,accuracy", gyroLines.first())
        assertEquals(51, gyroLines.size)

        val stepDetLines = sessionWriter.stepDetectorFile.readLines()
        assertEquals("t_ns", stepDetLines.first())
        assertEquals(2, stepDetLines.size)

        val stepCountLines = sessionWriter.stepCounterFile.readLines()
        assertEquals("t_ns,cumulative", stepCountLines.first())
        assertEquals(2, stepCountLines.size)
        assertEquals("1000500000,120", stepCountLines[1])

        val locLines = sessionWriter.locationFile.readLines()
        assertTrue(locLines.first().startsWith("t_ns,utc_ms,provider"))
        assertEquals(2, locLines.size)

        val eventLines = sessionWriter.eventsFile.readLines()
        assertEquals("t_ns,type,note", eventLines.first())
        assertEquals(2, eventLines.size)
        assertEquals("1000000000,bout_start,planned_s=480", eventLines[1])

        // Verify dropped counts are 0
        assertEquals(0L, sessionWriter.droppedAccel.get())
        assertEquals(0L, sessionWriter.droppedGyro.get())
    }

    @Test
    fun testQueueOverflowDroppedEventCount() {
        val tinyWriter = SessionWriter(sessionDir = tempDir, queueCapacity = 5)
        // Note: start() is NOT called so the queue will not be drained, filling it up
        for (i in 0 until 10) {
            tinyWriter.offerAccel(
                AccelSample(tNs = i.toLong(), x = 0f, y = 0f, z = 0f, accuracy = 0)
            )
        }

        assertEquals(5L, tinyWriter.droppedAccel.get())
        tinyWriter.stopAndClose()
    }

    @Test
    fun testGzipCompressionAndVerification() {
        sessionWriter.start()
        for (i in 0 until 100) {
            sessionWriter.offerAccel(
                AccelSample(tNs = i * 1000L, x = 1f, y = 2f, z = 3f, accuracy = 3)
            )
            sessionWriter.offerGyro(
                GyroSample(tNs = i * 1000L, x = 0f, y = 0f, z = 0f, accuracy = 3)
            )
        }
        sessionWriter.stopAndClose()

        assertTrue(sessionWriter.accelFile.exists())
        assertTrue(sessionWriter.gyroFile.exists())

        val compressionSuccess = sessionWriter.compressAndVerify()
        assertTrue("Compression and verification must succeed", compressionSuccess)

        // Raw accel and gyro CSV should be deleted
        assertFalse(sessionWriter.accelFile.exists())
        assertFalse(sessionWriter.gyroFile.exists())

        // .csv.gz files must exist and be non-empty
        val accelGz = File(tempDir, "accel.csv.gz")
        val gyroGz = File(tempDir, "gyro.csv.gz")
        assertTrue(accelGz.exists() && accelGz.length() > 0)
        assertTrue(gyroGz.exists() && gyroGz.length() > 0)
    }
}
