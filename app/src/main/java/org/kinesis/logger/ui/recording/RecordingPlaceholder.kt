package org.kinesis.logger.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kinesis.logger.model.Task
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.TextPrimary

@Composable
fun RecordingPlaceholder(
    task: Task,
    subjectId: String,
    onFinishRecording: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {
        Column {
            Text("Recording: ${task.taskId} ($subjectId)", color = TextPrimary)
            Spacer(modifier = Modifier.height(20.dp))
            KinesisPrimaryButton(text = "Complete Session", onClick = onFinishRecording)
        }
    }
}
