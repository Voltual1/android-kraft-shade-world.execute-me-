package world.eyes.witness.ui.screen.color

import androidx.compose.runtime.Composable
import com.cardinalblue.kraftshade.shader.builtin.DifferenceBlendKraftShader

@Composable
fun DifferenceBlendShaderScreen() {
    TwoTextureInputShaderScreen(
        shaderCreator = { DifferenceBlendKraftShader() }
    )
}