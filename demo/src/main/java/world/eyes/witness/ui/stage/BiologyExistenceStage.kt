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
import world.eyes.witness.shader.BiologyExistenceKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun BiologyExistenceStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { BiologyExistenceKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 0: NUTRIENTS    (0.000 ~ 3.531s)
    // 1: ANTIOXIDANTS (3.531 ~ 7.306s)
    // 2: ENJOYMENT    (7.306 ~ 11.033s)
    // 3: EXISTENCE    (11.033 ~ 14.542s)
    val subPhase = when {
        localTime < 3.531f -> 0
        localTime < 7.306f -> 1
        localTime < 11.033f -> 2
        else -> 3
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isImpact = currentText in listOf(
        "NUTRIENTS",
        "ANTIOXIDANTS",
        "ENJOYMENT",
        "EXISTENCE"
    )

    LaunchedEffect(currentText) {
        if (isImpact) {
            scaleAnim.snapTo(1.4f)
            scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 260))
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
        // 底层：营养素 / 抗氧化剂 / 猫咪呼噜 / 神之存在 KraftShader 管线
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
                        0 -> "ENTITY CLASS: goddrinksjava.Eggplant"
                        1 -> "ENTITY CLASS: goddrinksjava.Tomato"
                        2 -> "ENTITY CLASS: goddrinksjava.TabbyCat"
                        else -> "THEOLOGY: world.getGod().equals(me)"
                    },
                    color = when (subPhase) {
                        0 -> Color(0xFFD500F9)
                        1 -> Color(0xFFFF1744)
                        2 -> Color(0xFFFFAB00)
                        else -> Color(0xFFFFD600)
                    },
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )

                Text(
                    text = "BIOLOGY_SIM",
                    color = Color.Green,
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
                            0 -> Color(0xFFE040FB)
                            1 -> Color(0xFFFF5252)
                            2 -> Color(0xFFFFD54F)
                            else -> Color(0xFFFFF176)
                        },
                        fontFamily = if (isImpact) MvFonts.DisplayBlack else MvFonts.CodeBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isImpact) 40.sp else 24.sp,
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

            // 底栏：底层 Java 源码调用
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (subPhase) {
                        0 -> "you.addAttribute(me.getNutrients().toAttribute()); me.resetNutrients();"
                        1 -> "you.addAttribute(me.getAntioxidants().toAttribute()); me.resetAntioxidants();"
                        2 -> "me.purr(); // TabbyCat enjoyment harmonic oscillation"
                        else -> "if (world.getGod().equals(me)) { me.setProof(you.toProof()); }"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}