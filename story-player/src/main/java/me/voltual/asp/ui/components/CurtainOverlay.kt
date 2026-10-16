package me.voltual.asp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.Vec2
import me.voltual.asp.engine.rendering.CurtainVisualState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val CURTAIN_GRADIENT_PX = 20f

private val STAGE_CORNERS = listOf(
    Vec2(0f, 0f),
    Vec2(STORY_WIDTH.toFloat(), 0f),
    Vec2(STORY_WIDTH.toFloat(), STORY_HEIGHT.toFloat()),
    Vec2(0f, STORY_HEIGHT.toFloat())
)

private val DIRECTION_ANGLES = mapOf(
    0 to 180f,
    1 to 225f,
    2 to 270f,
    3 to 315f,
    4 to 0f,
    5 to 45f,
    6 to 90f,
    7 to 135f
)

private fun resolveCurtainVector(direction: Int): Vec2? {
    val angleDeg = DIRECTION_ANGLES[direction] ?: return null
    val radians = Math.toRadians(angleDeg.toDouble()).toFloat()
    return Vec2(
        x = sin(radians),
        y = -cos(radians)
    )
}

private fun isCurtainPointInside(point: Vec2, vector: Vec2, threshold: Float): Boolean {
    return point.x * vector.x + point.y * vector.y <= threshold + 0.0001f
}

private fun intersectCurtainEdge(
    start: Vec2,
    end: Vec2,
    vector: Vec2,
    threshold: Float
): Vec2 {
    val startProj = start.x * vector.x + start.y * vector.y
    val endProj = end.x * vector.x + end.y * vector.y
    val delta = endProj - startProj
    if (abs(delta) <= 0.0001f) return start

    val ratio = (threshold - startProj) / delta
    return Vec2(
        x = start.x + (end.x - start.x) * ratio,
        y = start.y + (end.y - start.y) * ratio
    )
}

private fun clipCurtainPolygon(
    polygon: List<Vec2>,
    vector: Vec2,
    threshold: Float
): List<Vec2> {
    if (polygon.isEmpty()) return emptyList()
    val output = mutableListOf<Vec2>()
    var previous = polygon.last()
    var previousInside = isCurtainPointInside(previous, vector, threshold)

    for (current in polygon) {
        val currentInside = isCurtainPointInside(current, vector, threshold)
        if (currentInside != previousInside) {
            output.add(intersectCurtainEdge(previous, current, vector, threshold))
        }
        if (currentInside) {
            output.add(current)
        }
        previous = current
        previousInside = currentInside
    }
    return output
}

private fun buildCurtainBand(
    vector: Vec2,
    low: Float?,
    high: Float
): List<Vec2> {
    var polygon = clipCurtainPolygon(STAGE_CORNERS, vector, high)
    if (low != null) {
        polygon = clipCurtainPolygon(polygon, Vec2(-vector.x, -vector.y), -low)
    }
    return if (polygon.size >= 3) polygon else emptyList()
}

@Composable
fun CurtainOverlay(
    curtains: Map<Int, CurtainVisualState>,
    modifier: Modifier = Modifier
) {
    if (curtains.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        curtains.values.forEach { curtain ->
            if (curtain.alpha <= 0f || curtain.fill <= 0f) return@forEach
            val vector = resolveCurtainVector(curtain.direction) ?: return@forEach

            val clampedFill = curtain.fill.coerceIn(0f, 1f)
            val projections = STAGE_CORNERS.map { it.x * vector.x + it.y * vector.y }
            val minProj = projections.minOrNull() ?: 0f
            val maxProj = projections.maxOrNull() ?: 0f
            if (abs(maxProj - minProj) <= 0.0001f) return@forEach

            val threshold = minProj + (maxProj - minProj) * clampedFill
            val bodyEnd = if (curtain.grad) threshold - CURTAIN_GRADIENT_PX else threshold

            // Solid black body
            val bodyPoints = buildCurtainBand(vector, null, bodyEnd)
            if (bodyPoints.size >= 3) {
                val path = Path().apply {
                    moveTo(bodyPoints[0].x, bodyPoints[0].y)
                    for (i in 1 until bodyPoints.size) {
                        lineTo(bodyPoints[i].x, bodyPoints[i].y)
                    }
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = curtain.alpha.coerceIn(0f, 1f))
                )
            }

            // Feather gradient band
            if (curtain.grad) {
                val featherPoints = buildCurtainBand(vector, bodyEnd, threshold)
                if (featherPoints.size >= 3) {
                    val featherPath = Path().apply {
                        moveTo(featherPoints[0].x, featherPoints[0].y)
                        for (i in 1 until featherPoints.size) {
                            lineTo(featherPoints[i].x, featherPoints[i].y)
                        }
                        close()
                    }
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = curtain.alpha.coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        start = Offset(vector.x * bodyEnd, vector.y * bodyEnd),
                        end = Offset(vector.x * threshold, vector.y * threshold)
                    )
                    drawPath(
                        path = featherPath,
                        brush = brush
                    )
                }
            }
        }
    }
}