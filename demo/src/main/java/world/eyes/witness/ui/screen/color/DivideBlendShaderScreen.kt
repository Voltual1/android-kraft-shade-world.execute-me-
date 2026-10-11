package world.eyes.witness.ui.screen.color

import androidx.compose.runtime.Composable
import com.cardinalblue.kraftshade.shader.builtin.DivideBlendKraftShader

@Composable
fun DivideBlendShaderScreen() {
    TwoTextureInputShaderScreen(
        shaderCreator = { DivideBlendKraftShader() }
    )
}