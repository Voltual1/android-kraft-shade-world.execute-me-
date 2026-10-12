package world.eyes.witness

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import world.eyes.witness.audio.MvAudioPlayer
import world.eyes.witness.director.SceneDirector
import world.eyes.witness.director.SceneState
import world.eyes.witness.director.SceneType
import world.eyes.witness.timeline.LrcLine
import world.eyes.witness.timeline.LrcParser
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.components.TouchTrailCanvas
import world.eyes.witness.ui.stage.AlgebraicLoveStage
import world.eyes.witness.ui.stage.ArgumentStackStage
import world.eyes.witness.ui.stage.BiologyExistenceStage
import world.eyes.witness.ui.stage.BootSequenceStage
import world.eyes.witness.ui.stage.CurrentConversionStage
import world.eyes.witness.ui.stage.DesperateExecutionStage
import world.eyes.witness.ui.stage.GenderRolesStage
import world.eyes.witness.ui.stage.GeometryMathStage
import world.eyes.witness.ui.stage.IsolationErrorStage
import world.eyes.witness.ui.stage.RapidExecutionStage
import world.eyes.witness.ui.stage.ShutdownStage
import world.eyes.witness.ui.stage.SimulationExecutionStage
import world.eyes.witness.ui.stage.WorldBootStage
import world.eyes.witness.ui.theme.KraftShadeTheme
import world.eyes.witness.ui.theme.MvFonts

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayer: MvAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveMode()

        MvFonts.init(this)

        audioPlayer = MvAudioPlayer(this).apply {
            prepare("original/Mili - world.execute (me) ;.mp3")
        }

        setContent {
            KraftShadeTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    WorldExecuteMeStage(audioPlayer = audioPlayer)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 底层各分镜内容与 Shader 渲染
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
            SceneType.ARGUMENT_STACK -> {
                ArgumentStackStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.RAPID_EXECUTION_STORM -> {
                RapidExecutionStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.DESPERATE_EXECUTION -> {
                DesperateExecutionStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.ALGEBRAIC_LOVE -> {
                AlgebraicLoveStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SceneType.OPEN_LOOP_SHUTDOWN -> {
                ShutdownStage(
                    sceneState = sceneState,
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 最顶层：触控轨迹跟随 Canvas（划动留光，短触切换播放/暂停）
        TouchTrailCanvas(
            onTap = {
                if (isPlaying) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.play()
                }
            },
            trailColor = when (sceneState.scene) {
                SceneType.ALGEBRAIC_LOVE -> Color(0xFFFF4081)
                SceneType.RAPID_EXECUTION_STORM,
                SceneType.SIMULATION_EXECUTION -> Color(0xFFFF1744)
                SceneType.CURRENT_CONVERSION -> Color(0xFFFFD600)
                else -> Color(0xFF00E5FF)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 未播放时的交互提示
        if (!isPlaying) {
            Text(
                text = "TAP SCREEN TO EXECUTE",
                color = Color.Green.copy(alpha = 0.7f),
                fontFamily = MvFonts.Terminal,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
            )
        }
    }
}