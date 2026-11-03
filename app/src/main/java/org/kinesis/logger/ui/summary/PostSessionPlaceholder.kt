package org.kinesis.logger.ui.summary

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
import org.kinesis.logger.ui.components.KinesisPrimaryButton
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.TextPrimary

@Composable
fun PostSessionPlaceholder(
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {
        Column {
            Text("Session Summary", color = TextPrimary)
            Spacer(modifier = Modifier.height(20.dp))
            KinesisPrimaryButton(text = "Return to Dashboard", onClick = onDone)
        }
    }
}
