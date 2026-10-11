package world.eyes.witness.ui.stage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import world.eyes.witness.director.SceneState
import world.eyes.witness.shader.ShutdownKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun ShutdownStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { ShutdownKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 03:25.811 是终局 EXECUTION 唱响点 (相对 localTime 约 12.351s)
    val isFinalExecution = localTime in 12.351f..13.160f
    val isShutdownOutro = localTime >= 13.160f

    val currentText = snapshot.currentLine?.text ?: ""

    LaunchedEffect(isFinalExecution) {
        if (isFinalExecution) {
            scaleAnim.snapTo(1.5f)
            scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 280))
        } else {
            scaleAnim.snapTo(1.0f)
        }
    }

    LaunchedEffect(Unit) {
        effectState.setEffect { targetBuffer ->
            pipeline(targetBuffer) {
                step(shader, targetBuffer)
            }
        }
    }

    LaunchedEffect(sceneState.globalTimeSeconds, sceneState.beatPulse) {
        shader.time = localTime
        shader.beatPulse = sceneState.beatPulse
        shader.progress = sceneState.progress
        shader.isFinalExecution = if (isFinalExecution) 1.0f else 0.0f
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：死循环消散 / 终局处决闪屏 / CRT 停机湮灭 KraftShader 管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 与文字排版 (若在 CRT 关机阶段则逐渐隐退)
        if (!isShutdownOutro) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶栏系统停机指示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFinalExecution) "CRITICAL: EXECUTING HOST ENTITY" else "SYSTEM: SHUTTING DOWN OPEN LOOP",
                        color = if (isFinalExecution) Color.Red else Color.Cyan,
                        fontFamily = MvFonts.Terminal,
                        fontSize = 15.sp
                    )

                    Text(
                        text = if (isFinalExecution) "EXECUTE(ME)" else "LOOP_END",
                        color = if (isFinalExecution) Color.Red else Color.Green,
                        fontFamily = MvFonts.Terminal,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 中央：终局 EXECUTION 大词冲击
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scaleAnim.value),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFinalExecution) "EXECUTION" else (snapshot.currentLine?.text ?: "world.execute(me);"),
                            color = if (isFinalExecution) Color(0xFFFF1744) else Color(0xFF00E5FF),
                            fontFamily = if (isFinalExecution) MvFonts.DisplayBlack else MvFonts.CodeBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isFinalExecution) 46.sp else 26.sp,
                            letterSpacing = if (isFinalExecution) 5.sp else 2.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        snapshot.nextLine?.let { next ->
                            Text(
                                text = ">> ${next.text}",
                                color = Color.Gray,
                                fontFamily = MvFonts.Terminal,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 底栏：终局代码行
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.15f))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "world.execute(me); // process terminated with exit code 0",
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = MvFonts.CodeRegular,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}