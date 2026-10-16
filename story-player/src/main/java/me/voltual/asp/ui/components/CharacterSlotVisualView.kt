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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
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

    LaunchedEffect(charSlot.characterKey, charSlot.expression) {
        visual = FaceOverlayBaker.resolveAndBake(
            context = context,
            characterKey = charSlot.characterKey,
            expression = charSlot.expression,
            androidContext = androidContext
        )
    }

    val currentVisual = visual ?: return
    val linkNode = currentVisual.linkNode

    // Layout calculation according to AVG native CharacterPanel
    val sizeX = linkNode?.size?.x?.takeIf { it > 0f } ?: 1024f
    val sizeY = linkNode?.size?.y?.takeIf { it > 0f } ?: 1024f
    val offsetX = linkNode?.pos?.x ?: 0f
    val offsetY = linkNode?.pos?.y ?: 0f

    val baseX = (SLOT_BASE_X[charSlot.slot] ?: 640f) - sizeX / 2f + offsetX
    val baseY = STORY_HEIGHT - sizeY / 2f - offsetY

    val targetX = baseX + charSlot.x
    val targetY = baseY + charSlot.y

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
            .fillMaxSize()
            .offset {
                IntOffset(
                    x = targetX.roundToInt(),
                    y = targetY.roundToInt()
                )
            }
            .size(width = sizeX.dp, height = sizeY.dp)
            .alpha(charSlot.alpha.coerceIn(0f, 1f))
            .scale(charSlot.scaleX, charSlot.scaleY)
            .rotate(charSlot.rotationDeg),
        contentAlignment = Alignment.Center
    ) {
        when (currentVisual) {
            is CharacterArtVisual.BakedBitmap -> {
                Image(
                    bitmap = currentVisual.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is CharacterArtVisual.SingleUrl -> {
                AsyncImage(
                    model = currentVisual.url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}