package org.kinesis.logger.service

import android.content.Context
import java.io.File
import java.io.FileWriter

class CrashRecoveryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("kinesis_crash_prefs", Context.MODE_PRIVATE)

    fun markSessionActive(sessionId: String, sessionPath: String) {
        prefs.edit()
            .putString("active_session_id", sessionId)
            .putString("active_session_path", sessionPath)
            .putBoolean("is_active", true)
            .apply()
    }

    fun markSessionClosed(sessionId: String) {
        if (prefs.getString("active_session_id", null) == sessionId) {
            prefs.edit()
                .remove("active_session_id")
                .remove("active_session_path")
                .putBoolean("is_active", false)
                .apply()
        }
    }

    /**
     * Checks if a session was left unclosed due to a crash or system termination.
     * Appends an 'interruption' event to events.csv and flags the session.
     */
    fun checkAndRecoverInterruptedSession(): String? {
        val isActive = prefs.getBoolean("is_active", false)
        if (!isActive) return null

        val sessionId = prefs.getString("active_session_id", null) ?: return null
        val sessionPath = prefs.getString("active_session_path", null) ?: return null

        val sessionDir = File(sessionPath)
        if (sessionDir.exists() && sessionDir.isDirectory) {
            val eventsFile = File(sessionDir, "events.csv")
            try {
                if (eventsFile.exists()) {
                    val tNs = System.nanoTime()
                    FileWriter(eventsFile, true).use { writer ->
                        writer.append("$tNs,interruption,app_or_service_killed_mid_session\n")
                    }
                }
                // Mark recovery file
                val recoveryFile = File(sessionDir, "interrupted.flag")
                recoveryFile.writeText("Session interrupted at ${System.currentTimeMillis()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Clear active session marker once handled
        prefs.edit()
            .remove("active_session_id")
            .remove("active_session_path")
            .putBoolean("is_active", false)
            .apply()

        return sessionId
    }
}
