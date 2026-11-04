package org.kinesis.logger

import android.app.Application
import org.kinesis.logger.upload.UploadQueueManager
import java.io.File

class KinesisApplication : Application() {

    /** Singleton upload queue manager — survives Activity lifecycle changes. */
    lateinit var uploadQueue: UploadQueueManager
        private set

    override fun onCreate() {
        super.onCreate()
        uploadQueue = UploadQueueManager(this)

        // On launch drain any LOCAL/FAILED sessions (Wi-Fi guarded inside)
        val rawDir = File(getExternalFilesDir(null) ?: filesDir, "raw")
        if (rawDir.exists()) uploadQueue.drainQueue(rawDir)
    }
}
