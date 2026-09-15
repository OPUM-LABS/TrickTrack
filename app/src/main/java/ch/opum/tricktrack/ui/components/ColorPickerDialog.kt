package ch.opum.tricktrack.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.DialogAcceptButton
import ch.opum.tricktrack.ui.DialogDeclineButton
import ch.opum.tricktrack.ui.theme.SpecialThemeHelper
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class ColorPreset(
    val name: String,
    val colorLong: Long
)

val defaultColorPresets = listOf(
    ColorPreset("Crimson", 0xFFD32F2FL),
    ColorPreset("Blue", 0xFF1976D2L),
    ColorPreset("Green", 0xFF2E7D32L),
    ColorPreset("Violet", 0xFF6750A4L),
    ColorPreset("Orange", 0xFFE65100L),
    ColorPreset("Teal", 0xFF00796BL)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    initialColorLong: Long,
    initialIsDynamic: Boolean,
    initialSpecialTheme: String = SpecialThemeHelper.THEME_NONE,
    onDismiss: () -> Unit,
    onSave: (colorLong: Long, isDynamic: Boolean, specialTheme: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialHsv = remember(initialColorLong) { colorToHsv(initialColorLong) }
    var hue by remember(initialColorLong) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColorLong) { mutableFloatStateOf(initialHsv[1].coerceIn(0.1f, 1.0f)) }
    var value by remember(initialColorLong) { mutableFloatStateOf(initialHsv[2].coerceIn(0.3f, 1.0f)) }
    var isDynamicSelected by remember(initialIsDynamic) { mutableStateOf(initialIsDynamic) }
    var selectedSpecialTheme by remember(initialSpecialTheme) { mutableStateOf(initialSpecialTheme) }

    val currentColor = remember(hue, saturation, value) {
        hsvToColor(hue, saturation, value)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.accent_color_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color Wheel Canvas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = min(size.width, size.height) / 2f
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist <= radius) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    hue = angle
                                    saturation = (dist / radius).coerceIn(0f, 1f)
                                    isDynamicSelected = false
                                    selectedSpecialTheme = SpecialThemeHelper.THEME_NONE
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = min(size.width, size.height) / 2f
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                val dist = sqrt(dx * dx + dy * dy)
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                hue = angle
                                saturation = (dist / radius).coerceIn(0f, 1f)
                                isDynamicSelected = false
                                selectedSpecialTheme = SpecialThemeHelper.THEME_NONE
                            }
                        }
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) / 2f

                    // Draw Color Spectrum Radial Gradient
                    for (step in 0..360 step 2) {
                        val color = hsvToColor(step.toFloat(), 1.0f, value)

                        drawArc(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, color),
                                center = center,
                                radius = radius
                            ),
                            startAngle = step.toFloat(),
                            sweepAngle = 2.5f,
                            useCenter = true
                        )
                    }

                    // Selector Ring Indicator
                    val selectorRadius = saturation * radius
                    val selectorRad = Math.toRadians(hue.toDouble())
                    val selectorX = center.x + selectorRadius * cos(selectorRad).toFloat()
                    val selectorY = center.y + selectorRadius * sin(selectorRad).toFloat()

                    drawCircle(
                        color = Color.Black,
                        radius = 12.dp.toPx(),
                        center = Offset(selectorX, selectorY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = Offset(selectorX, selectorY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isSpecialActive = isDynamicSelected || selectedSpecialTheme != SpecialThemeHelper.THEME_NONE

            // Real-time Gradient Brightness / Value Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isSpecialActive) 0.38f else 1.0f)
            ) {
                Text(
                    text = stringResource(R.string.accent_color_brightness_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))

                val lowBrightnessColor = remember(hue, saturation) { hsvToColor(hue, saturation, 0.2f) }
                val maxBrightnessColor = remember(hue, saturation) { hsvToColor(hue, saturation, 1.0f) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Real-time Gradient Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(lowBrightnessColor, maxBrightnessColor)
                                )
                            )
                    )

                    // Slider overlaid with custom thumb colors
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            isDynamicSelected = false
                            selectedSpecialTheme = SpecialThemeHelper.THEME_NONE
                        },
                        enabled = !isSpecialActive,
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            disabledThumbColor = currentColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            disabledActiveTrackColor = Color.Transparent,
                            disabledInactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Presets Row
            Text(
                text = stringResource(R.string.accent_color_presets_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                defaultColorPresets.forEach { preset ->
                    val presetColor = Color(preset.colorLong)
                    val isSelected = !isDynamicSelected && selectedSpecialTheme == SpecialThemeHelper.THEME_NONE && colorMatches(currentColor, presetColor)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(presetColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                val hsv = colorToHsv(preset.colorLong)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                isDynamicSelected = false
                                selectedSpecialTheme = SpecialThemeHelper.THEME_NONE
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Dynamic Material You Option (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val isMaterialYouSelected = isDynamicSelected && selectedSpecialTheme == SpecialThemeHelper.THEME_NONE
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                width = if (isMaterialYouSelected) 3.dp else 1.dp,
                                color = if (isMaterialYouSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                isDynamicSelected = true
                                selectedSpecialTheme = SpecialThemeHelper.THEME_NONE
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMaterialYouSelected) Icons.Default.Check else Icons.Default.AutoAwesome,
                            contentDescription = "Dynamic Color",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Special Themes Row (Gradients & Daytime)
            Text(
                text = stringResource(R.string.special_themes_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpecialThemeHelper.specialPresets.forEach { preset ->
                    val isSelected = !isDynamicSelected && selectedSpecialTheme == preset.id

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(preset.previewColors)
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedSpecialTheme = preset.id
                                isDynamicSelected = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(preset.titleRes),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (preset.isDaytime) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = stringResource(preset.titleRes),
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogDeclineButton(onClick = onDismiss)
                Spacer(modifier = Modifier.width(12.dp))
                DialogAcceptButton(
                    onClick = {
                        val colorLong = hsvToLong(hue, saturation, value)
                        onSave(colorLong, isDynamicSelected, selectedSpecialTheme)
                    }
                )
            }
        }
    }
}

fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue, saturation, value)
    return Color(android.graphics.Color.HSVToColor(0xFF, hsv))
}

fun hsvToLong(hue: Float, saturation: Float, value: Float): Long {
    val hsv = floatArrayOf(hue, saturation, value)
    val argb = android.graphics.Color.HSVToColor(0xFF, hsv)
    return argb.toLong() and 0xFFFFFFFFL
}

fun colorToHsv(colorLong: Long): FloatArray {
    val argb = colorLong.toInt()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

fun colorMatches(c1: Color, c2: Color): Boolean {
    val hsv1 = FloatArray(3)
    val hsv2 = FloatArray(3)
    android.graphics.Color.colorToHSV(c1.toArgb(), hsv1)
    android.graphics.Color.colorToHSV(c2.toArgb(), hsv2)
    return abs(hsv1[0] - hsv2[0]) < 10f && abs(hsv1[1] - hsv2[1]) < 0.2f
}
