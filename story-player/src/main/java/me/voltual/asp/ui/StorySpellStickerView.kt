package me.voltual.asp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.SpellStickerItem

@Composable
fun StorySpellStickerView(
    stickers: Map<String, SpellStickerItem>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        stickers.values.filter { it.isVisible }.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(item.x.toInt(), item.y.toInt()) }
                    .rotate(item.angle)
                    .scale(item.scaleX, item.scaleY)
                    .alpha(item.alpha),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (item.mainText.isNotEmpty()) {
                        Text(
                            text = item.mainText,
                            color = Color.White,
                            fontSize = if (item.style == "sami") 28.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 3.sp
                        )
                    }
                    if (item.subText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.subText,
                            color = Color(0x99FFFFFF),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}