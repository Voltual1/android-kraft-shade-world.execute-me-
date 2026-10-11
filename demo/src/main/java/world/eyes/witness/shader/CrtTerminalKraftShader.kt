package world.eyes.witness.shader

import com.cardinalblue.kraftshade.model.GlVec2
import com.cardinalblue.kraftshade.shader.TextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class CrtTerminalKraftShader(
    time: Float = 0f,
    scanlineIntensity: Float = 0.25f,
    curvature: Float = 0.08f,
    vignetteStrength: Float = 0.35f,
    glitchIntensity: Float = 0f
) : TextureInputKraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var scanlineIntensity: Float by GlUniformDelegate("uScanlineIntensity", required = false)
    var curvature: Float by GlUniformDelegate("uCurvature", required = false)
    var vignetteStrength: Float by GlUniformDelegate("uVignetteStrength", required = false)
    var glitchIntensity: Float by GlUniformDelegate("uGlitchIntensity", required = false)

    init {
        this.time = time
        this.scanlineIntensity = scanlineIntensity
        this.curvature = curvature
        this.vignetteStrength = vignetteStrength
        this.glitchIntensity = glitchIntensity
    }

    override fun loadFragmentShader(): String = CRT_TERMINAL_FRAGMENT_SHADER
}

@Language("GLSL")
private const val CRT_TERMINAL_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform vec2 resolution;

uniform float uTime;
uniform float uScanlineIntensity;
uniform float uCurvature;
uniform float uVignetteStrength;
uniform float uGlitchIntensity;

// 桶形畸变
vec2 curveRemapUV(vec2 uv) {
    uv = uv * 2.0 - 1.0;
    vec2 offset = abs(uv.yx) / vec2(6.0, 4.0);
    uv = uv + uv * offset * offset * uCurvature * 10.0;
    uv = uv * 0.5 + 0.5;
    return uv;
}

void main() {
    vec2 uv = curveRemapUV(textureCoordinate);

    // 屏幕黑边裁切
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    // 模拟电压抖动与色差 (Chromatic Aberration)
    float jitter = sin(uTime * 45.0) * 0.002 * uGlitchIntensity;
    vec2 gOffset = vec2(jitter, 0.0);
    vec2 rOffset = vec2(0.0015 + jitter * 1.5, 0.0);
    vec2 bOffset = vec2(-0.0015 - jitter * 1.5, 0.0);

    float r = texture2D(inputImageTexture, uv + rOffset).r;
    float g = texture2D(inputImageTexture, uv + gOffset).g;
    float b = texture2D(inputImageTexture, uv + bOffset).b;
    vec3 color = vec3(r, g, b);

    // 扫描线
    float count = resolution.y > 0.0 ? resolution.y * 0.5 : 540.0;
    float scanline = sin((uv.y + uTime * 0.02) * count * 3.14159) * 0.5 + 0.5;
    color -= color * scanline * uScanlineIntensity;

    // 暗角
    float vignette = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vignette = clamp(pow(16.0 * vignette, uVignetteStrength), 0.0, 1.0);
    color *= vignette;

    // 荧光微弱发光增强
    color += vec3(0.02, 0.05, 0.03);

    gl_FragColor = vec4(color, 1.0);
}
"""