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
import world.eyes.witness.shader.CurrentConversionKraftShader
import world.eyes.witness.timeline.TimelineSnapshot

@Composable
fun CurrentConversionStage(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val localTime = sceneState.localTimeSeconds
    val effectState = rememberKraftShadeEffectState()
    val shader = remember { CurrentConversionKraftShader() }

    val subPhase = when {
        localTime < 3.220f -> 0
        localTime < 6.911f -> 1
        localTime < 10.631f -> 2
        else -> 3
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
        shader.subPhase = subPhase
        shader.beatPulse = sceneState.beatPulse
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