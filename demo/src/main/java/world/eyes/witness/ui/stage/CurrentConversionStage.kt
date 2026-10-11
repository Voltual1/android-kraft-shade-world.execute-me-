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
import world.eyes.witness.shader.CurrentConversionKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun CurrentConversionStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { CurrentConversionKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 0: AC/DC (0.00 ~ 3.22s), 1: DIZZY (3.22 ~ 6.91s), 2: AD/BC (6.91 ~ 10.63s), 3: UNITE (10.63 ~ 14.77s)
    val subPhase = when {
        localTime < 3.220f -> 0
        localTime < 6.911f -> 1
        localTime < 10.631f -> 2
        else -> 3
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isImpact = currentText.contains("AC") ||
            currentText.contains("dizzy") ||
            currentText.contains("A.D.") ||
            currentText.contains("deeply")

    LaunchedEffect(currentText) {
        if (isImpact) {
            scaleAnim.snapTo(1.35f)
            scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 250))
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
        shader.subPhase = subPhase
        shader.beatPulse = sceneState.beatPulse
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：电流转换 / 眩晕 / 穿越 / 联合 KraftShader 管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 与动力学排版
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 顶栏状态指示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (subPhase) {
                        0 -> "STATUS: CURRENT TOGGLE [AC <-> DC]"
                        1 -> "STATUS: OPTICAL BLINDED [CAN_SEE=FALSE]"
                        2 -> "STATUS: CHRONO TIME TRAVEL [A.D. / B.C.]"
                        else -> "STATUS: QUANTUM ENTANGLEMENT [UNITE]"
                    },
                    color = when (subPhase) {
                        0 -> Color.Yellow
                        1 -> Color.Red
                        2 -> Color.Cyan
                        else -> Color(0xFFFF4081)
                    },
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )

                Text(
                    text = "ENERGY: ${String.format("%.1f", (sceneState.beatPulse * 100f))}%",
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 中央：卡点主歌词动力学呈现
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentText,
                        color = when (subPhase) {
                            0 -> Color(0xFFFFD600)
                            1 -> Color(0xFFFF1744)
                            2 -> Color(0xFF00E5FF)
                            else -> Color(0xFFE040FB)
                        },
                        fontFamily = if (isImpact) MvFonts.DisplayBlack else MvFonts.CodeBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isImpact) 36.sp else 24.sp,
                        letterSpacing = if (isImpact) 3.sp else 1.sp,
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

            // 底栏：底层 Java 运行时方法调用日志
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (subPhase) {
                        0 -> "me.toggleCurrent(); // alternating <-> direct"
                        1 -> "me.canSee(false); me.addFeeling(\"dizzy\");"
                        2 -> "world.timeTravelForTwo(\"AC\", 617, me, you); world.timeTravelForTwo(\"BC\", 3691, me, you);"
                        else -> "world.unite(me, you); // consciousness singularity"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}