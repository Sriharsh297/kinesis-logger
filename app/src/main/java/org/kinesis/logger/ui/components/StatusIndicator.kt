package org.kinesis.logger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kinesis.logger.ui.theme.AmberWarning
import org.kinesis.logger.ui.theme.AmberWarningBg
import org.kinesis.logger.ui.theme.CyanPrimary
import org.kinesis.logger.ui.theme.GreenSuccess
import org.kinesis.logger.ui.theme.GreenSuccessBg
import org.kinesis.logger.ui.theme.RedAlert
import org.kinesis.logger.ui.theme.RedAlertBg

enum class StatusLevel {
    PASS,
    WARN,
    FAIL,
    INFO
}

@Composable
fun StatusBadge(
    text: String,
    level: StatusLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (level) {
        StatusLevel.PASS -> Triple(GreenSuccessBg, GreenSuccess, GreenSuccess)
        StatusLevel.WARN -> Triple(AmberWarningBg, AmberWarning, AmberWarning)
        StatusLevel.FAIL -> Triple(RedAlertBg, RedAlert, RedAlert)
        StatusLevel.INFO -> Triple(Color(0x2206B6D4), CyanPrimary, CyanPrimary)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
