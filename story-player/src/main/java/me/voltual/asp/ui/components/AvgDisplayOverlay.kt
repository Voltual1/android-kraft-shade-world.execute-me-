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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.Context
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.rendering.AvgDisplayVisualItem
import me.voltual.asp.engine.rendering.CharacterArtVisual
import me.voltual.asp.engine.rendering.FaceOverlayBaker
import me.voltual.asp.engine.resolveStoryAssetByKey
import kotlin.math.roundToInt

@Composable
fun AvgDisplayOverlay(
    slotName: String,
    avgDisplays: Map<String, AvgDisplayVisualItem>,
    context: Context,
    modifier: Modifier = Modifier
) {
    val items = avgDisplays.values
        .filter { it.slot == slotName && it.visible && it.name.isNotEmpty() }
        .sortedBy { it.layer }

    if (items.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        items.forEach { item ->
            RenderSingleAvgDisplayItem(item = item, context = context)
        }
    }
}

@Composable
private fun RenderSingleAvgDisplayItem(
    item: AvgDisplayVisualItem,
    context: Context
) {
    val androidContext = LocalContext.current
    var characterVisual by remember(item.name, item.style) {
        mutableStateOf<CharacterArtVisual?>(null)
    }

    LaunchedEffect(item.name, item.style) {
        if (item.style == "character") {
            characterVisual = FaceOverlayBaker.resolveAndBake(
                context = context,
                characterKey = item.name,
                expression = null,
                androidContext = androidContext
            )
        }
    }

    if (item.style == "bg") {
        val url = resolveStoryAssetByKey(item.name, StoryAssetFamily.BACKGROUND)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(item.alpha.coerceIn(0f, 1f))
            )
        }
    } else if (item.style == "character") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = (STORY_WIDTH / 2f + item.x - 512f).roundToInt(),
                        y = (STORY_HEIGHT / 2f - item.y - 512f).roundToInt()
                    )
                }
                .size(1024.dp)
                .alpha(item.alpha.coerceIn(0f, 1f))
                .scale(item.scaleX, item.scaleY)
                .rotate(item.rotationZ),
            contentAlignment = Alignment.Center
        ) {
            characterVisual?.let { art ->
                when (art) {
                    is CharacterArtVisual.BakedBitmap -> {
                        Image(
                            bitmap = art.bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is CharacterArtVisual.SingleUrl -> {
                        AsyncImage(
                            model = art.url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    } else {
        // animekv, effect, bgeffect, image
        val url = resolveStoryAssetByKey(item.name, StoryAssetFamily.IMAGE)
            ?: resolveStoryAssetByKey(item.name, StoryAssetFamily.BACKGROUND)
        if (url != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = (STORY_WIDTH / 2f + item.x).roundToInt(),
                            y = (STORY_HEIGHT / 2f - item.y).roundToInt()
                        )
                    }
                    .alpha(item.alpha.coerceIn(0f, 1f))
                    .scale(item.scaleX, item.scaleY)
                    .rotate(item.rotationZ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}