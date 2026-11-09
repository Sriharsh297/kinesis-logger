package org.kinesis.logger.quality

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import org.kinesis.logger.model.Task
import org.kinesis.logger.recorder.SensorRecorder
import org.kinesis.logger.recorder.SessionWriter
import java.io.File
import java.util.UUID

object SessionFinalizer {
    private const val TAG = "SessionFinalizer"

    /**
     * Finalizes a recorded session directory:
     * 1. Compresses accel.csv -> accel.csv.gz and gyro.csv -> gyro.csv.gz, verifying gzip checksums and removing raw csv files.
     * 2. Computes the quality report via QualityCalculator.
     * 3. Generates the initial manifest.sha256.
     * 4. Builds session.json with complete metadata and saves it.
     * 5. Re-generates manifest.sha256 to include session.json.
     * 6. Returns true if all mandatory files exist and are verified.
     */
    fun finalizeSession(
        context: Context,
        sessionDir: File,
        task: Task,
        subjectId: String,
        sensorRecorder: SensorRecorder? = null,
        manualStepCount: Int? = null,
        notes: String = "",
        sessionOk: Boolean = true,
        discardReason: String? = null
    ): Boolean {
        return try {
            if (!sessionDir.exists() || !sessionDir.isDirectory) {
                Log.e(TAG, "Cannot finalize non-existent directory: ${sessionDir.absolutePath}")
                return false
            }

            // Step 1: Compress accel.csv and gyro.csv if raw exists
            val accelFile = File(sessionDir, "accel.csv")
            val gyroFile = File(sessionDir, "gyro.csv")
            val accelGz = File(sessionDir, "accel.csv.gz")
            val gyroGz = File(sessionDir, "gyro.csv.gz")

            if (accelFile.exists()) {
                val ok = SessionWriter.gzipFile(accelFile, accelGz)
                if (ok && SessionWriter.verifyGzip(accelGz)) {
                    accelFile.delete()
                    Log.i(TAG, "Compressed accel.csv -> accel.csv.gz (${accelGz.length()} bytes)")
                } else {
                    Log.w(TAG, "Failed to compress or verify accel.csv.gz")
                }
            }

            if (gyroFile.exists()) {
                val ok = SessionWriter.gzipFile(gyroFile, gyroGz)
                if (ok && SessionWriter.verifyGzip(gyroGz)) {
                    gyroFile.delete()
                    Log.i(TAG, "Compressed gyro.csv -> gyro.csv.gz (${gyroGz.length()} bytes)")
                } else {
                    Log.w(TAG, "Failed to compress or verify gyro.csv.gz")
                }
            }

            // Step 2: Compute Quality Report
            val qualityReport = QualityCalculator.calculate(sessionDir)

            // Step 3: Generate preliminary manifest (before session.json)
            val initialHashes = ManifestGenerator.generateManifest(sessionDir)

            // Step 4: Build session.json
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val endBattery = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80

            val startUtc = sensorRecorder?.startUtcMs ?: (System.currentTimeMillis() - (qualityReport.durationS * 1000).toLong())
            val startNs = sensorRecorder?.startElapsedNs ?: System.nanoTime()
            val bootOffset = sensorRecorder?.bootOffsetMs ?: (startUtc - (startNs / 1_000_000L))

            val params = SessionSaveParams(
                sessionId = sessionDir.name,
                subjectId = subjectId,
                task = task,
                startUtcMs = startUtc,
                startElapsedNs = startNs,
                bootOffsetMs = bootOffset,
                sessionOk = sessionOk,
                discardReason = if (!sessionOk) discardReason else null,
                notes = notes,
                manualStepCount = manualStepCount,
                countMethod = if (task.boutType == "counted") "partner" else null,
                batteryStartPct = 80,
                batteryEndPct = endBattery,
                installId = UUID.randomUUID().toString().substring(0, 8)
            )

            val sessionJson = SessionJsonBuilder.build(params, qualityReport, initialHashes)
            File(sessionDir, "session.json").writeText(sessionJson.toString(2))
            Log.i(TAG, "Wrote session.json for ${sessionDir.name}")

            // Step 5: Final manifest including session.json
            val finalHashes = ManifestGenerator.generateManifest(sessionDir)
            Log.i(TAG, "Generated final manifest.sha256 with ${finalHashes.size} entries.")

            // Verify mandatory files
            val mandatoryFiles = listOf("events.csv", "session.json", "manifest.sha256")
            val hasAllMandatory = mandatoryFiles.all { File(sessionDir, it).exists() } &&
                    (File(sessionDir, "accel.csv.gz").exists() || File(sessionDir, "accel.csv").exists()) &&
                    (File(sessionDir, "gyro.csv.gz").exists() || File(sessionDir, "gyro.csv").exists())

            hasAllMandatory
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing session: ${e.message}", e)
            false
        }
    }
}
