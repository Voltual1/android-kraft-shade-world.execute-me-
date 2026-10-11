package world.eyes.witness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import world.eyes.witness.audio.MvAudioPlayer
import world.eyes.witness.director.SceneDirector
import world.eyes.witness.director.SceneState
import world.eyes.witness.director.SceneType
import world.eyes.witness.timeline.LrcLine
import world.eyes.witness.timeline.LrcParser
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.stage.BiologyExistenceStage
import world.eyes.witness.ui.stage.BootSequenceStage
import world.eyes.witness.ui.stage.CurrentConversionStage
import world.eyes.witness.ui.stage.GenderRolesStage
import world.eyes.witness.ui.stage.GeometryMathStage
import world.eyes.witness.ui.stage.IsolationErrorStage
import world.eyes.witness.ui.stage.SimulationExecutionStage
import world.eyes.witness.ui.stage.WorldBootStage
import world.eyes.witness.ui.theme.KraftShadeTheme
import world.eyes.witness.ui.theme.MvFonts

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayer: MvAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MvFonts.init(this)

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
            }
    ) {
        when (sceneState.scene) {
            SceneType.BOOT_SEQUENCE -> {
                BootSequenceStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.WORLD_BOOT_INSTRUMENTAL -> {
                WorldBootStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.GEOMETRY_MATH -> {
                GeometryMathStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.CURRENT_CONVERSION -> {
                CurrentConversionStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.SIMULATION_EXECUTION -> {
                SimulationExecutionStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.BIOLOGY_EXISTENCE -> {
                BiologyExistenceStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.GENDER_AND_ROLES -> {
                GenderRolesStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.ISOLATION_AND_ERROR -> {
                IsolationErrorStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ ${sceneState.scene.title} ]\n${snapshot.currentLine?.text ?: ""}",
                        color = Color.White,
                        fontFamily = MvFonts.CodeBold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        if (!isPlaying) {
            Text(
                text = "TAP SCREEN TO EXECUTE",
                color = Color.Green.copy(alpha = 0.7f),
                fontFamily = MvFonts.Terminal,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}