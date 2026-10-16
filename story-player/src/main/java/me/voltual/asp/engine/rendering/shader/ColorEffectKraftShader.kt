package me.voltual.asp.engine.rendering.shader

import com.cardinalblue.kraftshade.shader.TextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

/**
 * Native port: AVG/[UC]Common/Arts/Materials/mat_grayscale (Torappu/PostEffect/Grayscale)
 * Supports continuous grayscale and color-inverse effects.
 */
class ColorEffectKraftShader(
    grayAmount: Float = 0f,
    inverseAmount: Float = 0f
) : TextureInputKraftShader(
    samplerUniformName = "inputImageTexture"
) {
    var grayAmount: Float by GlUniformDelegate("uGray")
    var inverseAmount: Float by GlUniformDelegate("uInverse")

    init {
        this.grayAmount = grayAmount
        this.inverseAmount = inverseAmount
    }

    override fun loadFragmentShader(): String = COLOR_EFFECT_FRAGMENT_SHADER
}

@Language("GLSL")
private const val COLOR_EFFECT_FRAGMENT_SHADER = """
precision mediump float;
varying highp vec2 textureCoordinate;

uniform sampler2D inputImageTexture;
uniform float uGray;
uniform float uInverse;

// Rec.601 Luma coefficients
const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 color = texture2D(inputImageTexture, textureCoordinate);
    // Apply inversion first as defined in Torappu shader: A = lerp(color, 1.0 - color, _Inverse)
    vec3 inverted = mix(color.rgb, 1.0 - color.rgb, clamp(uInverse, 0.0, 1.0));
    // Apply grayscale on inverted result: out = lerp(A, dot(A, Luma), _Gray)
    float grayVal = dot(inverted, LUMA);
    vec3 result = mix(inverted, vec3(grayVal), clamp(uGray, 0.0, 1.0));
    gl_FragColor = vec4(result, color.a);
}
"""