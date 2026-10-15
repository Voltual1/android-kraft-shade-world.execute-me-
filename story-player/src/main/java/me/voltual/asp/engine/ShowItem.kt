package me.voltual.asp.engine

import kotlin.math.max
import kotlin.math.min

private const val LEGACY_STORY_WIDTH = 960f
private const val LEGACY_STORY_HEIGHT = 540f
private const val LEGACY_SHOWITEM_CONTENT_SCALE = 0.6f
private const val LEGACY_SHOWITEM_BORDER_PX = 7.5f

data class LegacyShowItemLayout(
    val borderPx: Float,
    val contentHeight: Float,
    val contentWidth: Float,
    val scale: Float
)

fun computeLegacyShowItemLayout(sourceWidth: Float, sourceHeight: Float): LegacyShowItemLayout {
    val width = max(1f, sourceWidth)
    val height = max(1f, sourceHeight)
    val viewportScale = min(
        STORY_WIDTH.toFloat() / LEGACY_STORY_WIDTH,
        STORY_HEIGHT.toFloat() / LEGACY_STORY_HEIGHT
    )
    val scale = LEGACY_SHOWITEM_CONTENT_SCALE * viewportScale

    return LegacyShowItemLayout(
        borderPx = LEGACY_SHOWITEM_BORDER_PX * viewportScale,
        contentHeight = height * scale,
        contentWidth = width * scale,
        scale = scale
    )
}