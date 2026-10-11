package world.eyes.witness.ui.stage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import world.eyes.witness.director.SceneState
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun BootSequenceStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val scaleAnim = remember { Animatable(1f) }

    // 当歌词跳入全大写关键词时触发冲击动效
    val currentText = snapshot.currentLine?.text ?: ""
    val isImpactWord = currentText in listOf(
        "PROTECTION",
        "OBJECT CREATION",
        "INITIALIZATION",
        "SIMULATION"
    )

    LaunchedEffect(currentText) {
        if (isImpactWord) {
            scaleAnim.snapTo(1.35f)
            scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 280))
        } else {
            scaleAnim.snapTo(1.0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {
        // 顶部终端打字输出
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)
        ) {
            Text(
                text = ">>> POWER LINE CONNECTED: 220V AC -> DC STABILIZED",
                color = Color(0xFF00FF66),
                fontFamily = MvFonts.Terminal,
                fontSize = 15.sp
            )

            if (localTime >= 1.74f) {
                Text(
                    text = ">>> MEMORY STACK: ALLOCATING PROTECTION SHIELD...",
                    color = Color(0xFF00EE55),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )
            }

            if (localTime >= 3.87f) {
                Text(
                    text = ">>> CONSTRUCTING ENTITY GRAPH: goddrinksjava.World",
                    color = Color(0xFF00DD44),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )
            }

            if (localTime >= 7.44f) {
                Text(
                    text = ">>> INJECTING PARAMETERS: dim=5, me=Lovable, you=Lovable",
                    color = Color(0xFF00CC33),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )
            }

            if (localTime >= 11.09f) {
                Text(
                    text = ">>> ENVIRONMENT SIMULATION BOOT READY.",
                    color = Color(0xFF00FF88),
                    fontFamily = MvFonts.Terminal,
                    fontSize = 15.sp
                )
            }
        }

        // 中央动态排版唱词
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(scaleAnim.value)
            ) {
                if (isImpactWord) {
                    Text(
                        text = currentText,
                        color = Color.White,
                        fontFamily = MvFonts.DisplayBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = currentText,
                        color = Color(0xFFEEEEEE),
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
                        color = Color.DarkGray,
                        fontFamily = MvFonts.Terminal,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 底部自检状态
        Text(
            text = "SYSTEM STATUS: POST OK | TICK: ${String.format("%05.2f", localTime)}s",
            color = Color(0xFF007722),
            fontFamily = MvFonts.Terminal,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}