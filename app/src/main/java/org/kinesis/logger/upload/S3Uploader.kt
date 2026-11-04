package org.kinesis.logger.upload

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Possible outcomes of a single-file upload attempt. */
sealed class UploadResult {
    data class Success(val key: String) : UploadResult()
    data class Failure(val reason: String, val retryable: Boolean) : UploadResult()
}

/**
 * Kinesis S3 upload pipeline (spec v0.5).
 *
 * Protocol:
 *  1. POST {session_id, filename, sha256} to [presignEndpoint] → receive presigned PUT URL.
 *  2. PUT the file bytes to that URL with the matching checksum header.
 *  3. Verify 200/204; mark session uploaded only after manifest.sha256 succeeds.
 *
 * Rules:
 * - Wi-Fi only (caller must check NetworkCapabilities before calling).
 * - AWS keys NEVER embedded in the APK.
 * - Local copy stays intact until all files are verified on S3.
 */
object S3Uploader {

    private const val TAG = "S3Uploader"
    private const val PRESIGN_ENDPOINT =
        "https://q4ab24v9cb.execute-api.us-east-1.amazonaws.com/prod/presign"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS    = 60_000

    // Files to upload in order; manifest last so it's the commit marker.
    private val UPLOAD_ORDER = listOf(
        "accel.csv.gz",
        "gyro.csv.gz",
        "location.csv",
        "step_detector.csv",
        "step_counter.csv",
        "events.csv",
        "session.json",
        "manifest.sha256"
    )

    /**
     * Upload every file in [sessionDir] to S3.
     *
     * @return map of filename → UploadResult. All must be [UploadResult.Success] for the
     *         caller to mark the session as uploaded.
     */
    suspend fun uploadSession(
        sessionDir: File,
        presignEndpoint: String = PRESIGN_ENDPOINT,
        onProgress: (filename: String, done: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Map<String, UploadResult> = withContext(Dispatchers.IO) {

        val sessionId = sessionDir.name
        val results   = mutableMapOf<String, UploadResult>()
        val files     = UPLOAD_ORDER.mapNotNull { name ->
            val f = File(sessionDir, name)
            if (f.exists() && f.length() > 0) name to f else null
        }
        val total = files.size

        for ((index, pair) in files.withIndex()) {
            val (filename, file) = pair
            onProgress(filename, index, total)
            Log.i(TAG, "Uploading [$sessionId] $filename (${file.length()} bytes)")

            val sha256 = sha256Hex(file)
            val result = runCatching {
                // Step 1: get pre-signed URL
                val presignResponse = requestPresignUrl(presignEndpoint, sessionId, filename, sha256)
                val putUrl = presignResponse.getString("url")

                // Step 2: PUT the file
                putFile(putUrl, file, sha256)
            }.fold(
                onSuccess = { UploadResult.Success(key = "sessions/$sessionId/$filename") },
                onFailure = { e ->
                    Log.e(TAG, "Upload failed [$sessionId/$filename]: ${e.message}")
                    UploadResult.Failure(
                        reason    = e.message ?: "Unknown error",
                        retryable = e !is IllegalArgumentException
                    )
                }
            )

            results[filename] = result
            if (result is UploadResult.Failure && !result.retryable) break
        }

        onProgress("done", total, total)
        results
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun requestPresignUrl(
        endpoint: String,
        sessionId: String,
        filename: String,
        sha256: String
    ): JSONObject {
        val body = JSONObject().apply {
            put("session_id", sessionId)
            put("filename",   filename)
            put("sha256",     sha256)
        }.toString().toByteArray(Charsets.UTF_8)

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod      = "POST"
            connectTimeout     = CONNECT_TIMEOUT_MS
            readTimeout        = READ_TIMEOUT_MS
            doOutput           = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Content-Length", body.size.toString())
        }

        conn.outputStream.use { it.write(body) }

        val code = conn.responseCode
        val responseBody = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText() ?: ""

        if (code !in 200..299) {
            throw RuntimeException("Presign request failed HTTP $code: $responseBody")
        }
        return JSONObject(responseBody)
    }

    private fun putFile(presignedUrl: String, file: File, sha256Hex: String) {
        val conn = (URL(presignedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod  = "PUT"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout    = READ_TIMEOUT_MS
            doOutput       = true
            setRequestProperty("Content-Length", file.length().toString())
            setRequestProperty("x-amz-checksum-sha256", sha256ToBase64(sha256Hex))
            setRequestProperty("Content-Type", contentTypeFor(file.name))
        }

        file.inputStream().use { input ->
            conn.outputStream.use { output -> input.copyTo(output) }
        }

        val code = conn.responseCode
        if (code !in 200..204) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw RuntimeException("S3 PUT failed HTTP $code: $err")
        }
    }

    // ── Checksum utilities ───────────────────────────────────────────────────

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(65_536)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256ToBase64(hex: String): String {
        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    private fun contentTypeFor(filename: String) = when {
        filename.endsWith(".gz")   -> "application/gzip"
        filename.endsWith(".json") -> "application/json"
        else                       -> "text/plain"
    }

    /** True when all files in [results] succeeded. */
    fun allSucceeded(results: Map<String, UploadResult>) =
        results.isNotEmpty() && results.values.all { it is UploadResult.Success }
}
