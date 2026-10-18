package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import coil3.compose.AsyncImage
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

    // Layout calculation strictly aligned with Web PixiStoryRenderer.ts & native CharacterPanel
    val sizeX = linkNode?.size?.x?.takeIf { it > 0f } ?: currentVisual.sourceWidth.takeIf { it > 0f } ?: 1024f
    val sizeY = linkNode?.size?.y?.takeIf { it > 0f } ?: currentVisual.sourceHeight.takeIf { it > 0f } ?: 1024f
    val offsetX = linkNode?.pos?.x ?: 0f
    val offsetY = linkNode?.pos?.y ?: 0f

    val baseX = (SLOT_BASE_X[charSlot.slot] ?: 640f) - sizeX / 2f + offsetX
    val baseY = STORY_HEIGHT - sizeY / 2f - offsetY

    val targetX = baseX + charSlot.totalOffsetX
    val targetY = baseY + charSlot.totalOffsetY

    val colorFilter = if (charSlot.dimmed) {
        // Dimmed: 50% brightness
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

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = targetX.roundToInt(),
                    y = targetY.roundToInt()
                )
            }
            .size(width = sizeX.dp, height = sizeY.dp)
            .graphicsLayer {
                scaleX = charSlot.scaleX
                scaleY = charSlot.scaleY
                rotationZ = charSlot.rotationDeg
                alpha = charSlot.alpha.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        contentAlignment = Alignment.Center
    ) {
        // Render previous character when crossfading
        if (charSlot.oldCharacterKey != null && charSlot.oldCharAlpha > 0f) {
            oldVisual?.let { oldVis ->
                RenderArtVisual(
                    art = oldVis,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(charSlot.oldCharAlpha.coerceIn(0f, 1f))
                )
            }
        }

        // Render current character
        visual?.let { curVis ->
            val curAlpha = if (charSlot.oldCharacterKey != null) (1f - charSlot.oldCharAlpha) else 1f
            RenderArtVisual(
                art = curVis,
                colorFilter = colorFilter,
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
    modifier: Modifier = Modifier
) {
    when (art) {
        is CharacterArtVisual.BakedBitmap -> {
            Image(
                bitmap = art.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        is CharacterArtVisual.SingleUrl -> {
            AsyncImage(
                model = art.url,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
    }
}