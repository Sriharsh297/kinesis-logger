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
 * Manages the Wi-Fi-only upload queue for all saved sessions.
 *
 * Spec v0.5:
 * - Wi-Fi only, resumable per-file, SHA-256 checked.
 * - Delete local copy only after manifest.sha256 is verified on S3.
 * - Never embed AWS keys in the APK.
 */
class UploadQueueManager(private val context: Context) {

    private val TAG = "UploadQueue"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val STATUS_FILE = "upload_status.json"

    // ── Public API ────────────────────────────────────────────────────────────

    /** Enqueue a completed session directory for background upload. */
    fun enqueue(sessionDir: File) {
        if (!isOnWifi()) {
            Log.i(TAG, "Not on Wi-Fi; skipping upload for ${sessionDir.name}")
            setStatus(sessionDir, UploadStatus.LOCAL, "Waiting for Wi-Fi")
            return
        }
        setStatus(sessionDir, UploadStatus.UPLOADING, null)
        scope.launch { doUpload(sessionDir) }
    }

    /** Try to upload any LOCAL / FAILED sessions that haven't been sent yet. */
    fun drainQueue(rawDir: File) {
        if (!isOnWifi()) return
        scope.launch {
            rawDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
                val status = getStatus(dir)
                if (status == UploadStatus.LOCAL || status == UploadStatus.FAILED) {
                    setStatus(dir, UploadStatus.UPLOADING, null)
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
            }
        )

        if (S3Uploader.allSucceeded(results)) {
            setStatus(sessionDir, UploadStatus.UPLOADED, null)
            Log.i(TAG, "Upload complete: ${sessionDir.name}")
            // Spec: delete local copy only after manifest.sha256 verified
            // (the uploader already checks HTTP 200 on each PUT; manifest is last)
            // Keep files locally for now per v0 policy; v0.5 can add deleteRecursively()
        } else {
            val failures = results.filterValues { it is UploadResult.Failure }
            val note = failures.entries.joinToString { (k, v) ->
                "$k: ${(v as UploadResult.Failure).reason}"
            }
            setStatus(sessionDir, UploadStatus.FAILED, note)
            Log.e(TAG, "Upload partial failure: $note")
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

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
