package org.kinesis.logger.quality

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SessionExporter {

    fun exportSessionZip(context: Context, sessionDir: File): Intent? {
        if (!sessionDir.exists() || !sessionDir.isDirectory) return null

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "${sessionDir.name}.zip")

        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val files = sessionDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile) {
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Kinesis Session Data: ${sessionDir.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            return Intent.createChooser(shareIntent, "Export Session ZIP")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
