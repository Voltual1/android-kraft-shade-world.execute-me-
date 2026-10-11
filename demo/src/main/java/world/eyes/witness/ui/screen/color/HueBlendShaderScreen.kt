package world.eyes.witness.ui.screen.color

import androidx.compose.runtime.Composable
import com.cardinalblue.kraftshade.shader.builtin.HueBlendKraftShader

@Composable
fun HueBlendShaderScreen() {
    TwoTextureInputShaderScreen(
        shaderCreator = { HueBlendKraftShader() }
    )
}