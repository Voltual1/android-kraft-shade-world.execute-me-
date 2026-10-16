package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.computeLegacyShowItemLayout
import me.voltual.asp.engine.rendering.ShowItemVisualState
import me.voltual.asp.engine.resolveStoryAssetByKey
import kotlin.math.roundToInt

@Composable
fun ShowItemOverlay(
    showItem: ShowItemVisualState?,
    modifier: Modifier = Modifier
) {
    if (showItem == null || !showItem.visible || showItem.key.isEmpty()) return

    val url = resolveStoryAssetByKey(showItem.key, StoryAssetFamily.IMAGE) ?: return
    val painter = rememberAsyncImagePainter(model = url)
    val painterState = painter.state

    val intrinsicWidth = if (painterState is AsyncImagePainter.State.Success) {
        painter.intrinsicSize.width.takeIf { it > 0f } ?: 800f
    } else {
        800f
    }
    val intrinsicHeight = if (painterState is AsyncImagePainter.State.Success) {
        painter.intrinsicSize.height.takeIf { it > 0f } ?: 600f
    } else {
        600f
    }

    val layout = computeLegacyShowItemLayout(intrinsicWidth, intrinsicHeight)

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(showItem.alpha.coerceIn(0f, 1f))
    ) {
        // Fullscreen black backdrop
        if (showItem.blackAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = showItem.blackAlpha.coerceIn(0f, 1f)))
            )
        }

        // Photo Card with White Border Frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = showItem.offsetX.roundToInt(),
                        y = showItem.offsetY.roundToInt()
                    )
                }
                .size(
                    width = (layout.contentWidth + layout.borderPx * 2f).dp,
                    height = (layout.contentHeight + layout.borderPx * 2f).dp
                )
                .background(Color.White)
                .padding(layout.borderPx.dp)
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}