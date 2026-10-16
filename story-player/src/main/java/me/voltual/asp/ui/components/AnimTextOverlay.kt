package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.R
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.rendering.AnimTextVisualItem
import kotlin.math.roundToInt

private fun lerpKeyframes(time: Float, frames: List<Pair<Float, Float>>): Float {
    if (frames.isEmpty()) return 0f
    if (time <= frames.first().first) return frames.first().second
    for (i in 1 until frames.size) {
        val next = frames[i]
        val prev = frames[i - 1]
        if (time <= next.first) {
            val factor = (time - prev.first) / (next.first - prev.first)
            return prev.second + (next.second - prev.second) * factor
        }
    }
    return frames.last().second
}

@Composable
fun AnimTextOverlay(
    animTexts: List<AnimTextVisualItem>,
    modifier: Modifier = Modifier
) {
    if (animTexts.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        animTexts.forEach { stamp ->
            RenderSingleStamp(stamp = stamp)
        }
    }
}

@Composable
private fun RenderSingleStamp(stamp: AnimTextVisualItem) {
    val time = stamp.progress * 5f // 0f to 5f seconds timeline

    val rootAlpha = lerpKeyframes(
        time,
        listOf(0f to 0f, 1f / 3f to 1f, 4f to 1f, 5f to 0f)
    )

    val outerSize = lerpKeyframes(
        time,
        listOf(0f to 25f, 1f / 3f to 109.74f, 5f / 6f to 123f, 5f to 140.01f)
    )

    val innerSize = lerpKeyframes(
        time,
        listOf(0f to 16f, 1f / 3f to 50.1f, 5f / 6f to 53f, 5f to 55.81f)
    )

    val outerAlpha = lerpKeyframes(
        time,
        listOf(0f to 1f, 2.5f to 1f, 4.5f to 0f)
    )

    val iconGroupAlpha = lerpKeyframes(
        time,
        listOf(0f to 1f, 4.5f to 1f, 5f to 0f)
    )

    val compsAlpha = lerpKeyframes(
        time,
        listOf(0f to 0f, 0.95f to 0f, 1.1167f to 1f)
    )

    val textGroupX = lerpKeyframes(
        time,
        listOf(0f to -121.03f, 1f / 3f to -97.31f, 5f / 6f to -93.42f, 5f to -85.35f)
    )

    // Stamp root container
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (STORY_WIDTH / 2f + stamp.positionX).roundToInt(),
                    y = (STORY_HEIGHT / 2f - stamp.positionY).roundToInt()
                )
            }
            .alpha(rootAlpha)
    ) {
        // 1. Back shadow
        Image(
            painter = painterResource(id = R.drawable.back_shadow),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(x = (-244.14f).dp, y = (-104.53f).dp)
                .size(width = 520.dp, height = 209.06.dp)
        )

        // 2. Frame outer
        Image(
            painter = painterResource(id = R.drawable.frame_outer),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (-143.42f - outerSize / 2f).roundToInt(),
                        y = (-outerSize / 2f).roundToInt()
                    )
                }
                .size(outerSize.dp)
                .alpha(outerAlpha)
        )

        // 3. Icon Group
        Box(
            modifier = Modifier
                .offset(x = (-143.42f).dp, y = 0.dp)
                .alpha(iconGroupAlpha)
        ) {
            // Icon back (74x74)
            Image(
                painter = painterResource(id = R.drawable.icon_back),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset(x = (-37).dp, y = (-37).dp)
                    .size(74.dp)
                    .alpha(0.6f)
            )

            // Inner frame
            Image(
                painter = painterResource(id = R.drawable.frame_inner),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (-innerSize / 2f).roundToInt(),
                            y = (-innerSize / 2f).roundToInt()
                        )
                    }
                    .size(innerSize.dp)
                    .alpha(0.6f)
            )

            // Comps
            Image(
                painter = painterResource(id = R.drawable.icon_comps),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset(x = (-7).dp, y = (-21).dp)
                    .size(width = 14.dp, height = 42.dp)
                    .alpha(compsAlpha)
            )

            // 4 Corner Bricks
            val brickTargets = listOf(
                -5.5f to -5.5f,
                5.5f to -5.5f,
                5.5f to 5.5f,
                -5.5f to 5.5f
            )

            brickTargets.forEach { (endX, endY) ->
                val brickAlpha = lerpKeyframes(
                    time,
                    listOf(0f to 0f, 1f / 12f to 1f, 0.8167f to 1f, 0.9667f to 0f)
                )
                val curX = lerpKeyframes(
                    time,
                    listOf(0f to 0f, 2f / 3f to endX * 0.92f, 0.9167f to endX)
                )
                val curY = lerpKeyframes(
                    time,
                    listOf(0f to 0f, 2f / 3f to endY * 0.92f, 0.9167f to endY)
                )

                Image(
                    painter = painterResource(id = R.drawable.icon_start),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (curX - 5.5f).roundToInt(),
                                y = (curY - 5.5f).roundToInt()
                            )
                        }
                        .size(11.dp)
                        .alpha(brickAlpha)
                )
            }
        }

        // 4. Text Group
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = textGroupX.roundToInt(),
                        y = 0
                    )
                }
        ) {
            // Gradient back
            Image(
                painter = painterResource(id = R.drawable.back_gradient),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset(x = (-200).dp, y = (-43.5f).dp)
                    .size(width = 260.dp, height = 87.dp)
                    .alpha(0.15f)
            )

            // Main Location Text
            if (stamp.style != "avg_only_medium" && stamp.mainText.isNotEmpty()) {
                Text(
                    text = stamp.mainText,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = 0.dp, y = (-26).dp)
                )
            }

            // Sub Location Text
            if (stamp.style != "avg_only_heavy" && stamp.subText.isNotEmpty()) {
                Text(
                    text = stamp.subText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.offset(x = 0.dp, y = 8.dp)
                )
            }
        }
    }
}