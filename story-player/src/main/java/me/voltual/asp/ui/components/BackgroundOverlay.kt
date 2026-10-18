package me.voltual.asp.ui.components

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.toBitmap
import me.voltual.asp.Context
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.rendering.BackgroundVisualState
import me.voltual.asp.engine.resolveStoryAssetByKey
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 依据 Web 端 nativeBackgroundRect 计算官方 Native 显示矩形：
 * - 仅当资产属于 BACKGROUND 时才应用 background.json 的 PPU 或 16:9 铺满；
 * - IMAGE 插图（如 avg_0_1, avg_0_2 等）严格保持自身的实际像素尺寸，不被拉伸也不填充伪背景。
 */
private fun calculateNativeRect(
    key: String,
    rawWidth: Float,
    rawHeight: Float,
    family: StoryAssetFamily,
    backgroundPpuMap: Map<String, Float>?
): Pair<Float, Float> {
    if (family == StoryAssetFamily.BACKGROUND) {
        val ppu = backgroundPpuMap?.get(key.lowercase())
        if (ppu != null && ppu > 0f) {
            return Pair((rawWidth / ppu) * 100f, (rawHeight / ppu) * 100f)
        }
        if (abs(rawWidth / rawHeight - STORY_WIDTH.toFloat() / STORY_HEIGHT.toFloat()) < 0.01f) {
            return Pair(STORY_WIDTH.toFloat(), STORY_HEIGHT.toFloat())
        }
    }
    return Pair(rawWidth, rawHeight)
}

/**
 * 依据 Web 端 layoutImageForScreenAdapt 计算适配尺寸：
 * 仅在明确传入 screenAdapt 参数时才做缩放，否则保持原尺寸。
 */
private fun layoutForScreenAdapt(
    nativeWidth: Float,
    nativeHeight: Float,
    widthMultiplier: Float,
    heightMultiplier: Float,
    mode: String?
): Pair<Float, Float> {
    val sourceWidth = nativeWidth * widthMultiplier
    val sourceHeight = nativeHeight * heightMultiplier
    var width = sourceWidth
    var height = sourceHeight
    val targetAspect = STORY_WIDTH.toFloat() / STORY_HEIGHT.toFloat()
    val sourceAspect = sourceWidth / maxOf(1f, sourceHeight)

    when (mode) {
        "fill" -> {
            width = STORY_WIDTH.toFloat()
            height = STORY_HEIGHT.toFloat()
        }
        "width" -> {
            width = STORY_WIDTH.toFloat()
            height = (sourceHeight * STORY_WIDTH.toFloat()) / sourceWidth
        }
        "height" -> {
            height = STORY_HEIGHT.toFloat()
            width = (sourceWidth * STORY_HEIGHT.toFloat()) / sourceHeight
        }
        "coverall" -> {
            if (sourceAspect < targetAspect) {
                width = STORY_WIDTH.toFloat()
                height = (sourceHeight * STORY_WIDTH.toFloat()) / sourceWidth
            } else {
                height = STORY_HEIGHT.toFloat()
                width = (sourceWidth * STORY_HEIGHT.toFloat()) / sourceHeight
            }
        }
        "showall" -> {
            if (sourceAspect > targetAspect) {
                width = STORY_WIDTH.toFloat()
                height = (sourceHeight * STORY_WIDTH.toFloat()) / sourceWidth
            } else {
                height = STORY_HEIGHT.toFloat()
                width = (sourceWidth * STORY_HEIGHT.toFloat()) / sourceHeight
            }
        }
    }
    return Pair(width, height)
}

@Composable
fun BackgroundOverlay(
    background: BackgroundVisualState?,
    context: Context,
    family: StoryAssetFamily,
    modifier: Modifier = Modifier,
    rotationDeg: Float = 0f,
    colorFilter: ColorFilter? = null
) {
    if (background == null || background.textureKey.isNullOrEmpty() || background.alpha <= 0f) return

    val key = background.textureKey
    val url = resolveStoryAssetByKey(key, family) ?: return

    val painter = rememberAsyncImagePainter(model = url)
    val painterState = painter.state

    // 若图片尚未就绪则不盲目按 1280x720 占位，避免画面闪烁变形
    if (painterState !is AsyncImagePainter.State.Success) return

    val sz = painter.intrinsicSize
    val rawWidth = if (sz.width > 0f) sz.width else 1024f
    val rawHeight = if (sz.height > 0f) sz.height else 1024f

    val (nativeWidth, nativeHeight) = remember(key, rawWidth, rawHeight, family, context.backgroundPpuMap) {
        calculateNativeRect(key, rawWidth, rawHeight, family, context.backgroundPpuMap)
    }

    val (layoutWidth, layoutHeight) = remember(
        nativeWidth, nativeHeight, background.widthMultiplier, background.heightMultiplier, background.screenAdapt
    ) {
        layoutForScreenAdapt(
            nativeWidth = nativeWidth,
            nativeHeight = nativeHeight,
            widthMultiplier = background.widthMultiplier,
            heightMultiplier = background.heightMultiplier,
            mode = background.screenAdapt
        )
    }

    // Centered placement:
    // centerX = STORY_WIDTH / 2 + x
    // centerY = STORY_HEIGHT / 2 - y
    val centerX = STORY_WIDTH / 2f + background.x
    val centerY = STORY_HEIGHT / 2f - background.y
    val topLeftX = centerX - layoutWidth / 2f
    val topLeftY = centerY - layoutHeight / 2f

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(background.alpha.coerceIn(0f, 1f))
    ) {
        if (background.tiled) {
            val bitmap = painterState.result.image.toBitmap()
            // Tiled repeat drawing with alignTilesToLowerLeft compensation
            Canvas(
                modifier = Modifier
                    .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
                    .size(layoutWidth.dp, layoutHeight.dp)
                    .graphicsLayer {
                        scaleX = background.scaleX
                        scaleY = background.scaleY
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .rotate(rotationDeg)
            ) {
                val tileScaleX = nativeWidth / maxOf(1f, rawWidth)
                val tileScaleY = nativeHeight / maxOf(1f, rawHeight)
                val scaledTileH = rawHeight * tileScaleY
                val yOffset = if (scaledTileH > 0f) (layoutHeight % scaledTileH) else 0f

                val shader = android.graphics.BitmapShader(
                    bitmap,
                    android.graphics.Shader.TileMode.REPEAT,
                    android.graphics.Shader.TileMode.REPEAT
                )
                val matrix = Matrix().apply {
                    setScale(tileScaleX, tileScaleY)
                    postTranslate(0f, yOffset)
                }
                shader.setLocalMatrix(matrix)

                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    this.shader = shader
                }

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRect(0f, 0f, layoutWidth, layoutHeight, paint)
                }
            }
        } else {
            // Simple sprite drawing
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                modifier = Modifier
                    .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
                    .size(layoutWidth.dp, layoutHeight.dp)
                    .graphicsLayer {
                        scaleX = background.scaleX
                        scaleY = background.scaleY
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .rotate(rotationDeg)
            )
        }
    }
}