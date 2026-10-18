package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.Context
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.rendering.BackgroundVisualState
import me.voltual.asp.engine.resolveStoryAssetByKey

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

    // 背景全屏平铺/裁切适应，插画（IMAGE）保持原比例居中呈现
    val contentScale = if (family == StoryAssetFamily.BACKGROUND) {
        when (background.screenAdapt) {
            "fill" -> ContentScale.FillBounds
            "width" -> ContentScale.FillWidth
            "height" -> ContentScale.FillHeight
            else -> ContentScale.Crop
        }
    } else {
        ContentScale.Fit
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(background.alpha.coerceIn(0f, 1f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = contentScale,
            colorFilter = colorFilter,
            modifier = Modifier
                .fillMaxSize()
                .offset(x = background.x.dp, y = (-background.y).dp)
                .scale(background.scaleX, background.scaleY)
                .rotate(rotationDeg)
        )
    }
}