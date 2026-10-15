package me.voltual.asp.engine.rendering.core

import kotlin.math.*

typealias EaseCurve = (Float) -> Float

val linearEase: EaseCurve = { time -> time }

private fun outBounceEase(time: Float): Float {
    val d1 = 2.75f
    return when {
        time < 1f / d1 -> 7.5625f * time * time
        time < 2f / d1 -> {
            val t = time - 1.5f / d1
            7.5625f * t * t + 0.75f
        }
        time < 2.5f / d1 -> {
            val t = time - 2.25f / d1
            7.5625f * t * t + 0.9375f
        }
        else -> {
            val t = time - 2.625f / d1
            7.5625f * t * t + 0.984375f
        }
    }
}

private const val DEFAULT_OVERSHOOT_OR_AMPLITUDE: Float = 1.70158f

private const val EASE_BACK_C1: Float = DEFAULT_OVERSHOOT_OR_AMPLITUDE
private const val EASE_BACK_C2: Float = EASE_BACK_C1 * 1.525f
private const val EASE_BACK_C3: Float = EASE_BACK_C1 + 1f

private const val TWO_PI: Float = (2.0 * Math.PI).toFloat()
private const val ELASTIC_PERIOD: Float = 0.3f
private const val ELASTIC_IN_OUT_PERIOD: Float = 0.45f

private fun elasticShift(period: Float): Float {
    return (period / TWO_PI) * asin(1f / DEFAULT_OVERSHOOT_OR_AMPLITUDE)
}

private val ELASTIC_S: Float = elasticShift(ELASTIC_PERIOD)
private val ELASTIC_IN_OUT_S: Float = elasticShift(ELASTIC_IN_OUT_PERIOD)

private fun inOutQuadEase(time: Float): Float {
    return if (time < 0.5f) {
        2f * time * time
    } else {
        1f - (-2f * time + 2f).pow(2) / 2f
    }
}

private fun flashEase(wave: EaseCurve): EaseCurve {
    return { time -> min(1f, wave(time)) }
}

val dotweenEaseCurves: Map<String, EaseCurve> = mapOf(
    "Flash" to flashEase(linearEase),
    "InBack" to { time -> EASE_BACK_C3 * time.pow(3) - EASE_BACK_C1 * time.pow(2) },
    "InBounce" to { time -> 1f - outBounceEase(1f - time) },
    "InCirc" to { time -> 1f - sqrt(1f - time * time) },
    "InCubic" to { time -> time.pow(3) },
    "InElastic" to { time ->
        if (time == 0f || time == 1f) {
            time
        } else {
            -(DEFAULT_OVERSHOOT_OR_AMPLITUDE * 2f.pow(10f * (time - 1f)) * sin(((time - 1f - ELASTIC_S) * TWO_PI) / ELASTIC_PERIOD))
        }
    },
    "InExpo" to { time -> if (time == 0f) 0f else 2f.pow(10f * time - 10f) },
    "InFlash" to flashEase { time -> time * time },
    "InOutBack" to { time ->
        if (time < 0.5f) {
            ((2f * time).pow(2) * ((EASE_BACK_C2 + 1f) * 2f * time - EASE_BACK_C2)) / 2f
        } else {
            ((2f * time - 2f).pow(2) * ((EASE_BACK_C2 + 1f) * (2f * time - 2f) + EASE_BACK_C2) + 2f) / 2f
        }
    },
    "InOutBounce" to { time ->
        if (time < 0.5f) {
            (1f - outBounceEase(1f - 2f * time)) / 2f
        } else {
            (1f + outBounceEase(2f * time - 1f)) / 2f
        }
    },
    "InOutCirc" to { time ->
        if (time < 0.5f) {
            (1f - sqrt(1f - (2f * time).pow(2))) / 2f
        } else {
            (sqrt(1f - (-2f * time + 2f).pow(2)) + 1f) / 2f
        }
    },
    "InOutCubic" to { time ->
        if (time < 0.5f) 4f * time.pow(3) else 1f - (-2f * time + 2f).pow(3) / 2f
    },
    "InOutElastic" to { time ->
        if (time == 0f || time == 1f) {
            time
        } else {
            val doubled = 2f * time - 1f
            val wave = DEFAULT_OVERSHOOT_OR_AMPLITUDE * sin(((doubled - ELASTIC_IN_OUT_S) * TWO_PI) / ELASTIC_IN_OUT_PERIOD)
            if (time < 0.5f) {
                -0.5f * (2f.pow(10f * doubled) * wave)
            } else {
                2f.pow(-10f * doubled) * wave * 0.5f + 1f
            }
        }
    },
    "InOutExpo" to { time ->
        when {
            time == 0f -> 0f
            time == 1f -> 1f
            time < 0.5f -> 2f.pow(20f * time - 10f) / 2f
            else -> (2f - 2f.pow(-20f * time + 10f)) / 2f
        }
    },
    "InOutFlash" to flashEase(::inOutQuadEase),
    "InOutQuad" to ::inOutQuadEase,
    "InOutQuart" to { time ->
        if (time < 0.5f) 8f * time.pow(4) else 1f - (-2f * time + 2f).pow(4) / 2f
    },
    "InOutQuint" to { time ->
        if (time < 0.5f) 16f * time.pow(5) else 1f - (-2f * time + 2f).pow(5) / 2f
    },
    "InOutSine" to { time -> -(cos(Math.PI.toFloat() * time) - 1f) / 2f },
    "InQuad" to { time -> time * time },
    "InQuart" to { time -> time.pow(4) },
    "InQuint" to { time -> time.pow(5) },
    "InSine" to { time -> 1f - cos((time * Math.PI.toFloat()) / 2f) },
    "Linear" to linearEase,
    "OutBack" to { time ->
        1f + EASE_BACK_C3 * (time - 1f).pow(3) + EASE_BACK_C1 * (time - 1f).pow(2)
    },
    "OutBounce" to ::outBounceEase,
    "OutCirc" to { time -> sqrt(1f - (time - 1f).pow(2)) },
    "OutCubic" to { time -> 1f - (1f - time).pow(3) },
    "OutElastic" to { time ->
        if (time == 0f || time == 1f) {
            time
        } else {
            DEFAULT_OVERSHOOT_OR_AMPLITUDE * 2f.pow(-10f * time) * sin(((time - ELASTIC_S) * TWO_PI) / ELASTIC_PERIOD) + 1f
        }
    },
    "OutExpo" to { time -> if (time == 1f) 1f else 1f - 2f.pow(-10f * time) },
    "OutFlash" to flashEase { time -> -time * (time - 2f) },
    "OutQuad" to { time -> 1f - (1f - time) * (1f - time) },
    "OutQuart" to { time -> 1f - (1f - time).pow(4) },
    "OutQuint" to { time -> 1f - (1f - time).pow(5) },
    "OutSine" to { time -> sin((time * Math.PI.toFloat()) / 2f) }
)

private val DOTWEEN_EASE_BY_ORDINAL: List<EaseCurve> = listOf(
    dotweenEaseCurves["OutQuad"]!!,
    dotweenEaseCurves["Linear"]!!,
    dotweenEaseCurves["InSine"]!!,
    dotweenEaseCurves["OutSine"]!!,
    dotweenEaseCurves["InOutSine"]!!,
    dotweenEaseCurves["InQuad"]!!,
    dotweenEaseCurves["OutQuad"]!!,
    dotweenEaseCurves["InOutQuad"]!!,
    dotweenEaseCurves["InCubic"]!!,
    dotweenEaseCurves["OutCubic"]!!,
    dotweenEaseCurves["InOutCubic"]!!,
    dotweenEaseCurves["InQuart"]!!,
    dotweenEaseCurves["OutQuart"]!!,
    dotweenEaseCurves["InOutQuart"]!!,
    dotweenEaseCurves["InQuint"]!!,
    dotweenEaseCurves["OutQuint"]!!,
    dotweenEaseCurves["InOutQuint"]!!,
    dotweenEaseCurves["InExpo"]!!,
    dotweenEaseCurves["OutExpo"]!!,
    dotweenEaseCurves["InOutExpo"]!!,
    dotweenEaseCurves["InCirc"]!!,
    dotweenEaseCurves["OutCirc"]!!,
    dotweenEaseCurves["InOutCirc"]!!,
    dotweenEaseCurves["InElastic"]!!,
    dotweenEaseCurves["OutElastic"]!!,
    dotweenEaseCurves["InOutElastic"]!!,
    dotweenEaseCurves["InBack"]!!,
    dotweenEaseCurves["OutBack"]!!,
    dotweenEaseCurves["InOutBack"]!!,
    dotweenEaseCurves["InBounce"]!!,
    dotweenEaseCurves["OutBounce"]!!,
    dotweenEaseCurves["InOutBounce"]!!,
    dotweenEaseCurves["Flash"]!!,
    dotweenEaseCurves["InFlash"]!!,
    dotweenEaseCurves["OutFlash"]!!,
    dotweenEaseCurves["InOutFlash"]!!,
    { 1f },
    linearEase
)

fun dotweenEaseCurve(ease: String?): EaseCurve {
    if (ease == null) return linearEase
    val trimmed = ease.trim()
    val ordinal = trimmed.toIntOrNull()
    if (ordinal != null) {
        return DOTWEEN_EASE_BY_ORDINAL.getOrNull(ordinal) ?: dotweenEaseCurves["OutQuad"]!!
    }
    return dotweenEaseCurves[trimmed] ?: linearEase
}