package org.kinesis.logger.quality

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

data class QualityReport(
    val durationS: Double,
    val accelSamples: Long,
    val gyroSamples: Long,
    val stepDetectorEvents: Long,
    val stepCounterDelta: Long,
    val locationSamples: Long,
    val accelAchievedRateHz: Double,
    val gyroAchievedRateHz: Double,
    val accelGapsOver100ms: Int,
    val gyroGapsOver100ms: Int,
    val accelP99GapMs: Double,
    val gyroP99GapMs: Double,
    val gpsFixPercent: Double,
    val gpsMedianAccuracyM: Float,
    val mockFlagPercent: Double,
    val screenOffSeconds: Double
)

object QualityCalculator {

    fun calculate(sessionDir: File): QualityReport {
        val accelStats = computeStreamGapsAndRate(
            findStreamFile(sessionDir, "accel.csv")
        )
        val gyroStats = computeStreamGapsAndRate(
            findStreamFile(sessionDir, "gyro.csv")
        )

        val stepDetectorEvents = countLines(File(sessionDir, "step_detector.csv")) - 1
        val stepCounterDelta = computeStepCounterDelta(File(sessionDir, "step_counter.csv"))

        val locStats = computeLocationStats(File(sessionDir, "location.csv"))
        val screenOffS = computeScreenOffDuration(File(sessionDir, "events.csv"))

        val durationS = maxOf(accelStats.durationS, gyroStats.durationS, locStats.durationS)

        return QualityReport(
            durationS = (durationS * 10.0).roundToInt() / 10.0,
            accelSamples = accelStats.sampleCount,
            gyroSamples = gyroStats.sampleCount,
            stepDetectorEvents = stepDetectorEvents.coerceAtLeast(0),
            stepCounterDelta = stepCounterDelta,
            locationSamples = locStats.sampleCount,
            accelAchievedRateHz = accelStats.achievedRateHz,
            gyroAchievedRateHz = gyroStats.achievedRateHz,
            accelGapsOver100ms = accelStats.gapsOver100ms,
            gyroGapsOver100ms = gyroStats.gapsOver100ms,
            accelP99GapMs = accelStats.p99GapMs,
            gyroP99GapMs = gyroStats.p99GapMs,
            gpsFixPercent = locStats.fixPercent,
            gpsMedianAccuracyM = locStats.medianAccuracyM,
            mockFlagPercent = locStats.mockPercent,
            screenOffSeconds = (screenOffS * 10.0).roundToInt() / 10.0
        )
    }

    private fun findStreamFile(sessionDir: File, baseName: String): File? {
        val plain = File(sessionDir, baseName)
        if (plain.exists()) return plain
        val gzipped = File(sessionDir, "$baseName.gz")
        if (gzipped.exists()) return gzipped
        return null
    }

    data class StreamStats(
        val sampleCount: Long,
        val durationS: Double,
        val achievedRateHz: Double,
        val gapsOver100ms: Int,
        val p99GapMs: Double
    )

    fun computeStreamGapsAndRate(file: File?): StreamStats {
        if (file == null || !file.exists()) {
            return StreamStats(0, 0.0, 0.0, 0, 0.0)
        }

        val gapsMs = mutableListOf<Double>()
        var count = 0L
        var firstTimestampNs: Long? = null
        var lastTimestampNs: Long? = null
        var previousNs: Long? = null
        var gapsOver100ms = 0

        val inputStream = if (file.name.endsWith(".gz")) {
            GZIPInputStream(FileInputStream(file))
        } else {
            FileInputStream(file)
        }

        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line = reader.readLine() // header
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.isEmpty()) continue
                val tNs = parts[0].toLongOrNull() ?: continue

                count++
                if (firstTimestampNs == null) firstTimestampNs = tNs

                previousNs?.let { prev ->
                    val diffNs = tNs - prev
                    val diffMs = diffNs / 1_000_000.0
                    gapsMs.add(diffMs)
                    if (diffMs > 100.0) {
                        gapsOver100ms++
                    }
                }
                previousNs = tNs
                lastTimestampNs = tNs
            }
        }

        val durationS = if (firstTimestampNs != null && lastTimestampNs != null && lastTimestampNs!! > firstTimestampNs!!) {
            (lastTimestampNs!! - firstTimestampNs!!) / 1_000_000_000.0
        } else {
            0.0
        }

        val rateHz = if (durationS > 0.0) {
            ((count / durationS) * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }

        val p99GapMs = if (gapsMs.isNotEmpty()) {
            gapsMs.sort()
            val index = ((gapsMs.size * 0.99).toInt()).coerceIn(0, gapsMs.size - 1)
            ((gapsMs[index]) * 100.0).roundToInt() / 100.0
        } else {
            0.0
        }

        return StreamStats(
            sampleCount = count,
            durationS = durationS,
            achievedRateHz = rateHz,
            gapsOver100ms = gapsOver100ms,
            p99GapMs = p99GapMs
        )
    }

    private data class LocStats(
        val sampleCount: Long,
        val durationS: Double,
        val fixPercent: Double,
        val medianAccuracyM: Float,
        val mockPercent: Double
    )

    private fun computeLocationStats(file: File): LocStats {
        if (!file.exists()) return LocStats(0, 0.0, 0.0, -1f, 0.0)

        var count = 0L
        var mockCount = 0L
        val accuracies = mutableListOf<Float>()
        var firstNs: Long? = null
        var lastNs: Long? = null

        file.forEachLine { line ->
            if (!line.startsWith("t_ns")) {
                val parts = line.split(",")
                if (parts.size >= 12) {
                    val tNs = parts[0].toLongOrNull() ?: return@forEachLine
                    val acc = parts[6].toFloatOrNull() ?: -1f
                    val isMock = parts[11].toIntOrNull() ?: 0

                    count++
                    if (firstNs == null) firstNs = tNs
                    lastNs = tNs

                    if (acc > 0f) accuracies.add(acc)
                    if (isMock == 1) mockCount++
                }
            }
        }

        val durationS = if (firstNs != null && lastNs != null && lastNs!! > firstNs!!) {
            (lastNs!! - firstNs!!) / 1_000_000_000.0
        } else {
            0.0
        }

        val expectedSamples = (durationS.coerceAtLeast(1.0)).toInt()
        val fixPercent = if (expectedSamples > 0) {
            ((count.toDouble() / expectedSamples) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val medianAcc = if (accuracies.isNotEmpty()) {
            accuracies.sort()
            accuracies[accuracies.size / 2]
        } else {
            -1f
        }

        val mockPercent = if (count > 0) {
            ((mockCount.toDouble() / count) * 100.0)
        } else {
            0.0
        }

        return LocStats(
            sampleCount = count,
            durationS = durationS,
            fixPercent = ((fixPercent) * 10.0).roundToInt() / 10.0,
            medianAccuracyM = ((medianAcc) * 10f).roundToInt() / 10f,
            mockPercent = ((mockPercent) * 10.0).roundToInt() / 10.0
        )
    }

    private fun computeStepCounterDelta(file: File): Long {
        if (!file.exists()) return 0L
        var firstVal: Long? = null
        var lastVal: Long? = null

        file.forEachLine { line ->
            if (!line.startsWith("t_ns")) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val count = parts[1].toLongOrNull() ?: return@forEachLine
                    if (firstVal == null) firstVal = count
                    lastVal = count
                }
            }
        }

        return if (firstVal != null && lastVal != null) {
            lastVal!! - firstVal!!
        } else {
            0L
        }
    }

    private fun computeScreenOffDuration(file: File): Double {
        if (!file.exists()) return 0.0
        var totalScreenOffNs = 0L
        var screenOffStartNs: Long? = null

        file.forEachLine { line ->
            val parts = line.split(",")
            if (parts.size >= 2) {
                val tNs = parts[0].toLongOrNull() ?: return@forEachLine
                val type = parts[1].trim()
                if (type == "screen_off") {
                    screenOffStartNs = tNs
                } else if (type == "screen_on" && screenOffStartNs != null) {
                    totalScreenOffNs += (tNs - screenOffStartNs!!)
                    screenOffStartNs = null
                }
            }
        }

        return totalScreenOffNs / 1_000_000_000.0
    }

    private fun countLines(file: File): Long {
        if (!file.exists()) return 0L
        var count = 0L
        file.forEachLine { count++ }
        return count
    }
}
