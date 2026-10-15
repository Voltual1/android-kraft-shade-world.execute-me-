package me.voltual.asp.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ShakePoint(
    val durationRatio: Float,
    val x: Float,
    val y: Float
)

data class ShakePathInput(
    val fadeOut: Boolean,
    val randomness: Float,
    val vibrato: Int,
    val xStrength: Float,
    val yStrength: Float
)

object StoryShakePath {

    private fun randomRange(min: Float, max: Float): Float {
        return min + (max - min) * Random.nextFloat()
    }

    fun buildShakePath(
        durationMs: Long,
        input: ShakePathInput
    ): List<ShakePoint> {
        val durationSeconds = durationMs.coerceAtLeast(0) / 1000f
        val count = Math.max(2, (input.vibrato.coerceAtLeast(0) * durationSeconds).toInt())
        val randomness = input.randomness.coerceAtLeast(0f)
        val rawDurations = (0 until count).map { index ->
            if (input.fadeOut) index + 1f else 1f
        }
        val durationTotal = rawDurations.sum()
        var angle = randomRange(0f, 360f)

        return rawDurations.mapIndexed { index, rawDuration ->
            if (index == count - 1) {
                return@mapIndexed ShakePoint(durationRatio = rawDuration / durationTotal, x = 0f, y = 0f)
            }

            if (index > 0) {
                angle = angle - 180f + randomRange(-randomness, randomness)
            }

            val radians = Math.toRadians(angle.toDouble()).toFloat()
            val tilt = Math.toRadians(randomRange(-randomness, randomness).toDouble()).toFloat()
            val fade = if (input.fadeOut) 1f - (index.toFloat() / count) else 1f
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
    ): Pair<Float, Float> {
        val target = progress.coerceIn(0f, 1f)
        var elapsed = 0f
        var fromX = 0f
        var fromY = 0f

        for (point in path) {
            val end = elapsed + point.durationRatio
            if (target <= end || point == path.last()) {
                val localProgress = if (point.durationRatio > 0f) {
                    ((target - elapsed) / point.durationRatio).coerceIn(0f, 1f)
                } else 1f
                return Pair(
                    fromX + (point.x - fromX) * localProgress,
                    fromY + (point.y - fromY) * localProgress
                )
            }
            elapsed = end
            fromX = point.x
            fromY = point.y
        }

        return Pair(0f, 0f)
    }
}