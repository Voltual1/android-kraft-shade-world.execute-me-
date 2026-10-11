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
import world.eyes.witness.shader.GeometryMathKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun GeometryMathStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { GeometryMathKraftShader() }
    val scaleAnim = remember { Animatable(1f) }

    // 判断当前细分子阶段 (0: 点集/维度, 1: 圆/周长, 2: 正弦/切线, 3: 无穷/极限)
    val mathPhase = when {
        localTime < 3.703f -> 0
        localTime < 7.358f -> 1
        localTime < 10.997f -> 2
        else -> 3
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isImpactWord = currentText in listOf(
        "DIMENSION",
        "CIRCUMFERENCE",
        "TANGENTS",
        "LIMITATIONS"
    )

    LaunchedEffect(currentText) {
        if (isImpactWord) {
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
        shader.mathPhase = mathPhase
        shader.beatPulse = sceneState.beatPulse
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：几何数学 KraftShader 片元着色器
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 排版与数学命题
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 顶栏：数学几何命题断言
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (mathPhase) {
                        0 -> "PROPOSITION 01: me instanceof PointSet"
                        1 -> "PROPOSITION 02: me instanceof Circle"
                        2 -> "PROPOSITION 03: me instanceof SineWave"
                        else -> "PROPOSITION 04: me instanceof Sequence"
                    },
                    color = Color.Cyan,
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )

                Text(
                    text = "BPM 130",
                    color = Color.Yellow,
                    fontFamily = MvFonts.Terminal,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 中央：歌词动力学排版
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isImpactWord) {
                        Text(
                            text = currentText,
                            color = when (mathPhase) {
                                0 -> Color(0xFF00E5FF)
                                1 -> Color(0xFFFF4081)
                                2 -> Color(0xFF76FF03)
                                else -> Color(0xFFFF6D00)
                            },
                            fontFamily = MvFonts.DisplayBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = currentText,
                            color = Color.White,
                            fontFamily = MvFonts.CodeBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    snapshot.nextLine?.let { next ->
                        Text(
                            text = ">> ${next.text}",
                            color = Color.LightGray.copy(alpha = 0.6f),
                            fontFamily = MvFonts.Terminal,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底栏：调用语句与参数
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (mathPhase) {
                        0 -> "you.addAttribute(me.getDimensions().toAttribute()); me.resetDimentions();"
                        1 -> "you.addAttribute(me.getCircumference().toAttribute()); me.resetCircumference();"
                        2 -> "you.addAction(\"sit\", me.getTangent(you.getXPosition()));"
                        else -> "me.setLimit(you.toLimit()); // lim(x -> inf)"
                    },
                    color = Color(0xFF80D8FF),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}