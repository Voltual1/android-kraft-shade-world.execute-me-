package me.voltual.asp.engine.rendering

import androidx.compose.ui.graphics.ColorMatrix

private const val LUMA_R = 0.299f
private const val LUMA_G = 0.587f
private const val LUMA_B = 0.114f

/**
 * Port of Web buildColorEffectMatrix (Rec.601 Luma + Inversion)
 * Matched with AVG/[UC]Common/Arts/Materials/mat_grayscale
 */
fun buildColorEffectColorMatrix(gray: Float, inverse: Float): ColorMatrix {
    val scale = 1f - 2f * inverse
    val lumR = LUMA_R * gray
    val lumG = LUMA_G * gray
    val lumB = LUMA_B * gray
    val diag = 1f - gray

    val r0 = (lumR + diag) * scale
    val r1 = lumG * scale
    val r2 = lumB * scale

    val g0 = lumR * scale
    val g1 = (lumG + diag) * scale
    val g2 = lumB * scale

    val b0 = lumR * scale
    val b1 = lumG * scale
    val b2 = (lumB + diag) * scale

    val offset = inverse * 255f

    return ColorMatrix(
        floatArrayOf(
            r0, r1, r2, 0f, offset,
            g0, g1, g2, 0f, offset,
            b0, b1, b2, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}