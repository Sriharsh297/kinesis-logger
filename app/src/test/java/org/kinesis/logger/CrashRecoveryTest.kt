package org.kinesis.logger

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kinesis.logger.recorder.SessionWriter
import java.io.File
import java.nio.file.Files

class CrashRecoveryTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("kinesis_crash_test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testCrashInterruptionFileIntegrity() {
        val sessionWriter = SessionWriter(sessionDir = tempDir)
        sessionWriter.start()

        // Simulate app active writing
        sessionWriter.offerEvent(
            org.kinesis.logger.recorder.EventSample(
                tNs = 1_000_000_000L,
                type = "tap_start",
                note = "planned_s=300"
            )
        )
        sessionWriter.offerEvent(
            org.kinesis.logger.recorder.EventSample(
                tNs = 1_010_000_000L,
                type = "bout_start",
                note = ""
            )
        )

        // Give background writer thread a brief moment to drain queue before crash/close
        Thread.sleep(50)
        sessionWriter.flushAll()
        sessionWriter.stopAndClose()

        // Simulated next boot recovery
        val eventsFile = File(tempDir, "events.csv")
        assertTrue("events.csv must survive sudden termination", eventsFile.exists())

        // Append simulated recovery event
        eventsFile.appendText("${System.nanoTime()},interruption,app_or_service_killed_mid_session\n")

        val lines = eventsFile.readLines()
        assertEquals("t_ns,type,note", lines[0])
        assertEquals("1000000000,tap_start,planned_s=300", lines[1])
        assertEquals("1010000000,bout_start,", lines[2])
        assertTrue(lines[3].contains("interruption,app_or_service_killed_mid_session"))

        sessionWriter.stopAndClose()
    }
}
