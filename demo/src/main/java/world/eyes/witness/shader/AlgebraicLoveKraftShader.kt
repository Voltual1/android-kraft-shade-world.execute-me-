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

// 精确正立代数心形距离场 (Upright Heart SDF)
// 在 OpenGL 纹理坐标系下确保双瓣在屏幕上方，心尖在屏幕下方
float sdHeartUpright(vec2 p) {
    p.x = abs(p.x);
    // 旋转变换至标准正立姿态
    if (p.y + p.x > 0.65) {
        return length(p - vec2(0.25, 0.40)) - 0.35;
    }
    return length(p - 0.5 * max(p.x + p.y, 0.0)) * sign(p.x - p.y);
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    // 核心修正：必须取反使得屏幕上方为正 Y，从而让心形双瓣在上方，心尖在下方
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.015, 0.01, 0.02);

    // 随 130 BPM 音频重拍真实跳动
    float beatScale = 1.0 + sin(uTime * 6.0) * 0.04 + uBeatPulse * 0.16;
    vec2 p = uv / beatScale;

    // 垂直中心对准屏幕中轴偏下位置
    p.y -= 0.12;

    // 1. 饱满正立心形轮廓与内部脉动充盈光
    float hDist = sdHeartUpright(p * 1.35);
    float heartOutline = drawLine(hDist, 0.018);
    float heartInside = smoothstep(0.0, -0.45, hDist);

    // 霓虹桃红与暖白高光
    vec3 heartColor = mix(vec3(1.0, 0.15, 0.6), vec3(1.0, 0.85, 0.95), uBeatPulse);
    finalColor += heartColor * heartOutline * (2.0 + uBeatPulse * 1.5);
    finalColor += vec3(0.5, 0.05, 0.25) * heartInside * (0.8 + uBeatPulse * 0.9);

    // 2. 心跳向外扩散的同心声波环
    float waveDist = fract(hDist * 3.2 - uTime * 2.2);
    float heartWave = smoothstep(0.12, 0.0, abs(waveDist - 0.5)) * step(0.0, hDist);
    finalColor += vec3(0.9, 0.25, 0.65) * heartWave * 0.5;

    // 3. 极坐标放射光芒
    float angle = atan(uv.y, uv.x);
    float rays = smoothstep(0.25, 0.9, sin(angle * 16.0 + uTime * 1.8));
    finalColor += vec3(0.6, 0.1, 0.7) * rays * exp(-length(uv) * 1.6) * 0.45;

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * max(vig, 0.0), 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""