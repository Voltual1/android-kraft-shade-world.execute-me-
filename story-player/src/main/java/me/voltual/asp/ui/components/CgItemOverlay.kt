package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.rendering.CgItemVisualSlot
import me.voltual.asp.engine.resolveStoryAssetByKey
import kotlin.math.roundToInt

@Composable
fun CgItemOverlay(
    cgItems: Map<String, CgItemVisualSlot>,
    modifier: Modifier = Modifier
) {
    if (cgItems.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        cgItems.values.forEach { slot ->
            val rawKey = slot.assetKey.substringBeforeLast('_')
            val url = resolveStoryAssetByKey(rawKey, StoryAssetFamily.IMAGE)
                ?: resolveStoryAssetByKey(slot.assetKey, StoryAssetFamily.IMAGE)

            if (url != null) {
                val colorFilter = slot.tintRgb?.let {
                    ColorFilter.tint(
                        Color(
                            red = it.r.coerceIn(0f, 1f),
                            green = it.g.coerceIn(0f, 1f),
                            blue = it.b.coerceIn(0f, 1f),
                            alpha = it.a.coerceIn(0f, 1f)
                        )
                    )
                }

                val sizeModifier = if (slot.width > 0 && slot.height > 0) {
                    Modifier.size(slot.width.dp, slot.height.dp)
                } else {
                    Modifier.fillMaxSize()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = slot.x.roundToInt(),
                                y = slot.y.roundToInt()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = sizeModifier
                            .alpha(slot.alpha.coerceIn(0f, 1f))
                            .scale(slot.scale)
                            .rotate(slot.rotationDeg)
                    )
                }
            }
        }
    }
}