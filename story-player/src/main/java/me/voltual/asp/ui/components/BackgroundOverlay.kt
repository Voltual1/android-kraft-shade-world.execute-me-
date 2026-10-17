package me.voltual.asp.ui.components

import android.graphics.Bitmap
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
import androidx.compose.ui.platform.LocalContext
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
 * - 优先读取 background.json 的 PPU：(width / ppu) * 100
 * - 若接近 16:9，则映射为标准的 1280x720
 * - 缺省则采用原始纹理像素大小
 */
private fun calculateNativeRect(
    key: String,
    rawWidth: Float,
    rawHeight: Float,
    backgroundPpuMap: Map<String, Float>?
): Pair<Float, Float> {
    val ppu = backgroundPpuMap?.get(key.lowercase())
    if (ppu != null && ppu > 0f) {
        return Pair((rawWidth / ppu) * 100f, (rawHeight / ppu) * 100f)
    }
    if (abs(rawWidth / rawHeight - STORY_WIDTH.toFloat() / STORY_HEIGHT.toFloat()) < 0.01f) {
        return Pair(STORY_WIDTH.toFloat(), STORY_HEIGHT.toFloat())
    }
    return Pair(rawWidth, rawHeight)
}

/**
 * 依据 Web 端 layoutImageForScreenAdapt 计算适配尺寸：
 * 支持 fill / width / height / coverall / showall 及宽高乘数
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

    val (rawWidth, rawHeight) = when (painterState) {
        is AsyncImagePainter.State.Success -> {
            val sz = painter.intrinsicSize
            Pair(
                if (sz.width > 0f) sz.width else STORY_WIDTH.toFloat(),
                if (sz.height > 0f) sz.height else STORY_HEIGHT.toFloat()
            )
        }
        else -> Pair(STORY_WIDTH.toFloat(), STORY_HEIGHT.toFloat())
    }

    val (nativeWidth, nativeHeight) = remember(key, rawWidth, rawHeight, context.backgroundPpuMap) {
        calculateNativeRect(key, rawWidth, rawHeight, context.backgroundPpuMap)
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
        if (background.tiled && painterState is AsyncImagePainter.State.Success) {
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
                    colorFilter?.let {
                        // Apply ColorFilter if provided
                    }
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