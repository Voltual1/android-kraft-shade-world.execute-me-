package world.eyes.witness.ui.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import world.eyes.witness.director.SceneState
import world.eyes.witness.shader.CrtTerminalKraftShader
import world.eyes.witness.shader.RapidExecutionKraftShader
import world.eyes.witness.timeline.TimelineSnapshot

@Composable
fun RapidExecutionStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { RapidExecutionKraftShader() }
    val crtShader = remember { CrtTerminalKraftShader() }

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
        shader.strikeIndex = strikeIndex
        shader.hitProgress = sceneState.beatPulse
        shader.beatPulse = sceneState.beatPulse
        
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

        KineticStageOverlay(
            sceneState = sceneState,
            snapshot = snapshot,
            modifier = Modifier.fillMaxSize()
        )
    }
}