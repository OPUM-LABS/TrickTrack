package ch.opum.tricktrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class SnowObstacleRegistry {
    val obstacles = mutableStateMapOf<String, Rect>()

    fun updateObstacle(id: String, bounds: Rect) {
        obstacles[id] = bounds
    }

    fun removeObstacle(id: String) {
        obstacles.remove(id)
    }

    private val clearedObstacles = mutableStateMapOf<String, Long>()

    fun clearObstacle(id: String) {
        clearedObstacles[id] = System.currentTimeMillis()
    }

    fun isCleared(id: String, sinceTime: Long): Boolean {
        val clearTime = clearedObstacles[id] ?: return false
        return clearTime >= sinceTime
    }
}

val LocalSnowObstacleRegistry = compositionLocalOf<SnowObstacleRegistry?> { null }

@Composable
fun Modifier.snowObstacle(id: String): Modifier {
    val registry = LocalSnowObstacleRegistry.current ?: return this
    DisposableEffect(id) {
        onDispose {
            registry.removeObstacle(id)
        }
    }
    return this
        .pointerInput(id) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                    registry.clearObstacle(id)
                }
            }
        }
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                registry.updateObstacle(id, coordinates.boundsInRoot())
            }
        }
}

private class Snowflake(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 3f,
    var speedY: Float = 2f,
    var driftAngle: Float = 0f,
    var driftSpeed: Float = 0.02f,
    var driftAmplitude: Float = 1.2f,
    var alpha: Float = 0.8f,
    var restingOnId: String? = null,
    var restingY: Float = 0f,
    var restingOffsetX: Float = 0f,
    var restingStartTime: Long = 0L,
    var isMelting: Boolean = false,
    var meltProgress: Float = 0f
) {
    fun reset(width: Float, height: Float, startAnywhereY: Boolean = false) {
        x = Random.nextFloat() * width
        y = if (startAnywhereY) Random.nextFloat() * height else -10f - Random.nextFloat() * 30f
        radius = 2.5f + Random.nextFloat() * 2.5f
        speedY = 1.2f + Random.nextFloat() * 1.8f
        driftAngle = Random.nextFloat() * 6.28f
        driftSpeed = 0.015f + Random.nextFloat() * 0.025f
        driftAmplitude = 0.8f + Random.nextFloat() * 1.2f
        alpha = 0.65f + Random.nextFloat() * 0.35f
        restingOnId = null
        restingY = 0f
        restingOffsetX = 0f
        restingStartTime = 0L
        isMelting = false
        meltProgress = 0f
    }
}

@Composable
fun SnowOverlay(
    isWinterModeEnabled: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isWinterModeEnabled) return

    val registry = LocalSnowObstacleRegistry.current
    val flakeCount = 45
    val flakes = remember {
        List(flakeCount) { Snowflake() }
    }

    var isInitialized by remember { mutableStateOf(false) }
    var frameTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(isWinterModeEnabled) {
        while (true) {
            withFrameNanos { time ->
                frameTrigger = time
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        if (!isInitialized) {
            flakes.forEach { it.reset(width, height, startAnywhereY = true) }
            isInitialized = true
        }

        // Count resting flakes per obstacle so boxes don't get overloaded
        val restingCounts = mutableMapOf<String, Int>()
        flakes.forEach { flake ->
            flake.restingOnId?.let { id ->
                restingCounts[id] = (restingCounts[id] ?: 0) + 1
            }
        }

        // Palette for Dark vs Light theme
        val darkCoreColor = Color.White
        val darkHaloColor = Color(0xFFE1F5FE)

        val lightCoreColor = Color(0xFFF0F8FF)
        val lightRimColor = Color(0xFF64B5F6)
        val lightShadowColor = Color(0x30000000)

        // Read frameTrigger to ensure Canvas redraws on each animation frame
        if (frameTrigger < 0L) return@Canvas

        for (flake in flakes) {
            val restingId = flake.restingOnId
            if (restingId != null) {
                val obstacleBounds = registry?.obstacles?.get(restingId)

                // If obstacle disappeared, moved (scrolled), or was cleared: trigger melt
                val hasMoved = obstacleBounds == null || abs(obstacleBounds.top - flake.restingY) > 3f
                val wasCleared = registry?.isCleared(restingId, flake.restingStartTime) == true

                if (hasMoved || wasCleared) {
                    flake.isMelting = true
                }

                if (flake.isMelting) {
                    flake.meltProgress += 0.06f
                    if (flake.meltProgress >= 1f) {
                        flake.reset(width, height, startAnywhereY = false)
                    }
                } else if (obstacleBounds != null) {
                    // Stay attached to the obstacle's top edge
                    flake.y = obstacleBounds.top
                    flake.x = (obstacleBounds.left + flake.restingOffsetX).coerceIn(
                        obstacleBounds.left + flake.radius,
                        obstacleBounds.right - flake.radius
                    )
                }
            } else {
                // Falling physics
                flake.driftAngle += flake.driftSpeed
                flake.x += sin(flake.driftAngle) * flake.driftAmplitude
                flake.y += flake.speedY

                // Check collision with obstacles
                if (registry != null) {
                    for ((id, rect) in registry.obstacles) {
                        // Check if flake touches the top boundary of an obstacle
                        val inHorizontalBounds = flake.x in (rect.left + 8f)..(rect.right - 8f)
                        val inVerticalThreshold = flake.y >= rect.top - flake.speedY && flake.y <= rect.top + 4f
                        val currentCount = restingCounts[id] ?: 0

                        if (inHorizontalBounds && inVerticalThreshold && currentCount < 6) {
                            flake.restingOnId = id
                            flake.restingY = rect.top
                            flake.restingOffsetX = flake.x - rect.left
                            flake.restingStartTime = System.currentTimeMillis()
                            flake.y = rect.top
                            restingCounts[id] = currentCount + 1
                            break
                        }
                    }
                }

                // Wrap or reset
                if (flake.y > height + 20f) {
                    flake.reset(width, height, startAnywhereY = false)
                }
                if (flake.x < -15f) flake.x = width + 15f
                if (flake.x > width + 15f) flake.x = -15f
            }

            // Draw the snowflake
            val effectiveAlpha = if (flake.isMelting) {
                (flake.alpha * (1f - flake.meltProgress)).coerceIn(0f, 1f)
            } else {
                flake.alpha
            }

            val center = Offset(flake.x, flake.y)
            if (isDarkTheme) {
                // Soft glow halo
                drawCircle(
                    color = darkHaloColor.copy(alpha = effectiveAlpha * 0.35f),
                    radius = flake.radius * 1.5f,
                    center = center
                )
                // Crisp core
                drawCircle(
                    color = darkCoreColor.copy(alpha = effectiveAlpha),
                    radius = flake.radius,
                    center = center
                )
            } else {
                // Soft shadow for contrast on white cards
                drawCircle(
                    color = lightShadowColor.copy(alpha = effectiveAlpha * 0.4f),
                    radius = flake.radius * 1.3f,
                    center = Offset(flake.x, flake.y + 0.8f)
                )
                // Frosty ice-blue rim
                drawCircle(
                    color = lightRimColor.copy(alpha = effectiveAlpha * 0.85f),
                    radius = flake.radius * 1.15f,
                    center = center
                )
                // White/icy core
                drawCircle(
                    color = lightCoreColor.copy(alpha = effectiveAlpha),
                    radius = flake.radius * 0.85f,
                    center = center
                )
            }
        }
    }
}
