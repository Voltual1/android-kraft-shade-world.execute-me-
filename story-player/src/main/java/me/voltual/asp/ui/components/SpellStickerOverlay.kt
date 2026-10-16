package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.rendering.SpellStickerVisualState
import kotlin.math.roundToInt

@Composable
fun SpellStickerOverlay(
    spellStickers: Map<String, SpellStickerVisualState>,
    modifier: Modifier = Modifier
) {
    if (spellStickers.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        spellStickers.values.filter { it.visible }.forEach { sticker ->
            RenderSingleSpellSticker(sticker = sticker)
        }
    }
}

@Composable
private fun RenderSingleSpellSticker(sticker: SpellStickerVisualState) {
    val isSami = sticker.style == "sami"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                IntOffset(
                    x = (STORY_WIDTH / 2f + sticker.x).roundToInt(),
                    y = (STORY_HEIGHT / 2f + sticker.y).roundToInt()
                )
            }
            .alpha(sticker.alpha.coerceIn(0f, 1f))
            .scale(sticker.scaleX, sticker.scaleY)
            .rotate(-sticker.angle),
        contentAlignment = Alignment.Center
    ) {
        // Main Spell Text
        if (sticker.mainText.isNotEmpty()) {
            Text(
                text = sticker.mainText,
                color = Color.White,
                fontSize = if (isSami) 32.sp else 24.sp,
                fontFamily = if (isSami) FontFamily.Serif else FontFamily.SansSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .offset(x = 36.dp, y = (-52).dp)
                    .scale(if (isSami) 0.9f else 1.0f, if (isSami) 1.1f else 1.0f)
            )
        }

        // Sub Spell Text
        if (sticker.subText.isNotEmpty()) {
            Text(
                text = sticker.subText,
                color = Color(0x8FFFFFFF), // rgba(255, 255, 255, 0.5608)
                fontSize = if (isSami) 16.sp else 14.sp,
                fontFamily = if (isSami) FontFamily.Serif else FontFamily.SansSerif,
                fontStyle = if (isSami) FontStyle.Italic else FontStyle.Normal,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .offset(x = if (isSami) (-147.5f).dp else (-189.4f).dp, y = 19.5.dp)
                    .scale(if (isSami) 0.9f else 0.88f, if (isSami) 1.2f else 1.0f)
            )
        }
    }
}