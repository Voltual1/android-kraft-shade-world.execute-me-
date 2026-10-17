package me.voltual.asp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.rendering.TimerVisualState

@Composable
fun TimerStickerOverlay(
    timer: TimerVisualState,
    modifier: Modifier = Modifier
) {
    if (!timer.visible || timer.timeFormatted.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = timer.timeFormatted,
            style = TextStyle(
                color = Color.White,
                fontSize = timer.sizePx.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                shadow = Shadow(
                    color = Color(0xE6404040),
                    offset = Offset(0f, 0f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier
                .offset(x = timer.x.dp, y = timer.y.dp)
                .width(timer.widthPx.dp)
                .alpha(timer.alpha.coerceIn(0f, 1f))
        )
    }
}