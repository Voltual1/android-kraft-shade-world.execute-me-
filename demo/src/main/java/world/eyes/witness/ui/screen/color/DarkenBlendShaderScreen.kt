package world.eyes.witness.ui.screen.color

import androidx.compose.runtime.Composable
import com.cardinalblue.kraftshade.shader.builtin.DarkenBlendKraftShader

@Composable
fun DarkenBlendShaderScreen() {
    TwoTextureInputShaderScreen(
        shaderCreator = { DarkenBlendKraftShader() }
    )
}