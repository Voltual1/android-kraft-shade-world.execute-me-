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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import world.eyes.witness.shader.WorldBootGridKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun WorldBootStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { WorldBootGridKraftShader() }

    LaunchedEffect(Unit) {
        effectState.setEffect { targetBuffer ->
            pipeline(targetBuffer) {
                step(shader, targetBuffer)
            }
        }
    }

    // 将音频时钟与拍点脉冲推送到 Shader
    LaunchedEffect(sceneState.globalTimeSeconds, sceneState.beatPulse) {
        shader.time = sceneState.localTimeSeconds
        shader.beatPulse = sceneState.beatPulse
        shader.progress = sceneState.progress
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：3D 透视网格 KraftShader 渲染管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 终端与世界构建数据监控
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 顶栏全息标线与系统状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WORLD SIMULATION INITIALIZER v2.0",
                        color = Color(0xFF00E5FF),
                        fontFamily = MvFonts.Terminal,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "TARGET: goddrinksjava.World(5) -> RUNNING",
                        color = Color(0xFF00FF99),
                        fontFamily = MvFonts.Terminal,
                        fontSize = 13.sp
                    )
                }

                // 实体在线雷达图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f + sceneState.beatPulse * 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3D",
                        color = Color.White,
                        fontFamily = MvFonts.CodeBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 矩阵装载进度条
            LinearProgressIndicator(
                progress = { sceneState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFF00FF99),
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.weight(1f))

            // 中央世界拓扑参数实时日志
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.15f))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(14.dp)
            ) {
                Text(
                    text = ">> STACK LOG: INITIALIZING TOPOLOGICAL SPACE",
                    color = Color(0xFF00E5FF),
                    fontFamily = MvFonts.CodeBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "world.addThing(new Lovable(\"Me\", 0, true, -1, false));",
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 12.sp
                )
                Text(
                    text = "world.addThing(new Lovable(\"You\", 0, false, -1, false));",
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 12.sp
                )
                Text(
                    text = "world.startSimulation();",
                    color = Color(0xFF00FF99),
                    fontFamily = MvFonts.CodeBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底栏拍点同步与时钟
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "PIANO ONSET SYNC: 130 BPM",
                        color = Color.Gray,
                        fontFamily = MvFonts.Terminal,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "PULSE: ${String.format("%.2f", sceneState.beatPulse)} | GRID TICK: ${String.format("%.2f", sceneState.localTimeSeconds)}s",
                        color = Color.Cyan.copy(alpha = 0.8f),
                        fontFamily = MvFonts.Terminal,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "[ SCENE 02 / 12 ]",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )
            }
        }
    }
}