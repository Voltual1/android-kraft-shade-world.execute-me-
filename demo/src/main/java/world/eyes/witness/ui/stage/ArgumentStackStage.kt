package world.eyes.witness.ui.stage

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import world.eyes.witness.director.SceneState
import world.eyes.witness.shader.ArgumentStackKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun ArgumentStackStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { ArgumentStackKraftShader() }

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
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：数据雨 / 栈帧几何 / 栈溢出警报 KraftShader 管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：调用栈追踪与系统崩溃转储 HUD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 顶栏异常警报
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FATAL EXCEPTION IN THREAD \"main\"",
                        color = Color.Red,
                        fontFamily = MvFonts.Terminal,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "CORE DUMPING: 0xDEADBEEF -> 0x00000000",
                        color = Color(0xFFFF9100),
                        fontFamily = MvFonts.Terminal,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "STACK_OVERFLOW",
                    color = Color.Red,
                    fontFamily = MvFonts.Terminal,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 崩溃倒计时进度条
            LinearProgressIndicator(
                progress = { sceneState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color.Red,
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.weight(1f))

            // 中央层叠 Java 调用栈轨迹 (Stack Trace)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Red.copy(alpha = 0.4f))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(14.dp)
            ) {
                Text(
                    text = "java.lang.IllegalArgumentException: God is always true",
                    color = Color.Red,
                    fontFamily = MvFonts.CodeBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "  at goddrinksjava.Thing.setOpinion(Thing.java:188)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
                Text(
                    text = "  at goddrinksjava.GodDrinksJava.main(GodDrinksJava.java:112)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
                Text(
                    text = "  at world.eyes.witness.Runtime.execute(Runtime.kt:42)",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
                Text(
                    text = ">> PENDING EXECUTIONS QUEUED: [12 REMAINING]",
                    color = Color.Yellow,
                    fontFamily = MvFonts.CodeBold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底栏时钟与转储状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "PRESSURE: ${String.format("%.1f", sceneState.progress * 100f)}% | TICK: ${String.format("%.2f", localTime)}s",
                    color = Color.Cyan.copy(alpha = 0.8f),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )

                Text(
                    text = "[ SCENE 09 / 12 ]",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )
            }
        }
    }
}