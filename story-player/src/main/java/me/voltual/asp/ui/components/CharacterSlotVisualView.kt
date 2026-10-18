package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import me.voltual.asp.Context
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.rendering.CharacterArtVisual
import me.voltual.asp.engine.rendering.CharacterVisualSlot
import me.voltual.asp.engine.rendering.FaceOverlayBaker
import kotlin.math.roundToInt

private val SLOT_BASE_X = mapOf(
    "l" to 440f,
    "m" to 640f,
    "r" to 840f
)

@Composable
fun CharacterSlotVisualView(
    charSlot: CharacterVisualSlot,
    context: Context,
    modifier: Modifier = Modifier
) {
    val androidContext = LocalContext.current
    var visual by remember(charSlot.characterKey, charSlot.expression) {
        mutableStateOf<CharacterArtVisual?>(null)
    }
    var oldVisual by remember(charSlot.oldCharacterKey, charSlot.oldExpression) {
        mutableStateOf<CharacterArtVisual?>(null)
    }

    LaunchedEffect(charSlot.characterKey, charSlot.expression) {
        visual = FaceOverlayBaker.resolveAndBake(
            context = context,
            characterKey = charSlot.characterKey,
            expression = charSlot.expression,
            androidContext = androidContext
        )
    }

    LaunchedEffect(charSlot.oldCharacterKey, charSlot.oldExpression) {
        oldVisual = FaceOverlayBaker.resolveAndBake(
            context = context,
            characterKey = charSlot.oldCharacterKey,
            expression = charSlot.oldExpression,
            androidContext = androidContext
        )
    }

    val currentVisual = visual ?: oldVisual ?: return
    val linkNode = currentVisual.linkNode

    var intrinsicWidth by remember(currentVisual) {
        mutableFloatStateOf(currentVisual.sourceWidth.takeIf { it > 0f } ?: 1024f)
    }
    var intrinsicHeight by remember(currentVisual) {
        mutableFloatStateOf(currentVisual.sourceHeight.takeIf { it > 0f } ?: 1024f)
    }

    // 优先采用配置的 size，否则保持原图实际宽高
    val configuredWidth = linkNode?.size?.x?.takeIf { it > 0f }
    val configuredHeight = linkNode?.size?.y?.takeIf { it > 0f }

    val sizeX: Float
    val sizeY: Float
    if (configuredWidth != null && configuredHeight != null) {
        sizeX = configuredWidth
        sizeY = configuredHeight
    } else if (configuredWidth != null) {
        sizeX = configuredWidth
        sizeY = configuredWidth * (intrinsicHeight / intrinsicWidth.coerceAtLeast(1f))
    } else {
        sizeX = intrinsicWidth
        sizeY = intrinsicHeight
    }

    val offsetX = linkNode?.pos?.x ?: 0f
    val offsetY = linkNode?.pos?.y ?: 0f

    // 严格对齐 Web 版 PixiStoryRenderer.ts 的 Top-Left 绝对排版坐标公式：
    // targetX = (slotBaseX[slot] ?? slotBaseX.m) - sizeX / 2 + offsetX + totalOffsetX
    // targetY = STORY_HEIGHT - sizeY / 2 - offsetY + totalOffsetY
    val baseX = (SLOT_BASE_X[charSlot.slot] ?: 640f) - sizeX / 2f + offsetX
    val baseY = STORY_HEIGHT - sizeY / 2f - offsetY

    val targetX = baseX + charSlot.totalOffsetX
    val targetY = baseY + charSlot.totalOffsetY

    val colorFilter = if (charSlot.dimmed) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.5f, 0f, 0f, 0f, 0f,
                    0f, 0.5f, 0f, 0f, 0f,
                    0f, 0f, 0.5f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else {
        null
    }

    // 通过 wrapContentSize(Alignment.TopStart, unbounded = true) 彻底解除 720dp 高度约束限制，
    // 使得 requiredSize 能够以原比例自由超出屏幕下方，而 Top-Left 精确落在 targetX, targetY
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            .offset {
                IntOffset(
                    x = targetX.roundToInt(),
                    y = targetY.roundToInt()
                )
            }
            .requiredSize(width = sizeX.dp, height = sizeY.dp)
            .graphicsLayer {
                scaleX = charSlot.scaleX
                scaleY = charSlot.scaleY
                rotationZ = charSlot.rotationDeg
                alpha = charSlot.alpha.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        contentAlignment = Alignment.Center
    ) {
        // 交叉淡出时的旧立绘
        if (charSlot.oldCharacterKey != null && charSlot.oldCharAlpha > 0f) {
            oldVisual?.let { oldVis ->
                RenderArtVisual(
                    art = oldVis,
                    colorFilter = colorFilter,
                    onIntrinsicSizeDetermined = { w, h ->
                        intrinsicWidth = w
                        intrinsicHeight = h
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(charSlot.oldCharAlpha.coerceIn(0f, 1f))
                )
            }
        }

        // 当前立绘
        visual?.let { curVis ->
            val curAlpha = if (charSlot.oldCharacterKey != null) (1f - charSlot.oldCharAlpha) else 1f
            RenderArtVisual(
                art = curVis,
                colorFilter = colorFilter,
                onIntrinsicSizeDetermined = { w, h ->
                    intrinsicWidth = w
                    intrinsicHeight = h
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(curAlpha.coerceIn(0f, 1f))
            )
        }
    }
}

@Composable
private fun RenderArtVisual(
    art: CharacterArtVisual,
    colorFilter: ColorFilter?,
    onIntrinsicSizeDetermined: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    when (art) {
        is CharacterArtVisual.BakedBitmap -> {
            LaunchedEffect(art.bitmap) {
                onIntrinsicSizeDetermined(art.bitmap.width.toFloat(), art.bitmap.height.toFloat())
            }
            Image(
                bitmap = art.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        is CharacterArtVisual.SingleUrl -> {
            val painter = rememberAsyncImagePainter(model = art.url)
            val painterState = painter.state
            LaunchedEffect(painterState) {
                if (painterState is AsyncImagePainter.State.Success) {
                    val size = painter.intrinsicSize
                    if (size.width > 0f && size.height > 0f) {
                        onIntrinsicSizeDetermined(size.width, size.height)
                    }
                }
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
    }
}