package world.eyes.witness.ui.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import me.voltual.asp.Context as StoryContext
import me.voltual.asp.loadContextFromAssets
import me.voltual.asp.ui.StoryPlayerScreen
import world.eyes.witness.director.SceneState
import world.eyes.witness.shader.CrtTerminalKraftShader
import world.eyes.witness.shader.ShutdownKraftShader
import world.eyes.witness.timeline.TimelineSnapshot

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

    val androidContext = LocalContext.current
    var scriptText by remember { mutableStateOf<String?>(null) }
    var storyContext by remember { mutableStateOf<StoryContext?>(null) }

    val isFinalExecution = localTime in 12.351f..13.160f
    val isShutdownOutro = localTime >= 13.160f

    // 预加载本地故事剧本资源
    LaunchedEffect(Unit) {
        try {
            val (text, ctx) = loadContextFromAssets(androidContext, "story/birthday_mousse")
            scriptText = text
            storyContext = ctx
        } catch (_: Throwable) {
        }
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
        if (!isShutdownOutro) {
            KraftShadeEffectView(
                modifier = Modifier.fillMaxSize(),
                state = effectState
            )

            KineticStageOverlay(
                sceneState = sceneState,
                snapshot = snapshot,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 黑屏时刻无缝衔接进入剧情播放
            val script = scriptText
            val context = storyContext
            if (script != null && context != null) {
                StoryPlayerScreen(
                    scriptText = script,
                    initialContext = context,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}