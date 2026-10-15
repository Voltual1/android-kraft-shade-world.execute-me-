package me.voltual.asp.engine.rendering.shader

import com.cardinalblue.kraftshade.shader.TextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class ColorEffectKraftShader(
    grayAmount: Float = 0f,
    inverseAmount: Float = 0f
) : TextureInputKraftShader() {

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

const vec3 luma = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 color = texture2D(inputImageTexture, textureCoordinate);
    vec3 inverted = mix(color.rgb, 1.0 - color.rgb, uInverse);
    float grayVal = dot(inverted, luma);
    vec3 result = mix(inverted, vec3(grayVal), uGray);
    gl_FragColor = vec4(result, color.a);
}
"""