package me.voltual.asp.shader

import com.cardinalblue.kraftshade.shader.TextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

/**
 * Native port of Torappu/PostEffect/Grayscale shader used by cameraeffect and focusout.
 * Formula:
 * inverted = mix(color.rgb, vec3(1.0) - color.rgb, inverseAmount);
 * gray = dot(inverted, vec3(0.299, 0.587, 0.114));
 * out = mix(inverted, vec3(gray), grayAmount);
 */
class StoryCameraEffectKraftShader(
    grayAmount: Float = 0f,
    inverseAmount: Float = 0f
) : TextureInputKraftShader() {

    var grayAmount: Float by GlUniformDelegate("grayAmount")
    var inverseAmount: Float by GlUniformDelegate("inverseAmount")

    init {
        this.grayAmount = grayAmount
        this.inverseAmount = inverseAmount
    }

    override fun loadFragmentShader(): String = CAMERA_EFFECT_FRAGMENT_SHADER

    companion object {
        @Language("GLSL")
        private const val CAMERA_EFFECT_FRAGMENT_SHADER = """
            precision highp float;
            varying highp vec2 textureCoordinate;

            uniform sampler2D inputImageTexture;
            uniform highp float grayAmount;
            uniform highp float inverseAmount;

            const highp vec3 luma = vec3(0.299, 0.587, 0.114);

            void main() {
                lowp vec4 color = texture2D(inputImageTexture, textureCoordinate);
                lowp vec3 inverted = mix(color.rgb, vec3(1.0) - color.rgb, inverseAmount);
                lowp float gray = dot(inverted, luma);
                lowp vec3 result = mix(inverted, vec3(gray), grayAmount);
                gl_FragColor = vec4(result, color.a);
            }
        """
    }
}