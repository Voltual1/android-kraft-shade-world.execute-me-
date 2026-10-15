package me.voltual.asp.engine.rendering.core

import me.voltual.asp.engine.CameraShakeInput
import me.voltual.asp.engine.Vec2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

data class ShakePoint(
    val durationRatio: Float,
    val x: Float,
    val y: Float
)

private fun randomRange(min: Float, max: Float, random: () -> Float): Float {
    return min + (max - min) * random()
}

fun buildShakePath(
    durationMs: Long,
    input: CameraShakeInput,
    random: () -> Float = { Random.nextFloat() }
): List<ShakePoint> {
    val durationSeconds = max(0L, durationMs).toFloat() / 1000f
    val count = max(2, (max(0, input.vibrato).toFloat() * durationSeconds).toInt())
    val randomness = max(0f, input.randomness)
    val rawDurations = List(count) { index ->
        if (input.fadeOut) (index + 1).toFloat() else 1f
    }
    val durationTotal = rawDurations.sum()
    var angle = randomRange(0f, 360f, random)

    return rawDurations.mapIndexed { index, rawDuration ->
        if (index == count - 1) {
            return@mapIndexed ShakePoint(durationRatio = rawDuration / durationTotal, x = 0f, y = 0f)
        }

        if (index > 0) {
            angle = angle - 180f + randomRange(-randomness, randomness, random)
        }

        val radians = (angle * Math.PI.toFloat()) / 180f
        val tilt = (randomRange(-randomness, randomness, random) * Math.PI.toFloat()) / 180f
        val fade = if (input.fadeOut) 1f - index.toFloat() / count.toFloat() else 1f

        ShakePoint(
            durationRatio = rawDuration / durationTotal,
            x = cos(radians) * cos(tilt) * input.xStrength * fade,
            y = sin(radians) * input.yStrength * fade
        )
    }
}

fun sampleShakePath(
    path: List<ShakePoint>,
    progress: Float
): Vec2 {
    val target = progress.coerceIn(0f, 1f)
    var elapsed = 0f
    var fromX = 0f
    var fromY = 0f

    for (point in path) {
        val end = elapsed + point.durationRatio
        if (target <= end || point == path.last()) {
            val localProgress = if (point.durationRatio > 0f) {
                ((target - elapsed) / point.durationRatio).coerceIn(0f, 1f)
            } else {
                1f
            }
            return Vec2(
                x = fromX + (point.x - fromX) * localProgress,
                y = fromY + (point.y - fromY) * localProgress
            )
        }
        elapsed = end
        fromX = point.x
        fromY = point.y
    }

    return Vec2(0f, 0f)
}