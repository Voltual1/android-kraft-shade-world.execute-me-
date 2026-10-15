package me.voltual.asp.engine.rendering.shader

import com.cardinalblue.kraftshade.shader.TwoTextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class SlideMaskKraftShader : TwoTextureInputKraftShader(
    samplerUniformName = "uTexture",
    secondTextureSampleName = "uMaskTexture"
) {
    var alpha: Float by GlUniformDelegate("uAlpha")
    var slide: Float by GlUniformDelegate("uSlide")
    var end: Float by GlUniformDelegate("uEnd")
    var extent: Float by GlUniformDelegate("uExtent")
    var vertical: Float by GlUniformDelegate("uVertical")
    var flipX: Float by GlUniformDelegate("uFlipX")
    var flipY: Float by GlUniformDelegate("uFlipY")

    init {
        alpha = 0f
        slide = SLIDE_MASK_MATERIAL_SLIDE
        end = SLIDE_MASK_MATERIAL_END
        extent = SLIDE_MASK_MATERIAL_WIDTH
        vertical = 0f
        flipX = 0f
        flipY = 0f
    }

    fun setVertical(isVertical: Boolean) {
        vertical = if (isVertical) 1f else 0f
        extent = if (isVertical) SLIDE_MASK_HEIGHT else SLIDE_MASK_MATERIAL_WIDTH
    }

    fun setFlip(flipHorizontal: Boolean, flipVertical: Boolean) {
        flipX = if (flipHorizontal) 1f else 0f
        flipY = if (flipVertical) 1f else 0f
    }

    override fun loadFragmentShader(): String = SLIDE_MASK_FRAGMENT_SHADER

    companion object {
        const val SLIDE_MASK_MATERIAL_END: Float = 0.641f
        const val SLIDE_MASK_MATERIAL_SLIDE: Float = 0.601f
        const val SLIDE_MASK_MATERIAL_WIDTH: Float = 0.787f
        const val SLIDE_MASK_HEIGHT: Float = 1.0f
    }
}

@Language("GLSL")
private const val SLIDE_MASK_FRAGMENT_SHADER = """
precision mediump float;
varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2;

uniform sampler2D uTexture;
uniform sampler2D uMaskTexture;

uniform float uAlpha;
uniform float uSlide;
uniform float uEnd;
uniform float uExtent;
uniform float uVertical;
uniform float uFlipX;
uniform float uFlipY;

void main() {
    vec2 uv = vec2(
        mix(textureCoordinate.x, 1.0 - textureCoordinate.x, uFlipX),
        mix(1.0 - textureCoordinate.y, textureCoordinate.y, uFlipY)
    );
    float progress = clamp((uAlpha - 0.2) * 1.25, 0.0, 1.0);
    float start = progress * (uSlide + uEnd) - uEnd;
    float span = uEnd + progress * (uExtent - uEnd);
    float coord = mix(uv.x, uv.y, uVertical);
    float other = mix(uv.y, uv.x, uVertical);
    float maskU = (coord - start) / span;
    float maskA = texture2D(uMaskTexture, vec2(maskU, other)).a;
    float alphaVal = clamp(maskA + uAlpha, 0.0, 1.0);
    vec4 base = texture2D(uTexture, textureCoordinate);
    gl_FragColor = vec4(base.rgb * alphaVal, alphaVal);
}
"""