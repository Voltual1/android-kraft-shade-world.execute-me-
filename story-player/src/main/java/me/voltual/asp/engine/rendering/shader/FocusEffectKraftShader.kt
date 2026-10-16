package me.voltual.asp.engine.rendering.shader

import com.cardinalblue.kraftshade.shader.TextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

/**
 * Port of Torappu.AVG.AVGSceneFocusOut:
 * Combines radial/directional sampling blur with optional grayscale or color inverse.
 */
class FocusEffectKraftShader(
    blurAmount: Float = 0f,
    grayAmount: Float = 0f,
    inverseAmount: Float = 0f
) : TextureInputKraftShader(
    samplerUniformName = "inputImageTexture"
) {
    var blurAmount: Float by GlUniformDelegate("uBlurAmount")
    var grayAmount: Float by GlUniformDelegate("uGrayAmount")
    var inverseAmount: Float by GlUniformDelegate("uInverseAmount")

    init {
        this.blurAmount = blurAmount
        this.grayAmount = grayAmount
        this.inverseAmount = inverseAmount
    }

    override fun loadFragmentShader(): String = FOCUS_EFFECT_FRAGMENT_SHADER
}

@Language("GLSL")
private const val FOCUS_EFFECT_FRAGMENT_SHADER = """
precision mediump float;
varying highp vec2 textureCoordinate;

uniform sampler2D inputImageTexture;
uniform float uBlurAmount;
uniform float uGrayAmount;
uniform float uInverseAmount;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    
    // Multi-tap blur approximation
    vec4 blurred = baseColor * 0.227027;
    float stepOffset = uBlurAmount * 0.004;
    
    blurred += texture2D(inputImageTexture, textureCoordinate + vec2(stepOffset, 0.0)) * 0.1945946;
    blurred += texture2D(inputImageTexture, textureCoordinate - vec2(stepOffset, 0.0)) * 0.1945946;
    blurred += texture2D(inputImageTexture, textureCoordinate + vec2(0.0, stepOffset)) * 0.1918918;
    blurred += texture2D(inputImageTexture, textureCoordinate - vec2(0.0, stepOffset)) * 0.1918918;

    vec3 rgb = mix(baseColor.rgb, blurred.rgb, clamp(uBlurAmount, 0.0, 1.0));

    // Optional Color Inversion
    rgb = mix(rgb, 1.0 - rgb, clamp(uInverseAmount, 0.0, 1.0));

    // Optional Grayscale
    float gray = dot(rgb, LUMA);
    rgb = mix(rgb, vec3(gray), clamp(uGrayAmount, 0.0, 1.0));

    gl_FragColor = vec4(rgb, baseColor.a);
}
"""