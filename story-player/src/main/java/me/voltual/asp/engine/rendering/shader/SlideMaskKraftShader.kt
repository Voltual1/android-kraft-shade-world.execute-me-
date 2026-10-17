package me.voltual.asp.engine.rendering.shader

import com.cardinalblue.kraftshade.model.GlColor
import com.cardinalblue.kraftshade.shader.TwoTextureInputKraftShader
import com.cardinalblue.kraftshade.shader.buffer.Texture
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

/**
 * Native port: AVG/[UC]Common/Arts/Materials/slide_mask
 * Shader: Torappu/UI/AVG/SlideMask
 *
 * uTexture: Base color / Background scene or optional blocker texture
 * uMaskTexture: slide_mask.png (slide_left mask texture)
 */
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
    var color: GlColor by GlUniformDelegate("uColor", required = false)
    var useTexture: Boolean by GlUniformDelegate("uUseTexture", required = false)

    init {
        alpha = 0f
        slide = SLIDE_MASK_MATERIAL_SLIDE
        end = SLIDE_MASK_MATERIAL_END
        extent = SLIDE_MASK_MATERIAL_WIDTH
        vertical = 0f
        flipX = 0f
        flipY = 0f
        color = GlColor.Black
        useTexture = false
        setInputTexture(Texture.Empty)
    }

    fun setVertical(isVertical: Boolean) {
        vertical = if (isVertical) 1f else 0f
        extent = if (isVertical) SLIDE_MASK_HEIGHT else SLIDE_MASK_MATERIAL_WIDTH
    }

    fun setFlip(flipHorizontal: Boolean, flipVertical: Boolean) {
        flipX = if (flipHorizontal) 1f else 0f
        flipY = if (flipVertical) 1f else 0f
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float = 1f) {
        color = GlColor(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
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

uniform vec4 uColor;
uniform int uUseTexture;

uniform float uAlpha;
uniform float uSlide;
uniform float uEnd;
uniform float uExtent;
uniform float uVertical;
uniform float uFlipX;
uniform float uFlipY;

void main() {
    // Coordinate mapping with flip consideration
    vec2 uv = vec2(
        mix(textureCoordinate.x, 1.0 - textureCoordinate.x, uFlipX),
        mix(1.0 - textureCoordinate.y, textureCoordinate.y, uFlipY)
    );
    float progress = clamp((uAlpha - 0.2) * 1.25, 0.0, 1.0);
    float start = progress * (uSlide + uEnd) - uEnd;
    float span = uEnd + progress * (uExtent - uEnd);
    float coord = mix(uv.x, uv.y, uVertical);
    float other = mix(uv.y, uv.x, uVertical);
    float maskU = (coord - start) / max(span, 0.0001);
    float maskA = texture2D(uMaskTexture, vec2(clamp(maskU, 0.0, 1.0), other)).a;
    float alphaVal = clamp(maskA + uAlpha, 0.0, 1.0);

    vec4 base = uColor;
    if (uUseTexture != 0) {
        base *= texture2D(uTexture, textureCoordinate);
    }
    gl_FragColor = vec4(base.rgb * alphaVal, base.a * alphaVal);
}
"""