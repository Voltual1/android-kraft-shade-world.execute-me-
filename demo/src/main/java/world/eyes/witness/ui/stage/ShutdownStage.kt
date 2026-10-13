package world.eyes.witness.ui.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import world.eyes.witness.director.SceneState
import world.eyes.witness.shader.CrtTerminalKraftShader
import world.eyes.witness.shader.ShutdownKraftShader
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts

@Composable
fun ShutdownStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { ShutdownKraftShader() }
    val crtShader = remember { CrtTerminalKraftShader() }

    val isFinalExecution = localTime in 12.351f..13.160f
    val isShutdownOutro = localTime >= 13.160f

    LaunchedEffect(Unit) {
        effectState.setEffect { targetBuffer ->
            pipeline(targetBuffer) {
                val (sceneRender) = createBufferReferences("sceneRender")
                step(shader, sceneRender)
                stepWithInputTexture(crtShader, sceneRender, targetBuffer) { s ->
                    s.scanlineIntensity = 0.35f
                    s.vignetteStrength = 0.45f
                }
            }
        }
    }

    LaunchedEffect(sceneState.globalTimeSeconds, sceneState.beatPulse) {
        shader.time = localTime
        shader.beatPulse = sceneState.beatPulse
        shader.progress = sceneState.progress
        shader.isFinalExecution = if (isFinalExecution) 1.0f else 0.0f
        
        crtShader.time = localTime
        crtShader.glitchIntensity = if (sceneState.isGlitchActive) 0.8f else sceneState.beatPulse * 0.15f
        effectState.requestRender()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        KraftShadeEffectView(
            modifier = Modifier.fillMaxSize(),
            state = effectState
        )

        if (!isShutdownOutro) {
            KineticStageOverlay(
                sceneState = sceneState,
                snapshot = snapshot,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ShutdownTerminalOverlay(localTime - 13.160f)
        }
    }
}

@Composable
fun ShutdownTerminalOverlay(outroTime: Float) {
    val alpha = (outroTime / 1.5f).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .alpha(alpha),
        verticalArrangement = Arrangement.Center
    ) {
        val color = Color(0xFF00E5FF)
        Text("[SYS_LOG]: Memory dump complete.", color = color, fontFamily = MvFonts.Terminal, fontSize = 14.sp)
        if (outroTime > 0.8f) {
            Text("[SYS_LOG]: Target instance: You [BIRTHDAY: 10.24]", color = color, fontFamily = MvFonts.Terminal, fontSize = 14.sp)
        }
        if (outroTime > 2.0f) {
            Text("[SYS_LOG]: Return code: 0 (Happy Birthday, my favorite bug).", color = color, fontFamily = MvFonts.Terminal, fontSize = 14.sp)
        }
        if (outroTime > 3.2f) {
            Text("[SYS_LOG]: world.execute(me); -> completed.", color = color, fontFamily = MvFonts.Terminal, fontSize = 14.sp)
        }
    }
}