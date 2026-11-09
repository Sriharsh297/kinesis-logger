package org.kinesis.logger.upload

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Upload status persisted inside each session directory. */
enum class UploadStatus { LOCAL, UPLOADING, UPLOADED, FAILED }

/**
 * Manages the upload queue for all saved sessions to private AWS S3 research bucket.
 *
 * Rules:
 * - Supports Wi-Fi and Mobile Data (configurable in Settings, cellular allowed by default).
 * - Per-file resumable upload with SHA-256 integrity verification.
 * - Mark as UPLOADED only after manifest.sha256 is verified on S3.
 * - Never embed AWS keys in the APK.
 */
class UploadQueueManager(private val context: Context) {

    private val TAG = "UploadQueue"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val STATUS_FILE = "upload_status.json"
    private val PREFS_NAME = "kinesis_prefs"
    private val KEY_REQUIRE_WIFI = "require_wifi_for_upload"

    // ── Public API ────────────────────────────────────────────────────────────

    fun isWifiRequired(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REQUIRE_WIFI, false)
    }

    fun setWifiRequired(required: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REQUIRE_WIFI, required).apply()
    }

    /** Enqueue a completed session directory for background upload. */
    fun enqueue(sessionDir: File) {
        val (eligible, reason) = getNetworkEligibility()
        if (!eligible) {
            Log.i(TAG, "Network not eligible ($reason); skipping upload for ${sessionDir.name}")
            setStatus(sessionDir, UploadStatus.LOCAL, reason)
            return
        }

        setStatus(sessionDir, UploadStatus.UPLOADING, "Queued on $reason")
        scope.launch { doUpload(sessionDir) }
    }

    /** Try to upload any LOCAL / FAILED sessions that haven't been sent yet. */
    fun drainQueue(rawDir: File) {
        val (eligible, _) = getNetworkEligibility()
        if (!eligible) return

        scope.launch {
            rawDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
                val status = getStatus(dir)
                if (status == UploadStatus.LOCAL || status == UploadStatus.FAILED) {
                    setStatus(dir, UploadStatus.UPLOADING, "Queue drained")
                    doUpload(dir)
                }
            }
        }
    }

    /** Read upload status for a session directory (for the Sessions screen). */
    fun getStatus(sessionDir: File): UploadStatus {
        val f = File(sessionDir, STATUS_FILE)
        return try {
            if (!f.exists()) return UploadStatus.LOCAL
            val obj = JSONObject(f.readText())
            UploadStatus.valueOf(obj.optString("status", "LOCAL"))
        } catch (_: Exception) { UploadStatus.LOCAL }
    }

    fun getStatusNote(sessionDir: File): String? {
        val f = File(sessionDir, STATUS_FILE)
        return try {
            if (!f.exists()) return null
            JSONObject(f.readText()).optString("note").takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun doUpload(sessionDir: File) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting upload for ${sessionDir.name}")
        val results = S3Uploader.uploadSession(
            sessionDir = sessionDir,
            onProgress = { filename, done, total ->
                Log.d(TAG, "${sessionDir.name} [$done/$total] $filename")
                setStatus(sessionDir, UploadStatus.UPLOADING, "Uploading [$done/$total] $filename")
            }
        )

        if (S3Uploader.allSucceeded(results)) {
            setStatus(sessionDir, UploadStatus.UPLOADED, "Verified on S3")
            Log.i(TAG, "Upload complete & verified: ${sessionDir.name}")
        } else {
            val failures = results.filterValues { it is UploadResult.Failure }
            val note = failures.entries.joinToString { (k, v) ->
                "$k: ${(v as UploadResult.Failure).reason}"
            }
            setStatus(sessionDir, UploadStatus.FAILED, note.ifBlank { "Upload failed: missing mandatory files" })
            Log.e(TAG, "Upload failure: $note")
        }
    }

    private fun setStatus(sessionDir: File, status: UploadStatus, note: String?) {
        try {
            val obj = JSONObject().apply {
                put("status", status.name)
                put("note", note ?: "")
                put("updated_ms", System.currentTimeMillis())
            }
            File(sessionDir, STATUS_FILE).writeText(obj.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Could not write status: ${e.message}")
        }
    }

    /**
     * Determines whether the current network connection is eligible for upload.
     * Returns Pair(isEligible, transportOrReason).
     */
    fun getNetworkEligibility(): Pair<Boolean, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false to "No ConnectivityManager available"

        val activeNet = cm.activeNetwork ?: return false to "No active network connection"
        val cap = cm.getNetworkCapabilities(activeNet) ?: return false to "No network capabilities available"

        val hasInternet = cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) {
            return false to "Network has no internet connectivity"
        }

        val isWifi = cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isEthernet = cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        val requireWifi = isWifiRequired()
        if (requireWifi && !isWifi) {
            return false to "Waiting for Wi-Fi (Wi-Fi-only upload is enabled)"
        }

        val transportName = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Mobile Data"
            isEthernet -> "Ethernet"
            else -> "Network"
        }

        return true to transportName
    }
}
