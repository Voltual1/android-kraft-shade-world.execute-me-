package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class RapidExecutionKraftShader(
    time: Float = 0f,
    strikeIndex: Int = 0,
    hitProgress: Float = 0f,
    beatPulse: Float = 0f
) : KraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var strikeIndex: Int by GlUniformDelegate("uStrikeIndex", required = false)
    var hitProgress: Float by GlUniformDelegate("uHitProgress", required = false)
    var beatPulse: Float by GlUniformDelegate("uBeatPulse", required = false)

    init {
        this.time = time
        this.strikeIndex = strikeIndex
        this.hitProgress = hitProgress
        this.beatPulse = beatPulse
    }

    override fun loadFragmentShader(): String = RAPID_EXECUTION_FRAGMENT_SHADER
}

@Language("GLSL")
private const val RAPID_EXECUTION_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uStrikeIndex;
uniform float uHitProgress;
uniform float uBeatPulse;

float drawLine(float d, float thickness) {
    return smoothstep(thickness, 0.0, abs(d));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    // 每次击发瞬间的剧烈屏幕震颤与色彩分离
    float shakeX = (hash(vec2(uTime * 40.0, 0.0)) - 0.5) * 0.08 * uHitProgress;
    float shakeY = (hash(vec2(0.0, uTime * 40.0)) - 0.5) * 0.08 * uHitProgress;
    uv += vec2(shakeX, shakeY);

    vec3 finalColor = vec3(0.02, 0.01, 0.01);

    // 1. 全屏红色警告斜切条纹
    float stripe = sin((uv.x + uv.y) * 22.0 + uTime * 15.0);
    float hazard = step(0.0, stripe);
    finalColor += mix(vec3(0.05, 0.0, 0.0), vec3(0.45, 0.02, 0.02), hazard) * 0.7;

    // 2. 处决收缩锁定环 (瞄准准星逐级缩小)
    float baseRadius = 0.65 - float(uStrikeIndex) * 0.025;
    baseRadius = max(baseRadius, 0.25);
    float targetRing = drawLine(length(uv) - baseRadius, 0.012);

    // 重型十字线与刻度
    float crosshair = drawLine(uv.x, 0.005) * step(abs(uv.y), baseRadius + 0.15);
    crosshair += drawLine(uv.y, 0.005) * step(abs(uv.x), baseRadius + 0.15);

    // 四角对角线标线
    float diag = drawLine(abs(uv.x) - abs(uv.y), 0.004) * step(length(uv), baseRadius + 0.08);

    vec3 reticleColor = vec3(1.0, 0.1, 0.15);
    finalColor += reticleColor * (targetRing + crosshair + diag * 0.5) * (1.5 + uHitProgress * 2.0);

    // 3. 击发核心白热耀斑
    float coreGlow = exp(-length(uv) * (3.5 - uHitProgress * 1.5)) * uHitProgress;
    finalColor += vec3(1.0, 0.95, 0.8) * coreGlow * 2.2;

    // 4. 击发瞬间全屏反相白热强闪
    if (uHitProgress > 0.65) {
        finalColor = vec3(1.0) - finalColor;
    }

    // 边缘剧烈暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.35), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""