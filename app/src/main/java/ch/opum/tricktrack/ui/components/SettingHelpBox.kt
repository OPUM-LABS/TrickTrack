package ch.opum.tricktrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SettingHelpBox(
    helpText: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Hand-drawn Curved Upward Arrow Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val startX = 96f
            val endX = 72f
            val startY = size.height
            val endY = 4f

            // Smooth organic curve from startX,startY curving left to endX,endY
            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX - 8f, startY * 0.65f,
                    endX - 4f, startY * 0.35f,
                    endX, endY
                )
            }

            // Draw thicker solid organic stroke
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 3.2f,
                    cap = StrokeCap.Round
                )
            )

            // Draw open arrowhead pointing UP
            val arrowHead = Path().apply {
                moveTo(endX - 6f, endY + 8f)
                lineTo(endX, endY)
                lineTo(endX + 6f, endY + 7f)
            }
            drawPath(
                path = arrowHead,
                color = primaryColor,
                style = Stroke(
                    width = 3.2f,
                    cap = StrokeCap.Round
                )
            )
        }

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
                    tint = MaterialTheme.colorScheme.primary,
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
    }
}
