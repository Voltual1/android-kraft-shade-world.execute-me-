package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class GeometryMathKraftShader(
    time: Float = 0f,
    mathPhase: Int = 0,
    beatPulse: Float = 0f
) : KraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var mathPhase: Int by GlUniformDelegate("uMathPhase", required = false)
    var beatPulse: Float by GlUniformDelegate("uBeatPulse", required = false)

    init {
        this.time = time
        this.mathPhase = mathPhase
        this.beatPulse = beatPulse
    }

    override fun loadFragmentShader(): String = GEOMETRY_MATH_FRAGMENT_SHADER
}

@Language("GLSL")
private const val GEOMETRY_MATH_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uMathPhase;
uniform float uBeatPulse;

// 抗锯齿线段绘制辅助
float drawLine(float d, float thickness) {
    return smoothstep(thickness, 0.0, abs(d));
}

void main() {
    // 居中归一化坐标 (-1.0 ~ 1.0)
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 color = vec3(0.015, 0.02, 0.035);

    // 背景微弱坐标网格
    vec2 gridCoord = abs(fract(uv * 3.0 - 0.5) - 0.5);
    float bgGrid = smoothstep(0.04, 0.0, min(gridCoord.x, gridCoord.y));
    color += vec3(0.05, 0.1, 0.15) * bgGrid * 0.4;

    // 坐标轴
    float axisX = drawLine(uv.y, 0.003);
    float axisY = drawLine(uv.x, 0.003);
    color += vec3(0.15, 0.25, 0.35) * (axisX + axisY);

    if (uMathPhase == 0) {
        // [阶段 0]: 点集与维度 (DIMENSION)
        // 动态凝聚的多维点阵与拓扑连线
        vec2 p = uv;
        float pointField = 0.0;
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                vec2 center = vec2(float(i) * 0.28, float(j) * 0.28);
                // 呼吸摆动
                center += vec2(sin(uTime * 2.0 + float(i)) * 0.04, cos(uTime * 2.0 + float(j)) * 0.04);
                float dist = length(p - center);
                pointField += smoothstep(0.018, 0.0, dist);
            }
        }
        color += vec3(0.0, 0.85, 1.0) * pointField * (1.0 + uBeatPulse * 0.5);

    } else if (uMathPhase == 1) {
        // [阶段 1]: 圆与圆周 (CIRCUMFERENCE)
        float r = 0.55 + sin(uTime * 2.5) * 0.04;
        float d = length(uv) - r;
        float circleLine = drawLine(d, 0.008);

        // 旋转扫描半径线
        float angle = uTime * 2.5;
        vec2 dir = vec2(cos(angle), sin(angle));
        float radiusLine = 0.0;
        float proj = dot(uv, dir);
        if (proj >= 0.0 && proj <= r) {
            float perp = length(uv - dir * proj);
            radiusLine = drawLine(perp, 0.005);
        }

        color += vec3(1.0, 0.3, 0.6) * circleLine * (1.0 + uBeatPulse * 0.6);
        color += vec3(1.0, 0.8, 0.9) * radiusLine;

    } else if (uMathPhase == 2) {
        // [阶段 2]: 正弦波与切线 (TANGENTS)
        // 正弦波: y = A * sin(k*x - omega*t)
        float waveY = 0.35 * sin(uv.x * 4.0 - uTime * 3.5);
        float waveLine = drawLine(uv.y - waveY, 0.009);
        color += vec3(0.2, 1.0, 0.5) * waveLine * (1.0 + uBeatPulse * 0.5);

        // 沿波形移动的点及其切线
        float ptX = sin(uTime * 1.5) * 0.9;
        float ptY = 0.35 * sin(ptX * 4.0 - uTime * 3.5);
        // 导数斜率: dy/dx = 0.35 * 4.0 * cos(...)
        float slope = 1.4 * cos(ptX * 4.0 - uTime * 3.5);

        // 切线方程: (y - ptY) - slope * (x - ptX) = 0
        float tangentDist = abs((uv.y - ptY) - slope * (uv.x - ptX)) / sqrt(1.0 + slope * slope);
        float tangentLine = drawLine(tangentDist, 0.005) * step(abs(uv.x - ptX), 0.55);

        color += vec3(1.0, 0.9, 0.2) * tangentLine;
        // 切点高亮
        color += vec3(1.0, 1.0, 1.0) * smoothstep(0.025, 0.0, length(uv - vec2(ptX, ptY)));

    } else {
        // [阶段 3]: 趋于无穷与极限 (LIMITATIONS)
        // 双曲渐近曲线与收敛夹逼边框
        float bound = 0.55;
        float boxD = max(abs(uv.x), abs(uv.y)) - bound;
        float limitBox = drawLine(boxD, 0.006);

        // 无穷逼近脉冲曲线
        float limitCurve = drawLine(uv.y - 0.45 * tanh(uv.x * (3.0 + uTime * 1.5)), 0.008);

        color += vec3(1.0, 0.4, 0.2) * limitBox * (1.0 + uBeatPulse * 0.8);
        color += vec3(0.4, 0.8, 1.0) * limitCurve;
    }

    // 经典暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.3), 0.0, 1.0);
    color *= vig;

    gl_FragColor = vec4(color, 1.0);
}
"""