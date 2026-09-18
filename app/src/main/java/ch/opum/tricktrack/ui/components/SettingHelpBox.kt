package ch.opum.tricktrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SettingHelpBox(
    helpText: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp)
    ) {
        // Styled Help Text Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = helpText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Curved arrow branching out from the left edge of the box and pointing up-right towards the topic
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val startX = 0f
            val startY = 8.dp.toPx()

            val cp1x = (-8).dp.toPx()
            val cp1y = 10.dp.toPx()

            val cp2x = (-10).dp.toPx()
            val cp2y = (-6).dp.toPx()

            val tipX = (-2).dp.toPx()
            val tipY = (-14).dp.toPx()

            // Stem: curves out to the left margin, then sweeps up and arcs toward the topic
            val stemPath = Path().apply {
                moveTo(startX, startY)
                cubicTo(cp1x, cp1y, cp2x, cp2y, tipX, tipY)
            }

            drawPath(
                path = stemPath,
                color = primaryColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Dynamic arrowhead aligned with the curve tangent, tilted counter-clockwise
            // so both arrow wings are clearly visible and branch symmetrically relative to the curve.
            val dx = tipX - cp2x
            val dy = tipY - cp2y
            val length = hypot(dx, dy)
            if (length > 0f) {
                val rawUx = dx / length
                val rawUy = dy / length

                // Tilt arrowhead ~18 degrees counter-clockwise in screen coordinates
                val tiltRad = Math.toRadians(18.0).toFloat()
                val cosT = cos(tiltRad)
                val sinT = sin(tiltRad)
                val ux = rawUx * cosT + rawUy * sinT
                val uy = -rawUx * sinT + rawUy * cosT

                val bx = -ux
                val by = -uy

                val nx = -by

                val barbLen = 5.5.dp.toPx()
                val barbWidth = 3.5.dp.toPx()

                val leftBarbX = tipX + barbLen * bx + barbWidth * nx
                val leftBarbY = tipY + barbLen * by + barbWidth * bx

                val rightBarbX = tipX + barbLen * bx - barbWidth * nx
                val rightBarbY = tipY + barbLen * by - barbWidth * bx

                val arrowHead = Path().apply {
                    moveTo(leftBarbX, leftBarbY)
                    lineTo(tipX, tipY)
                    lineTo(rightBarbX, rightBarbY)
                }

                drawPath(
                    path = arrowHead,
                    color = primaryColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
