package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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

    // 严密对齐 Web 版 layoutImageForScreenAdapt 规则：
    // coverall / 默认模式统一采用 Crop 铺满整个 1280x720 视口，杜绝上下留黑
    val contentScale = when (background.screenAdapt?.lowercase()) {
        "fill" -> ContentScale.FillBounds
        "width" -> ContentScale.FillWidth
        "height" -> ContentScale.FillHeight
        "showall" -> ContentScale.Fit
        "coverall" -> ContentScale.Crop
        else -> ContentScale.Crop
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
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
                .graphicsLayer {
                    translationX = background.x
                    translationY = -background.y
                    scaleX = background.scaleX
                    scaleY = background.scaleY
                    this.rotationZ = rotationDeg
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        )
    }
}