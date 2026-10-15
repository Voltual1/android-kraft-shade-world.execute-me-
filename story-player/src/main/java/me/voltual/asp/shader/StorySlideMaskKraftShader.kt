package me.voltual.asp.shader

import com.cardinalblue.kraftshade.model.GlVec4
import com.cardinalblue.kraftshade.shader.TwoTextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

/**
 * Native port of Torappu/UI/AVG/SlideMask shader.
 * Controls blocker screen wipes using the slide_mask ramp texture.
 */
class StorySlideMaskKraftShader : TwoTextureInputKraftShader(
    secondTextureSampleName = "inputMaskTexture"
) {
    var uAlpha: Float by GlUniformDelegate("uAlpha")
    var uSlide: Float by GlUniformDelegate("uSlide")
    var uEnd: Float by GlUniformDelegate("uEnd")
    var uExtent: Float by GlUniformDelegate("uExtent")
    var uVertical: Float by GlUniformDelegate("uVertical")
    var uFlipX: Float by GlUniformDelegate("uFlipX")
    var uFlipY: Float by GlUniformDelegate("uFlipY")
    var uBlockerColor: GlVec4 by GlUniformDelegate("uBlockerColor")

    init {
        uAlpha = 0f
        uSlide = 0.601f
        uEnd = 0.641f
        uExtent = 0.787f
        uVertical = 0f
        uFlipX = 0f
        uFlipY = 0f
        uBlockerColor = GlVec4(0f, 0f, 0f, 1f)
    }

    override fun loadFragmentShader(): String = SLIDE_MASK_FRAGMENT_SHADER

    companion object {
        @Language("GLSL")
        private const val SLIDE_MASK_FRAGMENT_SHADER = """
            precision highp float;
            varying highp vec2 textureCoordinate;
            varying highp vec2 textureCoordinate2;

            uniform sampler2D inputImageTexture;
            uniform sampler2D inputMaskTexture;

            uniform highp float uAlpha;
            uniform highp float uSlide;
            uniform highp float uEnd;
            uniform highp float uExtent;
            uniform highp float uVertical;
            uniform highp float uFlipX;
            uniform highp float uFlipY;
            uniform highp vec4 uBlockerColor;

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
                float maskA = texture2D(inputMaskTexture, vec2(maskU, other)).a;
                float alpha = clamp(maskA + uAlpha, 0.0, 1.0);
                gl_FragColor = vec4(uBlockerColor.rgb * alpha, alpha);
            }
        """
    }
}