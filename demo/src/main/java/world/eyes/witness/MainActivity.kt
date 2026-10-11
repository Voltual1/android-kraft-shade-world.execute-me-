package world.eyes.witness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = if (isPlaying) "PAUSE" else "TAP TO PLAY",
                color = Color.Green.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = String.format("%.3f s", timeSeconds),
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = snapshot.currentLine?.text ?: "world.execute(me);",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            snapshot.nextLine?.let { next ->
                Text(
                    text = ">> ${next.text}",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}