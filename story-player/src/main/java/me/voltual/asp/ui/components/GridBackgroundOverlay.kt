package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.Vec2
import me.voltual.asp.engine.rendering.GridBackgroundVisualState
import me.voltual.asp.engine.resolveStoryAssetByKey
import kotlin.math.roundToInt

private data class TileSpec(
    val url: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private data class LayoutResult(
    val tiles: List<TileSpec>,
    val totalWidth: Float,
    val totalHeight: Float,
    val pivotX: Float,
    val pivotY: Float
)

/**
 * Port of LargeBackgroundPanel.POSITION_INIT_FUNCTION as applied by AVG native client
 * and Web SceneGeometry.ts
 */
private fun calculateInitOffset(gridBg: GridBackgroundVisualState): Vec2 {
    val mode = gridBg.initPositionMode

    if (gridBg.layout == "vertical") {
        val h0 = gridBg.solidHeights.getOrNull(0) ?: 0f
        val h1 = gridBg.solidHeights.getOrNull(1) ?: 0f
        val heightSum = h0 + h1
        val w0 = gridBg.solidWidths.getOrNull(0) ?: 0f
        return when (mode) {
            "center" -> Vec2(0f, 0f)
            "upperleft" -> Vec2((w0 - STORY_WIDTH) / 2f, (STORY_HEIGHT - heightSum) / 2f)
            "lowercenter" -> Vec2(0f, (heightSum - STORY_HEIGHT) / 2f)
            else -> Vec2(0f, -h1 / 2f) // default mode
        }
    }

    val widths = gridBg.solidWidths

    if (gridBg.layout == "grid") {
        val h0 = gridBg.solidHeights.getOrNull(0) ?: 0f
        val h1 = gridBg.solidHeights.getOrNull(1) ?: 0f
        val totalHeight = h0 + h1
        val w0 = widths.getOrNull(0) ?: 0f
        val w1 = widths.getOrNull(1) ?: 0f
        return when (mode) {
            "center" -> Vec2(0f, 0f)
            "upperleft" -> Vec2((w0 + w1 - STORY_WIDTH) / 2f, (STORY_HEIGHT - totalHeight) / 2f)
            "lowercenter" -> Vec2(0f, (totalHeight - STORY_HEIGHT) / 2f)
            else -> Vec2(w1 / 2f, -h1 / 2f) // default mode
        }
    }

    if (gridBg.layout != "large") return Vec2(0f, 0f)

    val height = gridBg.solidHeights.getOrNull(0) ?: 0f
    val w1 = widths.getOrNull(1) ?: 0f
    val sumWidth = widths.sum()
    return when (mode) {
        "center" -> Vec2(0f, 0f)
        "upperleft" -> Vec2((sumWidth - STORY_WIDTH) / 2f, (STORY_HEIGHT - height) / 2f)
        "lowercenter" -> Vec2(0f, (height - STORY_HEIGHT) / 2f)
        else -> Vec2(w1 / 2f, 0f) // default mode
    }
}

private fun calculateLayout(gridBg: GridBackgroundVisualState): LayoutResult {
    val family = if (gridBg.assetKind == "image") StoryAssetFamily.IMAGE else StoryAssetFamily.BACKGROUND
    val tiles = mutableListOf<TileSpec>()

    when (gridBg.layout) {
        "vertical" -> {
            val width = gridBg.solidWidths.getOrNull(0) ?: STORY_WIDTH.toFloat()
            var currentY = 0f
            gridBg.imageKeys.forEachIndexed { index, key ->
                val h = gridBg.solidHeights.getOrNull(index) ?: STORY_HEIGHT.toFloat()
                val url = resolveStoryAssetByKey(key, family)
                if (url != null) {
                    tiles.add(TileSpec(url = url, x = 0f, y = currentY, width = width, height = h))
                }
                currentY += h
            }
            val pivotHeight = (gridBg.solidHeights.getOrNull(0) ?: 0f) + (gridBg.solidHeights.getOrNull(1) ?: 0f)
            return LayoutResult(
                tiles = tiles,
                totalWidth = width,
                totalHeight = currentY,
                pivotX = width / 2f,
                pivotY = pivotHeight / 2f
            )
        }
        "large" -> {
            val height = gridBg.solidHeights.getOrNull(0) ?: STORY_HEIGHT.toFloat()
            var currentX = 0f
            gridBg.imageKeys.forEachIndexed { index, key ->
                val w = gridBg.solidWidths.getOrNull(index) ?: (STORY_WIDTH / 2f)
                val url = resolveStoryAssetByKey(key, family)
                if (url != null) {
                    tiles.add(TileSpec(url = url, x = currentX, y = 0f, width = w, height = height))
                }
                currentX += w
            }
            val totalW = (gridBg.solidWidths.getOrNull(0) ?: 0f) + (gridBg.solidWidths.getOrNull(1) ?: 0f)
            return LayoutResult(
                tiles = tiles,
                totalWidth = if (totalW > 0f) totalW else currentX,
                totalHeight = height,
                pivotX = (if (totalW > 0f) totalW else currentX) / 2f,
                pivotY = height / 2f
            )
        }
        else -> {
            // 2x2 grid layout
            val w0 = gridBg.solidWidths.getOrNull(0) ?: (STORY_WIDTH / 2f)
            val w1 = gridBg.solidWidths.getOrNull(1) ?: (STORY_WIDTH / 2f)
            val h0 = gridBg.solidHeights.getOrNull(0) ?: (STORY_HEIGHT / 2f)
            val h1 = gridBg.solidHeights.getOrNull(1) ?: (STORY_HEIGHT / 2f)

            val totalW = w0 + w1
            val totalH = h0 + h1

            gridBg.imageKeys.forEachIndexed { index, key ->
                val col = index % 2
                val row = index / 2
                val tileX = if (col == 0) 0f else w0
                val tileY = if (row == 0) 0f else h0
                val tileW = if (col == 0) w0 else w1
                val tileH = if (row == 0) h0 else h1

                val url = resolveStoryAssetByKey(key, family)
                if (url != null) {
                    tiles.add(TileSpec(url = url, x = tileX, y = tileY, width = tileW, height = tileH))
                }
            }

            return LayoutResult(
                tiles = tiles,
                totalWidth = totalW,
                totalHeight = totalH,
                pivotX = totalW / 2f,
                pivotY = totalH / 2f
            )
        }
    }
}

@Composable
fun GridBackgroundOverlay(
    gridBg: GridBackgroundVisualState?,
    modifier: Modifier = Modifier
) {
    if (gridBg == null || gridBg.imageKeys.isEmpty() || gridBg.alpha <= 0f) return

    val initOffset = calculateInitOffset(gridBg)
    val layout = calculateLayout(gridBg)

    // Stage placement formula aligned with Web PixiStoryRenderer:
    // x = STORY_WIDTH / 2 + initOffset.x + input.x
    // y = STORY_HEIGHT / 2 - initOffset.y - input.y
    val posX = STORY_WIDTH / 2f + initOffset.x + gridBg.x
    val posY = STORY_HEIGHT / 2f - initOffset.y - gridBg.y

    val topLeftX = posX - layout.pivotX
    val topLeftY = posY - layout.pivotY

    val originPivotXRatio = if (layout.totalWidth > 0f) (layout.pivotX / layout.totalWidth).coerceIn(0f, 1f) else 0.5f
    val originPivotYRatio = if (layout.totalHeight > 0f) (layout.pivotY / layout.totalHeight).coerceIn(0f, 1f) else 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(gridBg.alpha.coerceIn(0f, 1f))
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt())
                }
                .size(layout.totalWidth.dp, layout.totalHeight.dp)
                .graphicsLayer {
                    scaleX = gridBg.scaleX
                    scaleY = gridBg.scaleY
                    transformOrigin = TransformOrigin(originPivotXRatio, originPivotYRatio)
                }
        ) {
            layout.tiles.forEach { tile ->
                AsyncImage(
                    model = tile.url,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset(tile.x.dp, tile.y.dp)
                        .size(tile.width.dp, tile.height.dp)
                )
            }
        }
    }
}