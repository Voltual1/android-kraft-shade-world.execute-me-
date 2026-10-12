package world.eyes.witness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

data class TrailPoint(
    val offset: Offset,
    val timestamp: Long
)

@Composable
fun TouchTrailCanvas(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    trailColor: Color = Color(0xFF00E5FF),
    fadeDurationMs: Long = 650L
) {
    val points = remember { mutableStateListOf<TrailPoint>() }
    val frameClock = remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 驱动每帧刷新的时钟，清理过期拖尾点
    LaunchedEffect(Unit) {
        while (isActive) {
            awaitFrame()
            val now = System.currentTimeMillis()
            frameClock.longValue = now
            if (points.isNotEmpty()) {
                val cutoff = now - fadeDurationMs
                points.removeAll { it.timestamp < cutoff }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    var downOffset = down.position
                    val downTime = System.currentTimeMillis()
                    var isDrag = false

                    points.add(TrailPoint(downOffset, downTime))

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val currentPos = change.position
                            val distSq = (currentPos.x - downOffset.x) * (currentPos.x - downOffset.x) +
                                    (currentPos.y - downOffset.y) * (currentPos.y - downOffset.y)

                            if (distSq > 100f) { // 位移大于 10px 判定为划动拖尾
                                isDrag = true
                            }

                            points.add(TrailPoint(currentPos, System.currentTimeMillis()))
                            change.consume()
                        } else {
                            // 手指抬起
                            val elapsed = System.currentTimeMillis() - downTime
                            if (!isDrag && elapsed < 350L) {
                                onTap()
                            }
                            break
                        }
                    }
                }
            }
    ) {
        val now = frameClock.longValue
        if (points.size < 2) return@Canvas

        val validPoints = points.filter { (now - it.timestamp) < fadeDurationMs }
        if (validPoints.size < 2) return@Canvas

        // 逐段按生命周期绘制衰减发光线条
        for (i in 0 until validPoints.size - 1) {
            val p1 = validPoints[i]
            val p2 = validPoints[i + 1]

            val age = (now - p1.timestamp).toFloat()
            val life = (1.0f - (age / fadeDurationMs.toFloat())).coerceIn(0f, 1f)

            if (life <= 0f) continue

            val strokeWidth = 14f * life
            val glowWidth = 32f * life

            // 外层发光辉光晕
            drawLine(
                color = trailColor.copy(alpha = 0.25f * life),
                start = p1.offset,
                end = p2.offset,
                strokeWidth = glowWidth,
                cap = StrokeCap.Round
            )

            // 中层霓虹光束
            drawLine(
                color = trailColor.copy(alpha = 0.85f * life),
                start = p1.offset,
                end = p2.offset,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // 内层高能白炽光芯
            drawLine(
                color = Color.White.copy(alpha = 0.95f * life),
                start = p1.offset,
                end = p2.offset,
                strokeWidth = (strokeWidth * 0.45f).coerceAtLeast(1.5f),
                cap = StrokeCap.Round
            )
        }

        // 最新触点处的微型全息瞄准点
        val latest = validPoints.last()
        val latestLife = (1.0f - ((now - latest.timestamp) / fadeDurationMs.toFloat())).coerceIn(0f, 1f)
        if (latestLife > 0.3f) {
            drawCircle(
                color = Color.White.copy(alpha = latestLife),
                radius = 7f,
                center = latest.offset
            )
            drawCircle(
                color = trailColor.copy(alpha = 0.5f * latestLife),
                radius = 16f,
                center = latest.offset,
                style = Stroke(width = 2f)
            )
        }
    }
}