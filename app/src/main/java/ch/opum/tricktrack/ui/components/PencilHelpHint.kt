package ch.opum.tricktrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.opum.tricktrack.R

@Composable
fun PencilHelpHint(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pencilColor = LocalContentColor.current.copy(alpha = 0.70f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer(rotationZ = -2.5f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(end = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.hint_toggle_inline_help),
            fontFamily = FontFamily.Cursive,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = pencilColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(4.dp))
        PencilArrow(color = pencilColor)
    }
}

@Composable
fun PencilArrow(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 12.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Draw gentle hand-drawn arched stem
        val stemPath = Path().apply {
            moveTo(1.dp.toPx(), midY + 1.dp.toPx())
            quadraticTo(
                w * 0.45f, midY - 1.5.dp.toPx(),
                w - 2.dp.toPx(), midY
            )
        }
        drawPath(
            path = stemPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw top arrowhead barb
        val barbTop = Path().apply {
            moveTo(w - 6.5.dp.toPx(), midY - 3.5.dp.toPx())
            lineTo(w - 2.dp.toPx(), midY)
        }
        drawPath(
            path = barbTop,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw bottom arrowhead barb
        val barbBottom = Path().apply {
            moveTo(w - 6.5.dp.toPx(), midY + 3.5.dp.toPx())
            lineTo(w - 2.dp.toPx(), midY)
        }
        drawPath(
            path = barbBottom,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
