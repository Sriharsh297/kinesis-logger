package org.kinesis.logger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.kinesis.logger.ui.theme.CardBorder
import org.kinesis.logger.ui.theme.Shapes
import org.kinesis.logger.ui.theme.SurfaceDark

@Composable
fun KinesisCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CardBorder,
    backgroundColor: Color = SurfaceDark,
    shape: Shape = Shapes.medium,
    elevation: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
