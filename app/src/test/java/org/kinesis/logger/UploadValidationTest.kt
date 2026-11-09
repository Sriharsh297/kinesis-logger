package org.kinesis.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kinesis.logger.recorder.SessionWriter
import org.kinesis.logger.upload.S3Uploader
import org.kinesis.logger.upload.UploadResult
import java.io.File
import java.nio.file.Files

class UploadValidationTest {

    @Test
    fun testAllSucceededRequiresEveryMandatoryFile() {
        val resultsPartial = mutableMapOf<String, UploadResult>(
            "events.csv" to UploadResult.Success("sessions/1/events.csv"),
            "location.csv" to UploadResult.Success("sessions/1/location.csv")
        )
        // Partial upload missing accel.csv.gz, gyro.csv.gz, session.json, manifest.sha256 MUST fail
        assertFalse(S3Uploader.allSucceeded(resultsPartial))

        // Complete upload with every required file
        val resultsComplete = S3Uploader.UPLOAD_ORDER.associateWith { name ->
            UploadResult.Success("sessions/1/$name")
        }
        assertTrue(S3Uploader.allSucceeded(resultsComplete))
    }

    @Test
    fun testAllSucceededFailsIfAnyFileFails() {
        val resultsWithFailure = S3Uploader.UPLOAD_ORDER.associateWith { name ->
            if (name == "manifest.sha256") {
                UploadResult.Failure("Signature mismatch", false)
            } else {
                UploadResult.Success("sessions/1/$name")
            }
        }
        assertFalse(S3Uploader.allSucceeded(resultsWithFailure))
    }

    @Test
    fun testGzipCompressionAndVerification() {
        val tempDir = Files.createTempDirectory("kinesis_gzip_test").toFile()
        try {
            val rawFile = File(tempDir, "accel.csv")
            rawFile.writeText("t_ns,x,y,z,accuracy\n1000,0.1,9.8,0.2,3\n2000,0.2,9.7,0.3,3\n")

            val gzFile = File(tempDir, "accel.csv.gz")
            val compressed = SessionWriter.gzipFile(rawFile, gzFile)
            assertTrue(compressed)
            assertTrue(gzFile.exists())
            assertTrue(gzFile.length() > 0)

            val verified = SessionWriter.verifyGzip(gzFile)
            assertTrue(verified)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
