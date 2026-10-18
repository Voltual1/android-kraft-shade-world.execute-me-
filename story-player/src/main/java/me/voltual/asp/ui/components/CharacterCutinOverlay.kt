package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.Context
import me.voltual.asp.engine.rendering.CharacterArtVisual
import me.voltual.asp.engine.rendering.CharacterCutinSlotState
import me.voltual.asp.engine.rendering.FaceOverlayBaker
import kotlin.math.roundToInt

@Composable
fun CharacterCutinOverlay(
    cutinSlots: Map<String, CharacterCutinSlotState>,
    context: Context,
    modifier: Modifier = Modifier
) {
    if (cutinSlots.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        cutinSlots.values.forEach { slot ->
            RenderSingleCutin(slot = slot, context = context)
        }
    }
}

@Composable
private fun RenderSingleCutin(
    slot: CharacterCutinSlotState,
    context: Context
) {
    val androidContext = LocalContext.current
    val maskWidthDp = slot.maskW.dp
    val maskHeightDp = slot.maskH.dp
    val targetWidthDp = slot.targetW.dp
    val targetHeightDp = slot.targetH.dp

    var currentVisual by remember(slot.characterKey, slot.expression) {
        mutableStateOf<CharacterArtVisual?>(null)
    }
    var oldVisual by remember(slot.oldCharacterKey, slot.oldExpression) {
        mutableStateOf<CharacterArtVisual?>(null)
    }

    LaunchedEffect(slot.characterKey, slot.expression) {
        currentVisual = FaceOverlayBaker.resolveAndBake(
            context = context,
            characterKey = slot.characterKey,
            expression = slot.expression,
            androidContext = androidContext
        )
    }

    LaunchedEffect(slot.oldCharacterKey, slot.oldExpression) {
        oldVisual = FaceOverlayBaker.resolveAndBake(
            context = context,
            characterKey = slot.oldCharacterKey,
            expression = slot.oldExpression,
            androidContext = androidContext
        )
    }

    val maskAlignment = when (slot.fadeStyle) {
        "horiz_expand_left2right" -> Alignment.CenterStart
        "horiz_expand_right2left" -> Alignment.CenterEnd
        "vert_expand_top2bottom" -> Alignment.TopCenter
        "vert_expand_bottom2top" -> Alignment.BottomCenter
        else -> Alignment.Center
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (slot.rootX - slot.targetW / 2f).roundToInt(),
                    y = (slot.rootY - 360f).roundToInt()
                )
            }
            .size(width = targetWidthDp, height = targetHeightDp)
            .alpha(slot.alpha),
        contentAlignment = maskAlignment
    ) {
        Box(
            modifier = Modifier
                .size(width = maskWidthDp, height = maskHeightDp)
                .clip(RectangleShape)
        ) {
            // Default background gray strip (#898989)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF898989))
            )

            // Zoom and POV container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(slot.zoom)
                    .offset(x = (-slot.povX).dp, y = slot.povY.dp)
            ) {
                // Previous character visual when crossfading
                if (slot.oldCharacterKey != null && slot.oldCharAlpha > 0f) {
                    oldVisual?.let { vis ->
                        RenderArtContent(
                            visual = vis,
                            alpha = slot.oldCharAlpha,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(x = slot.charSlotX.dp, y = (slot.charSlotY - 360f).dp)
                        )
                    }
                }

                // Current incoming character visual
                currentVisual?.let { vis ->
                    val currentAlpha = if (slot.oldCharacterKey != null) (1f - slot.oldCharAlpha) else 1f
                    RenderArtContent(
                        visual = vis,
                        alpha = currentAlpha,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = slot.charSlotX.dp, y = (slot.charSlotY - 360f).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderArtContent(
    visual: CharacterArtVisual,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    when (visual) {
        is CharacterArtVisual.BakedBitmap -> {
            Image(
                bitmap = visual.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = modifier.alpha(alpha.coerceIn(0f, 1f))
            )
        }
        is CharacterArtVisual.SingleUrl -> {
            AsyncImage(
                model = visual.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = modifier.alpha(alpha.coerceIn(0f, 1f))
            )
        }
    }
}