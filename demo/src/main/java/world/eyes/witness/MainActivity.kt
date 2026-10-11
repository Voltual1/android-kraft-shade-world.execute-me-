package world.eyes.witness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import world.eyes.witness.audio.MvAudioPlayer
import world.eyes.witness.director.SceneDirector
import world.eyes.witness.director.SceneState
import world.eyes.witness.timeline.LrcLine
import world.eyes.witness.timeline.LrcParser
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.KraftShadeTheme

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayer: MvAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioPlayer = MvAudioPlayer(this).apply {
            prepare("original/Mili - world.execute (me) ;.mp3")
        }

        setContent {
            KraftShadeTheme(darkTheme = true) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color.Black)
                    ) {
                        WorldExecuteMeStage(audioPlayer = audioPlayer)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
    }
}

@Composable
fun WorldExecuteMeStage(audioPlayer: MvAudioPlayer) {
    val context = LocalContext.current
    val timeSeconds by audioPlayer.playbackTimeSeconds.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()

    val lrcLines: List<LrcLine> = remember {
        context.assets.open("original/world.execute(me)-timeline.lrc").use { stream ->
            LrcParser.parse(stream)
        }
    }

    val snapshot: TimelineSnapshot = remember(timeSeconds, lrcLines) {
        LrcParser.findSnapshot(lrcLines, timeSeconds)
    }

    val sceneState: SceneState = remember(timeSeconds) {
        SceneDirector.resolveState(timeSeconds)
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.pause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (isPlaying) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.play()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // 顶栏：系统状态与当前章节指示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[ SCENE: ${sceneState.scene.name} ]",
                    color = if (sceneState.isGlitchActive) Color.Red else Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = if (isPlaying) "RUNNING" else "HALTED",
                    color = if (isPlaying) Color.Green else Color.Yellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 章节内部进度条
            LinearProgressIndicator(
                progress = { sceneState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = if (sceneState.isGlitchActive) Color.Red else Color.Green,
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.weight(1f))

            // 节拍脉冲与时间展示
            Text(
                text = String.format("CLOCK: %06.3f s (BPM 130)", sceneState.globalTimeSeconds),
                color = Color.White.copy(alpha = 0.5f + sceneState.beatPulse * 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 主歌词卡点文本
            Text(
                text = snapshot.currentLine?.text ?: "world.execute(me);",
                color = if (sceneState.isGlitchActive) Color.Red else Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            snapshot.nextLine?.let { next ->
                Text(
                    text = ">> ${next.text}",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底栏：场景元数据
            Text(
                text = "STAGE: ${sceneState.scene.title} | LOCAL: ${String.format("%.2f", sceneState.localTimeSeconds)}s",
                color = Color.LightGray.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}