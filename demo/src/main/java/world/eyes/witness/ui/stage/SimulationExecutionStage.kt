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
import world.eyes.witness.shader.SimulationExecutionKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun SimulationExecutionStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { SimulationExecutionKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 0: STIMULATIONS (0.00 ~ 3.366s)
    // 1: SATISFACTION (3.366 ~ 7.378s)
    // 2: EXECUTION    (7.378 ~ 10.861s)
    // 3: SIMULATION   (10.861 ~ 14.822s)
    val subPhase = when {
        localTime < 3.366f -> 0
        localTime < 7.378f -> 1
        localTime < 10.861f -> 2
        else -> 3
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isImpact = currentText in listOf(
        "STIMULATIONS",
        "SATISFACTION",
        "EXECUTION",
        "SIMULATION"
    )

    LaunchedEffect(currentText) {
        if (isImpact) {
            scaleAnim.snapTo(1.45f)
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
        // 底层：刺激 / 满足 / 处决 / 牢笼 KraftShader 管线
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
                        0 -> "PROCESS: SENSORY STIMULATION [BUFFER=FULL]"
                        1 -> "PROCESS: SATISFACTION METRIC [OVERFLOW]"
                        2 -> "ALERT: RUN_EXECUTION() INVOKED"
                        else -> "CONTAINMENT: ENTITY LOCKED [CELL_01]"
                    },
                    color = when (subPhase) {
                        0 -> Color(0xFF00E5FF)
                        1 -> Color(0xFFFF4081)
                        2 -> Color(0xFFFF1744)
                        else -> Color(0xFF2979FF)
                    },
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )

                Text(
                    text = if (subPhase == 2) "LETHAL" else "SIM_RUN",
                    color = if (subPhase == 2) Color.Red else Color.Green,
                    fontFamily = MvFonts.Terminal,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 中央：卡点大词爆发动效
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
                            1 -> Color(0xFFFF80AB)
                            2 -> Color(0xFFFF1744)
                            else -> Color(0xFF82B1FF)
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

            // 底栏：底层 Java 源码执行日志
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (subPhase) {
                        0 -> "if (me.getNumSimulationsAvailable() >= you.getNumSimulationsNeeded())"
                        1 -> "you.setSatisfaction(me.toSatisfaction()); // 100% capacity"
                        2 -> "if (you.getFeelingIndex(\"happy\") != -1) me.requestExecution(world);"
                        else -> "world.lockThing(me); world.lockThing(you); // trapped"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}