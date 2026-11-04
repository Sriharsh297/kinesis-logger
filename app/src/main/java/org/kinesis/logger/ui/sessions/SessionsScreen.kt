package org.kinesis.logger.ui.sessions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kinesis.logger.KinesisApplication
import org.kinesis.logger.quality.SessionExporter
import org.kinesis.logger.ui.components.KinesisCard
import org.kinesis.logger.ui.components.KinesisSecondaryButton
import org.kinesis.logger.ui.components.StatusBadge
import org.kinesis.logger.ui.components.StatusLevel
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.CardBorder
import org.kinesis.logger.ui.theme.CyanPrimary
import org.kinesis.logger.ui.theme.RedAlert
import org.kinesis.logger.ui.theme.SurfaceDark
import org.kinesis.logger.ui.theme.SurfaceElevated
import org.kinesis.logger.ui.theme.TextMuted
import org.kinesis.logger.ui.theme.TextPrimary
import org.kinesis.logger.ui.theme.TextSecondary
import org.kinesis.logger.upload.UploadQueueManager
import org.kinesis.logger.upload.UploadStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SessionItem(
    val directory: File,
    val sessionId: String,
    val dateString: String,
    val sizeMb: Double,
    val isInterrupted: Boolean,
    val uploadStatus: UploadStatus
)

@Composable
fun SessionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uploadQueue = remember {
        (context.applicationContext as KinesisApplication).uploadQueue
    }
    var sessions by remember { mutableStateOf<List<SessionItem>>(emptyList()) }

    fun refreshSessions() {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val rawDir = File(baseDir, "raw")
        if (rawDir.exists() && rawDir.isDirectory) {
            val items = rawDir.listFiles { file -> file.isDirectory }?.map { dir ->
                val interrupted = File(dir, "interrupted.flag").exists()
                val totalBytes = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                val sizeMb = (totalBytes / (1024.0 * 1024.0) * 10.0).toInt() / 10.0
                val lastModified = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(dir.lastModified()))
                val uploadStatus = if (interrupted) UploadStatus.LOCAL
                                   else uploadQueue.getStatus(dir)
                SessionItem(
                    directory     = dir,
                    sessionId     = dir.name,
                    dateString    = lastModified,
                    sizeMb        = sizeMb,
                    isInterrupted = interrupted,
                    uploadStatus  = uploadStatus
                )
            }?.sortedByDescending { it.directory.lastModified() } ?: emptyList()
            sessions = items
        }
    }

    LaunchedEffect(Unit) { refreshSessions() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // App Bar with 48dp touch targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "RECORDED SESSIONS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${sessions.size} sessions • ${sessions.count { it.uploadStatus == UploadStatus.UPLOADED }} uploaded to S3",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Global upload pending button
            IconButton(
                onClick = {
                    val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
                    val rawDir = File(baseDir, "raw")
                    if (rawDir.exists()) {
                        uploadQueue.drainQueue(rawDir)
                    }
                    Toast.makeText(context, "Queued pending uploads (Wi-Fi required)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload all pending",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No sessions recorded yet.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete a bout from the dashboard to log raw data.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions, key = { it.sessionId }) { session ->
                    KinesisCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = SurfaceDark,
                        borderColor = CardBorder
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status badge
                                val (badgeText, badgeLevel) = when {
                                    session.isInterrupted -> "INTERRUPTED" to StatusLevel.WARN
                                    session.uploadStatus == UploadStatus.UPLOADED -> "UPLOADED" to StatusLevel.PASS
                                    session.uploadStatus == UploadStatus.UPLOADING -> "UPLOADING…" to StatusLevel.INFO
                                    session.uploadStatus == UploadStatus.FAILED -> "UPLOAD FAILED" to StatusLevel.FAIL
                                    else -> "LOCAL" to StatusLevel.INFO
                                }
                                StatusBadge(text = badgeText, level = badgeLevel)
                                Text(
                                    text = "${session.sizeMb} MB",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = session.sessionId,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = session.dateString,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            // Upload failure note
                            uploadQueue.getStatusNote(session.directory)?.let { note ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note,
                                    fontSize = 11.sp,
                                    color = RedAlert
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Upload / Retry button
                                if (session.uploadStatus != UploadStatus.UPLOADED && !session.isInterrupted) {
                                    IconButton(
                                        onClick = {
                                            uploadQueue.enqueue(session.directory)
                                            refreshSessions()
                                            Toast.makeText(context, "Upload queued (Wi-Fi required)", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload to S3",
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = {
                                        session.directory.deleteRecursively()
                                        refreshSessions()
                                        Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = RedAlert.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                KinesisSecondaryButton(
                                    text = "Export ZIP",
                                    onClick = {
                                        val shareIntent = SessionExporter.exportSessionZip(context, session.directory)
                                        if (shareIntent != null) {
                                            context.startActivity(shareIntent)
                                        } else {
                                            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.height(38.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
