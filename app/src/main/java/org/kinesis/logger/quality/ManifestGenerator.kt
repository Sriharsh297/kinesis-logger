package org.kinesis.logger.quality

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ManifestGenerator {

    fun generateManifest(sessionDir: File): Map<String, String> {
        val hashes = mutableMapOf<String, String>()
        val manifestFile = File(sessionDir, "manifest.sha256")

        val targetFiles = sessionDir.listFiles { file ->
            file.isFile && file.name != "manifest.sha256" && !file.name.endsWith(".tmp")
        } ?: emptyArray()

        targetFiles.sortBy { it.name }

        val sb = StringBuilder()
        for (file in targetFiles) {
            val hash = calculateSha256(file)
            hashes[file.name] = hash
            sb.append("$hash  ${file.name}\n")
        }

        manifestFile.writeText(sb.toString())
        return hashes
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
