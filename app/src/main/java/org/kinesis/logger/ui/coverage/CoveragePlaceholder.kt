package org.kinesis.logger.ui.coverage

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
import org.kinesis.logger.ui.components.KinesisSecondaryButton
import org.kinesis.logger.ui.theme.BgDark
import org.kinesis.logger.ui.theme.TextPrimary

@Composable
fun CoveragePlaceholder(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {
        Column {
            Text("Coverage Tracker (Target vs Collected)", color = TextPrimary)
            Spacer(modifier = Modifier.height(20.dp))
            KinesisSecondaryButton(text = "Back", onClick = onBack)
        }
    }
}
