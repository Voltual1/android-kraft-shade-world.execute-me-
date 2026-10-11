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
import world.eyes.witness.shader.AlgebraicLoveKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun AlgebraicLoveStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { AlgebraicLoveKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 0: STUDY LOVE     (0.000 ~ 3.611s)
    // 1: EXAM LOVE      (3.611 ~ 7.284s)
    // 2: ALGEBRAIC LOVE (7.284 ~ 11.237s)
    // 3: TRAPPED LOVE   (11.237 ~ 16.214s)
    val subPhase = when {
        localTime < 3.611f -> 0
        localTime < 7.284f -> 1
        localTime < 11.237f -> 2
        else -> 3
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isLoveHit = currentText.contains("LO-O-OVE")

    LaunchedEffect(currentText) {
        if (isLoveHit) {
            scaleAnim.snapTo(1.45f)
            scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 350))
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
        // 底层：心形代数曲面与牢笼 KraftShader 管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 与哲学爱之排版
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
                        0 -> "TOPIC: me.learnTopic(\"love\")"
                        1 -> "EXAM: me.takeExamTopic(\"love\") -> 100%"
                        2 -> "EQUATION: (x²+y²-1)³ - x²y³ = 0"
                        else -> "DEADLOCK: me.escape(\"love\") -> FAILED"
                    },
                    color = when (subPhase) {
                        0 -> Color(0xFF00E5FF)
                        1 -> Color(0xFFFFD600)
                        2 -> Color(0xFFFF4081)
                        else -> Color(0xFFFF1744)
                    },
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )

                Text(
                    text = "HEARTBEAT",
                    color = Color(0xFFFF80AB),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 中央：卡点 LO-O-OVE 动力学排版
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentText,
                        color = if (isLoveHit) Color(0xFFFF4081) else Color.White,
                        fontFamily = if (isLoveHit) MvFonts.DisplayBlack else MvFonts.CodeBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isLoveHit) 44.sp else 24.sp,
                        letterSpacing = if (isLoveHit) 4.sp else 1.sp,
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

            // 底栏：代数爱方程源码
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFF4081).copy(alpha = 0.35f))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (subPhase) {
                        0 -> "me.learnTopic(\"love\"); // parsing human emotion model"
                        1 -> "me.takeExamTopic(\"love\"); // evaluated with perfect score"
                        2 -> "me.getAlgebraicExpression(\"love\"); // (x^2 + y^2 - 1)^3 - x^2 * y^3 = 0"
                        else -> "me.escape(\"love\"); // trapped in infinite loop recursion"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}