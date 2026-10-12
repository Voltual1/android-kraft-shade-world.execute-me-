package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class AlgebraicLoveKraftShader(
    time: Float = 0f,
    subPhase: Int = 0,
    beatPulse: Float = 0f
) : KraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var subPhase: Int by GlUniformDelegate("uSubPhase", required = false)
    var beatPulse: Float by GlUniformDelegate("uBeatPulse", required = false)

    init {
        this.time = time
        this.subPhase = subPhase
        this.beatPulse = beatPulse
    }

    override fun loadFragmentShader(): String = ALGEBRAIC_LOVE_FRAGMENT_SHADER
}

@Language("GLSL")
private const val ALGEBRAIC_LOVE_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uSubPhase;
uniform float uBeatPulse;

float drawLine(float d, float thickness) {
    return smoothstep(thickness, 0.0, abs(d));
}

// 稳定且优雅的正立心形隐式方程与 SDF
// 基于标准心形曲线: (x^2 + y^2 - 1)^3 - x^2 * y^3 = 0 的正向归一化
float heartShape(vec2 p) {
    p.y += 0.15; // 稍微向下微调让中心居中
    float x = p.x;
    float y = p.y;
    float a = x * x + y * y - 0.45;
    return a * a * a - x * x * y * y * y;
}

// Inigo Quilez 精确心形距离场 (Upright Heart SDF)
float sdHeart(vec2 p) {
    p.x = abs(p.x);
    p.y += 0.25;
    if (p.y + p.x > 0.8) {
        return length(p - vec2(0.25, 0.55)) - 0.35;
    }
    return length(p - 0.5 * max(p.x + p.y, 0.0)) * sign(p.x - p.y);
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    // 注意：不再反转 uv.y，保证正立心形朝向（双瓣在上，心尖在下）
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.015, 0.01, 0.02);

    // 心跳微弱脉动与冲击缩放
    float heartBeat = 1.0 + sin(uTime * 6.0) * 0.03 + uBeatPulse * 0.15;
    vec2 p = uv / heartBeat;

    // 1. 精确正立代数心形曲线
    float hSdf = sdHeart(p * 1.3);
    float heartOutline = drawLine(hSdf, 0.018);
    float heartFill = smoothstep(0.0, -0.4, hSdf);

    // 霓虹粉与红紫渐变
    vec3 heartColor = mix(vec3(1.0, 0.2, 0.6), vec3(1.0, 0.85, 0.95), uBeatPulse);
    finalColor += heartColor * heartOutline * (1.8 + uBeatPulse * 1.5);
    finalColor += vec3(0.45, 0.05, 0.25) * heartFill * (0.8 + uBeatPulse * 0.8);

    // 2. 外部扩散的多重同心心形声波
    float waveDist = fract(hSdf * 3.0 - uTime * 2.0);
    float heartWave = smoothstep(0.1, 0.0, abs(waveDist - 0.5)) * step(0.0, hSdf);
    finalColor += vec3(0.9, 0.3, 0.7) * heartWave * 0.45;

    // 3. 背景代数坐标标尺与光线辐射
    float angle = atan(uv.y, uv.x);
    float rays = smoothstep(0.2, 0.9, sin(angle * 16.0 + uTime * 2.0));
    finalColor += vec3(0.5, 0.1, 0.7) * rays * exp(-length(uv) * 1.8) * 0.4;

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * max(vig, 0.0), 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""