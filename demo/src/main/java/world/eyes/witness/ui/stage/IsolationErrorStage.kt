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
import world.eyes.witness.shader.IsolationErrorKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun IsolationErrorStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { IsolationErrorKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 0: VIBRATION / COMPLETION (0.000 ~ 7.411s)
    // 1: ISOLATION (7.411 ~ 14.844s)
    // 2: FRAGMENTS / DISHEARTENED (14.844 ~ 22.219s)
    // 3: ILLEGAL ARGUMENTS (22.219 ~ 30.891s)
    val subPhase = when {
        localTime < 7.411f -> 0
        localTime < 14.844f -> 1
        localTime < 22.219f -> 2
        else -> 3
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isImpact = currentText.contains("ISOLATION") ||
            currentText.contains("ILLEGAL ARGUMENTS") ||
            currentText.contains("COMPLETION") ||
            currentText.contains("FRAGMENTS")

    LaunchedEffect(currentText) {
        if (isImpact) {
            scaleAnim.snapTo(1.45f)
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
        shader.subPhase = subPhase
        shader.beatPulse = sceneState.beatPulse
        shader.glitchIntensity = if (subPhase == 3 || sceneState.isGlitchActive) 1.0f else 0.0f
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：补完 / 孤立坍缩 / 碎片 / 非法参数报错 KraftShader 管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 与动力学歌词排版
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
                        0 -> "STATUS: SENSORY RESONANCE [COMPLETE]"
                        1 -> "CRITICAL: ENTITY \"You\" NOT FOUND [ISOLATION]"
                        2 -> "MEMORY: DELETING UNREFERENCED FRAGMENTS"
                        else -> "EXCEPTION: java.lang.IllegalArgumentException"
                    },
                    color = when (subPhase) {
                        0 -> Color(0xFF00E5FF)
                        1 -> Color(0xFFFF1744)
                        2 -> Color(0xFFFF9100)
                        else -> Color.Red
                    },
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )

                Text(
                    text = if (subPhase == 3) "PANIC!" else "WARN",
                    color = if (subPhase == 3) Color.Red else Color.Yellow,
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 中央：卡点大词动力学排版
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
                            0 -> Color(0xFF18FFFF)
                            1 -> Color(0xFFFF1744)
                            2 -> Color(0xFFFFB74D)
                            else -> Color(0xFFFF0033)
                        },
                        fontFamily = if (isImpact) MvFonts.DisplayBlack else MvFonts.CodeBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isImpact) 38.sp else 24.sp,
                        letterSpacing = if (isImpact) 4.sp else 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    snapshot.nextLine?.let { next ->
                        Text(
                            text = ">> ${next.text}",
                            color = Color.LightGray.copy(alpha = 0.65f),
                            fontFamily = MvFonts.Terminal,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底栏：底层 Java 异常捕获代码
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (subPhase) {
                        0 -> "if (me.getSenseIndex(\"vibration\")) { me.addFeeling(\"complete\"); }"
                        1 -> "world.removeThing(you); // me.lookFor(you, world) -> NOT FOUND"
                        2 -> "if (me.getMemory().isErasable()) me.removeFeeling(\"disheartened\");"
                        else -> "try { me.setOpinion(...); } catch (IllegalArgumentException e) { ... }"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}