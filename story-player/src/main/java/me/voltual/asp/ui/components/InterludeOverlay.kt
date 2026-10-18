package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.voltual.asp.Context
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.StoryAssetFamily
import me.voltual.asp.engine.rendering.CharacterArtVisual
import me.voltual.asp.engine.rendering.FaceOverlayBaker
import me.voltual.asp.engine.rendering.InterludeChannelVisualState
import me.voltual.asp.engine.rendering.InterludeElementVisualState
import me.voltual.asp.engine.resolveStoryAssetByKey
import kotlin.math.roundToInt

@Composable
fun InterludeOverlay(
    interludes: Map<Int, InterludeChannelVisualState>,
    context: Context,
    modifier: Modifier = Modifier
) {
    if (interludes.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        interludes.values.filter { it.visible }.forEach { channel ->
            RenderSingleInterludeChannel(channel = channel, context = context)
        }
    }
}

@Composable
private fun RenderSingleInterludeChannel(
    channel: InterludeChannelVisualState,
    context: Context
) {
    val maskWidthDp = channel.maskWidth.dp
    val maskHeightDp = channel.maskHeight.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (STORY_WIDTH / 2f + channel.offsetX - channel.maskWidth / 2f).roundToInt(),
                    y = (STORY_HEIGHT / 2f - channel.offsetY - channel.maskHeight / 2f).roundToInt()
                )
            }
            .size(width = maskWidthDp, height = maskHeightDp)
            .clip(RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        // Elements in this channel
        channel.elements.values.filter { it.visible }.forEach { elem ->
            RenderSingleInterludeElement(elem = elem, context = context)
        }

        // Channel character name label
        if (channel.charName.isNotEmpty()) {
            Text(
                text = channel.charName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun RenderSingleInterludeElement(
    elem: InterludeElementVisualState,
    context: Context
) {
    val androidContext = LocalContext.current
    var characterVisual by remember(elem.avatarCharacterKey, elem.avatarExpression) {
        mutableStateOf<CharacterArtVisual?>(null)
    }

    LaunchedEffect(elem.avatarCharacterKey, elem.avatarExpression) {
        if (elem.type == 3 && elem.name.isNotEmpty()) {
            characterVisual = FaceOverlayBaker.resolveAndBake(
                context = context,
                characterKey = elem.avatarCharacterKey,
                expression = elem.avatarExpression,
                androidContext = androidContext
            )
        }
    }

    val elementModifier = Modifier
        .offset {
            IntOffset(
                x = elem.x.roundToInt(),
                y = elem.y.roundToInt()
            )
        }
        .scale(elem.scaleX, elem.scaleY)
        .alpha(elem.alpha.coerceIn(0f, 1f))

    if (elem.type == 3) {
        // Type 3: AVG Character avatar (裁剪保留上半身)
        characterVisual?.let { art ->
            when (art) {
                is CharacterArtVisual.BakedBitmap -> {
                    Image(
                        bitmap = art.bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = elementModifier
                    )
                }
                is CharacterArtVisual.SingleUrl -> {
                    AsyncImage(
                        model = art.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = elementModifier
                    )
                }
            }
        }
    } else {
        // Type 1: Cutin, Type 2: Background, Other: Image
        val family = when (elem.type) {
            1 -> StoryAssetFamily.CUTIN
            2 -> StoryAssetFamily.BACKGROUND
            else -> StoryAssetFamily.IMAGE
        }
        val url = resolveStoryAssetByKey(elem.name, family)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = elementModifier
            )
        }
    }
}