package org.kinesis.logger.quality

import android.os.Build
import org.json.JSONObject
import org.kinesis.logger.model.Task
import java.io.File
import java.security.MessageDigest
import java.util.TimeZone

data class SessionSaveParams(
    val sessionId: String,
    val subjectId: String,
    val task: Task,
    val startUtcMs: Long,
    val startElapsedNs: Long,
    val bootOffsetMs: Long,
    val sessionOk: Boolean,
    val discardReason: String?,
    val notes: String,
    val manualStepCount: Int?,
    val countMethod: String?,
    val batteryStartPct: Int,
    val batteryEndPct: Int,
    val installId: String
)

object SessionJsonBuilder {

    fun build(
        params: SessionSaveParams,
        quality: QualityReport,
        fileHashes: Map<String, String>
    ): JSONObject {
        val root = JSONObject()

        // 1. Identity
        val identity = JSONObject().apply {
            put("session_id", params.sessionId)
            put("subject_id", params.subjectId)
            put("task_id", params.task.taskId)
            put("protocol_version", "v0.1")
            put("app_version", "0.1.0")
            put("build_hash", "git-main")
            put("walk_group_id", JSONObject.NULL)
            put("paired_session_id", JSONObject.NULL)
        }
        root.put("identity", identity)

        // 2. Device (auto)
        val device = JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("android_version", Build.VERSION.RELEASE)
            put("sdk_int", Build.VERSION.SDK_INT)
            put("install_id", params.installId)
            put("has_step_counter", true)
            put("has_step_detector", true)
            put("gnss_raw_supported", false)
        }
        root.put("device", device)

        // 3. Time (auto)
        val timeZone = TimeZone.getDefault()
        val tzOffsetMin = timeZone.getOffset(params.startUtcMs) / (1000 * 60)
        val time = JSONObject().apply {
            put("start_utc_ms", params.startUtcMs)
            put("tz_offset_min", tzOffsetMin)
            put("start_elapsed_ns", params.startElapsedNs)
            put("boot_offset_ms", params.bootOffsetMs)
        }
        root.put("time", time)

        // 4. Labels (from card)
        val labels = JSONObject().apply {
            put("label_family", params.task.labelFamily)
            put("label_type", params.task.labelType)
            put("label_binary_fraud", params.task.labelBinaryFraud)
            put("reward_eligible", params.task.rewardEligible)
            put("speed_label", params.task.speedLabel)
            put("bout_planned_s", params.task.boutPlannedS)
        }
        root.put("labels", labels)

        // 5. Context
        val context = JSONObject().apply {
            put("placement", params.task.placement)
            put("orientation", params.task.orientationInstruction)
            put("carried_with", JSONObject.NULL)
            put("surface", params.task.surface)
            put("environment", params.task.environment)
            put("route_id", JSONObject.NULL)
            put("footwear", JSONObject.NULL)
        }
        root.put("context", context)

        // 6. Fraud only
        val fraud = JSONObject().apply {
            put("attack_mode", params.task.attackMode)
            put("rig_id", JSONObject.NULL)
            put("mock_app_name", JSONObject.NULL)
            put("mock_route_id", JSONObject.NULL)
            put("operator_note", JSONObject.NULL)
        }
        root.put("fraud", fraud)

        // 7. Ground truth
        val groundTruth = JSONObject().apply {
            put("bout_type", params.task.boutType)
            put("manual_step_count", params.manualStepCount ?: JSONObject.NULL)
            put("count_method", params.countMethod ?: JSONObject.NULL)
        }
        root.put("ground_truth", groundTruth)

        // 8. Quality (auto at end)
        val samplesJson = JSONObject().apply {
            put("accel", quality.accelSamples)
            put("gyro", quality.gyroSamples)
            put("step_detector", quality.stepDetectorEvents)
            put("step_counter", quality.stepCounterDelta)
            put("location", quality.locationSamples)
        }

        val hashesJson = JSONObject().apply {
            fileHashes.forEach { (name, hash) -> put(name, hash) }
        }

        val qualityJson = JSONObject().apply {
            put("duration_s", quality.durationS)
            put("n_samples", samplesJson)
            put("achieved_rate_median", quality.accelAchievedRateHz)
            put("p99_gap_ms", quality.accelP99GapMs)
            put("gap_count_over_100ms", quality.accelGapsOver100ms)
            put("gps_fix_pct", quality.gpsFixPercent)
            put("gps_acc_median_m", quality.gpsMedianAccuracyM)
            put("os_step_counter_delta", quality.stepCounterDelta)
            put("os_step_event_count", quality.stepDetectorEvents)
            put("mock_flag_pct", quality.mockFlagPercent)
            put("screen_off_s", quality.screenOffSeconds)
            put("battery_start_pct", params.batteryStartPct)
            put("battery_end_pct", params.batteryEndPct)
            put("app_restarts", 0)
            put("files_sha256", hashesJson)
        }
        root.put("quality", qualityJson)

        // 9. Admin
        val admin = JSONObject().apply {
            put("session_ok", params.sessionOk)
            put("discard_reason", params.discardReason ?: JSONObject.NULL)
            put("notes", params.notes)
            put("consent_version", "v0.1")
            put("upload_status", "local")
        }
        root.put("admin", admin)

        return root
    }
}
