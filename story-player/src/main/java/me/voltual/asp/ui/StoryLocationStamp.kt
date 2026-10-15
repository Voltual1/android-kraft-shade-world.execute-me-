package me.voltual.asp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.R
import me.voltual.asp.engine.AnimTextState

@Composable
fun StoryLocationStamp(
    animTextState: AnimTextState,
    modifier: Modifier = Modifier
) {
    if (!animTextState.isVisible) return

    val progress = remember(animTextState) { Animatable(0f) }

    LaunchedEffect(animTextState) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animTextState.durationMs.toInt(),
                easing = LinearEasing
            )
        )
    }

    val p = progress.value
    // 关键帧插值映射：0~0.2秒淡入，4~5秒淡出
    val overallAlpha = when {
        p < 0.07f -> p / 0.07f
        p > 0.8f -> (1f - p) / 0.2f
        else -> 1f
    }.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(animTextState.positionX.toInt(), -animTextState.positionY.toInt()) }
            .alpha(overallAlpha),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // 左侧图标组合 (外框、内框、光芒背景)
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.frame_outer),
                    contentDescription = null,
                    modifier = Modifier.size(68.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.frame_inner),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.icon_back),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.icon_start),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧标题与副标题
            Column {
                if (animTextState.mainText.isNotEmpty()) {
                    Text(
                        text = animTextState.mainText,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                if (animTextState.subText.isNotEmpty()) {
                    Text(
                        text = animTextState.subText,
                        color = Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}