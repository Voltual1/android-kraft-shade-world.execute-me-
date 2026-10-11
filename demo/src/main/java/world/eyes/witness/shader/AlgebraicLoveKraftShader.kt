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

// 心形符号距离场 (Heart Signed Distance Function)
float sdHeart(vec2 p) {
    p.x = abs(p.x);
    if (p.y + p.x > 1.0) {
        return sqrt(dot(p - vec2(0.25, 0.75), p - vec2(0.25, 0.75))) - sqrt(2.0) / 4.0;
    }
    return sqrt(max(dot(p - vec2(0.0, 1.0), p - vec2(0.0, 1.0)),
                    dot(p - 0.5 * max(p.x + p.y, 0.0), p - 0.5 * max(p.x + p.y, 0.0)))) * sign(p.x - p.y);
}

// 经典代数心形方程隐式曲线: (x^2 + y^2 - 1)^3 - x^2 * y^3 = 0
float heartEquation(vec2 p) {
    p *= 1.35;
    p.y -= 0.15;
    float a = p.x * p.x + p.y * p.y - 1.0;
    return a * a * a - p.x * p.x * p.y * p.y * p.y;
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.015, 0.01, 0.02);

    // 心跳微弱脉动
    float beatScale = 1.0 + sin(uTime * 5.0) * 0.04 + uBeatPulse * 0.12;
    vec2 p = uv / beatScale;

    // 1. 代数心形方程曲线与内部柔光
    float f = heartEquation(p);
    float heartCurve = drawLine(f, 0.08);

    // 内部渐变充盈光
    float inside = smoothstep(0.0, -0.25, f);

    vec3 loveColor = mix(vec3(1.0, 0.2, 0.65), vec3(1.0, 0.85, 0.95), uBeatPulse);
    finalColor += loveColor * heartCurve * (1.5 + uBeatPulse * 1.5);
    finalColor += vec3(0.35, 0.05, 0.2) * inside * (0.8 + uBeatPulse * 0.6);

    // 2. 阶段性几何外延
    if (uSubPhase == 0) {
        // [阶段 0]: 学习爱 (笛卡尔坐标系标尺与发光网格)
        vec2 grid = abs(fract(uv * 4.0 - 0.5) - 0.5);
        float gridLine = smoothstep(0.04, 0.0, min(grid.x, grid.y));
        finalColor += vec3(0.0, 0.8, 1.0) * gridLine * 0.3;

    } else if (uSubPhase == 1) {
        // [阶段 1]: 答卷与应试 (极坐标同心环与雷达射线)
        float r = length(uv);
        float ring1 = drawLine(r - 0.45, 0.006);
        float ring2 = drawLine(r - 0.65, 0.006);
        float ring3 = drawLine(r - 0.85, 0.006);
        finalColor += vec3(1.0, 0.9, 0.2) * (ring1 + ring2 + ring3) * 0.8;

    } else if (uSubPhase == 2) {
        // [阶段 2]: 代数表达式 (切线微积分与向量箭头阵列)
        float angle = atan(uv.y, uv.x) + uTime * 2.0;
        float rays = smoothstep(0.3, 0.9, sin(angle * 12.0));
        finalColor += vec3(0.8, 0.3, 1.0) * rays * exp(-length(uv) * 2.0) * 0.7;

    } else {
        // [阶段 3]: 困于爱中 (心形激光牢笼锁链，束缚核心)
        float cageRing = drawLine(length(uv) - 0.68, 0.015);
        float crossBars = drawLine(fract(uv.x * 8.0) - 0.5, 0.04) + drawLine(fract(uv.y * 8.0) - 0.5, 0.04);
        finalColor += vec3(1.0, 0.1, 0.4) * cageRing * 1.8;
        finalColor += vec3(0.4, 0.05, 0.2) * crossBars * inside * 0.6;
    }

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""