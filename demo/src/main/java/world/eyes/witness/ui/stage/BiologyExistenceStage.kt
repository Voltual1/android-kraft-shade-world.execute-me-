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
import world.eyes.witness.shader.BiologyExistenceKraftShader
import world.eyes.witness.shader.CrtTerminalKraftShader
import world.eyes.witness.timeline.TimelineSnapshot

@Composable
fun BiologyExistenceStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { BiologyExistenceKraftShader() }
    val crtShader = remember { CrtTerminalKraftShader() }

    val subPhase = when {
        localTime < 3.531f -> 0
        localTime < 7.306f -> 1
        localTime < 11.033f -> 2
        else -> 3
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
        shader.subPhase = subPhase
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