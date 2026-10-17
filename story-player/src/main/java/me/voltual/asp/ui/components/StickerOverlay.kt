package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.rendering.StickerVisualState

@Composable
fun StickerOverlay(
    stickers: Map<String, StickerVisualState>,
    modifier: Modifier = Modifier
) {
    if (stickers.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        stickers.values.filter { it.visible && it.text.isNotEmpty() }.forEach { sticker ->
            RenderSingleSticker(sticker = sticker)
        }
    }
}

@Composable
private fun RenderSingleSticker(sticker: StickerVisualState) {
    val textAlign = when (sticker.alignment.lowercase()) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        else -> TextAlign.Start
    }

    Text(
        text = sticker.text,
        style = TextStyle(
            color = Color.White,
            fontSize = sticker.sizePx.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = textAlign,
            shadow = Shadow(
                color = Color(0xE6404040),
                offset = Offset(0f, 0f),
                blurRadius = 4f
            )
        ),
        modifier = Modifier
            .offset(x = sticker.x.dp, y = sticker.y.dp)
            .width(sticker.widthPx.dp)
            .alpha(sticker.alpha.coerceIn(0f, 1f))
            .rotate(sticker.rotationDeg)
    )
}