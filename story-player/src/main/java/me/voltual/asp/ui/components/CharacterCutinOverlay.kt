package me.voltual.asp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.Context
import me.voltual.asp.engine.rendering.CharacterCutinSlotState
import me.voltual.asp.engine.resolveStoryCharacterAssetByKey
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
    val maskWidthDp = slot.maskW.dp
    val maskHeightDp = slot.maskH.dp
    val targetWidthDp = slot.targetW.dp
    val targetHeightDp = slot.targetH.dp

    // Alignment calculation according to native fadeStyle pivot
    val maskAlignment = when (slot.fadeStyle) {
        "horiz_expand_left2right" -> Alignment.CenterStart
        "horiz_expand_right2left" -> Alignment.CenterEnd
        "vert_expand_top2bottom" -> Alignment.TopCenter
        "vert_expand_bottom2top" -> Alignment.BottomCenter
        else -> Alignment.Center
    }

    // Offset in 1280x720 canvas
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
        // Mask clipping frame
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
                    val oldArtUrl = resolveCharacterArtUrl(context, slot.oldCharacterKey, slot.oldExpression)
                    if (oldArtUrl != null) {
                        AsyncImage(
                            model = oldArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(slot.oldCharAlpha)
                                .offset(x = slot.charSlotX.dp, y = (slot.charSlotY - 360f).dp)
                        )
                    }
                }

                // Current incoming character visual
                val currentArtUrl = resolveCharacterArtUrl(context, slot.characterKey, slot.expression)
                if (currentArtUrl != null) {
                    val currentAlpha = if (slot.oldCharacterKey != null) (1f - slot.oldCharAlpha) else 1f
                    AsyncImage(
                        model = currentArtUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(currentAlpha)
                            .offset(x = slot.charSlotX.dp, y = (slot.charSlotY - 360f).dp)
                    )
                }
            }
        }
    }
}

private fun resolveCharacterArtUrl(context: Context, characterKey: String?, expression: String?): String? {
    if (characterKey == null) return null
    val link = context.linkMap[characterKey] ?: return null
    val item = link.array.find { it.name == expression } ?: link.array.firstOrNull() ?: return null

    val assetKey = if (item.group == -1 && !item.image.isNullOrEmpty()) {
        item.image
    } else if (item.group >= 0) {
        val group = link.groups.getOrNull(item.group)
        if (group is me.voltual.asp.engine.StoryCharacterGroup.FaceOverlay) group.base else null
    } else {
        null
    } ?: return null

    return resolveStoryCharacterAssetByKey(assetKey)
}