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
import world.eyes.witness.shader.RapidExecutionKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun RapidExecutionStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { RapidExecutionKraftShader() }
    val scaleAnim = remember { Animatable(1f) }
    val hitAnim = remember { Animatable(0f) }

    // 12 连击精确索引计算 (02:27.660 起算，前 10.34s 内完成 12 击)
    val strikeIndex = when {
        localTime < 0.940f -> 1
        localTime < 1.860f -> 2
        localTime < 2.880f -> 3
        localTime < 3.860f -> 4
        localTime < 4.620f -> 5
        localTime < 5.500f -> 6
        localTime < 6.320f -> 7
        localTime < 7.540f -> 8
        localTime < 8.420f -> 9
        localTime < 9.380f -> 10
        localTime < 10.340f -> 11
        localTime < 11.240f -> 12
        else -> 13
    }

    val currentText = snapshot.currentLine?.text ?: ""
    val isCounting = currentText.contains("Ein") ||
            currentText.contains("Trois") ||
            currentText.contains("Fem")

    // 每逢新的唱词换行或新的处决点触发强震颤与暴击动画
    LaunchedEffect(currentText, strikeIndex) {
        scaleAnim.snapTo(1.5f)
        scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 200))
        hitAnim.snapTo(1.0f)
        hitAnim.animateTo(0.0f, animationSpec = tween(durationMillis = 180))
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
        shader.strikeIndex = strikeIndex
        shader.hitProgress = hitAnim.value
        shader.beatPulse = sceneState.beatPulse
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：12 连击处决风暴与强闪 KraftShader 管线
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        // 顶层：HUD 告警与暴击文字排版
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 顶栏危险警报
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCounting) "BROADCAST: MULTILINGUAL COUNTDOWN" else "EXECUTION SEQUENCE: [STRIKE #$strikeIndex / 12]",
                    color = Color.Red,
                    fontFamily = MvFonts.Terminal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "MAX_KILL",
                    color = Color.Yellow,
                    fontFamily = MvFonts.Terminal,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 中央：超大震撼排版唱词
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentText,
                        color = if (isCounting) Color(0xFFFFD600) else Color(0xFFFF1744),
                        fontFamily = MvFonts.DisplayBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isCounting) 36.sp else 46.sp,
                        letterSpacing = 5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    snapshot.nextLine?.let { next ->
                        Text(
                            text = ">> ${next.text}",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontFamily = MvFonts.Terminal,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底栏：代码世界宣告与连续处决执行
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Red.copy(alpha = 0.4f))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isCounting) {
                        "world.announce(\"1\",\"de\"); world.announce(\"2\",\"es\"); world.announce(\"3\",\"fr\"); world.announce(\"4\",\"kr\"); world.announce(\"5\",\"se\"); world.announce(\"6\",\"cn\");"
                    } else {
                        "for (int i = 0; i < 12; i++) { world.runExecution(); } // STRIKE #$strikeIndex"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp
                )
            }
        }
    }
}