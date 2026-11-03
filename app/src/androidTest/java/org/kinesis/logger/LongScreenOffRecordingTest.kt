package org.kinesis.logger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kinesis.logger.recorder.AccelSample
import org.kinesis.logger.recorder.EventSample
import org.kinesis.logger.recorder.GyroSample
import org.kinesis.logger.recorder.SessionWriter
import java.io.File

@RunWith(AndroidJUnit4::class)
class LongScreenOffRecordingTest {

    @Test
    fun testTenMinuteScreenOffFileVerification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testSessionDir = File(context.cacheDir, "test_10m_screenoff_session").apply { mkdirs() }
        val writer = SessionWriter(sessionDir = testSessionDir, queueCapacity = 10_000)

        writer.start()

        val startNs = 1_000_000_000L
        writer.offerEvent(
            EventSample(tNs = startNs, type = "screen_off", note = "simulated_screen_off_start")
        )

        // 10 minutes at 100 Hz = 600 seconds * 100 samples/s = 60,000 samples
        // We write in batches to verify buffer streaming and monotonicity
        val sampleIntervalNs = 10_000_000L // 10 ms = 100 Hz
        val totalExpectedSamples = 60_000
        var currentNs = startNs

        for (i in 0 until totalExpectedSamples) {
            currentNs += sampleIntervalNs
            writer.offerAccel(
                AccelSample(tNs = currentNs, x = 0.05f, y = 9.81f, z = 0.12f, accuracy = 3)
            )
            writer.offerGyro(
                GyroSample(tNs = currentNs, x = 0.01f, y = -0.01f, z = 0.02f, accuracy = 3)
            )
        }

        writer.offerEvent(
            EventSample(tNs = currentNs, type = "screen_on", note = "simulated_screen_off_end")
        )

        writer.stopAndClose()

        // Gate: At least 99% of samples preserved
        val totalPreservedAccel = writer.accelCount.get()
        val totalPreservedGyro = writer.gyroCount.get()
        val minimumExpected = (totalExpectedSamples * 0.99).toLong()

        assertTrue(
            "Must preserve at least 99% of expected samples (Preserved: $totalPreservedAccel)",
            totalPreservedAccel >= minimumExpected
        )
        assertTrue(
            "Must preserve at least 99% of expected samples (Preserved: $totalPreservedGyro)",
            totalPreservedGyro >= minimumExpected
        )

        // Verify files exist and are verified by gzip
        assertTrue("accel.csv exists", writer.accelFile.exists())
        val compressionSuccess = writer.compressAndVerify()
        assertTrue("Compression and verification succeeded", compressionSuccess)

        val accelGz = File(testSessionDir, "accel.csv.gz")
        val gyroGz = File(testSessionDir, "gyro.csv.gz")
        assertTrue("accel.csv.gz exists", accelGz.exists() && accelGz.length() > 0)
        assertTrue("gyro.csv.gz exists", gyroGz.exists() && gyroGz.length() > 0)

        testSessionDir.deleteRecursively()
    }
}
