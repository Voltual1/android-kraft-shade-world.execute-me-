package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class BiologyExistenceKraftShader(
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

    override fun loadFragmentShader(): String = BIOLOGY_EXISTENCE_FRAGMENT_SHADER
}

@Language("GLSL")
private const val BIOLOGY_EXISTENCE_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uSubPhase;
uniform float uBeatPulse;

float drawLine(float d, float thickness) {
    return smoothstep(thickness, 0.0, abs(d));
}

// 六边形距离场 (用于化学苯环抗氧化分子式)
float sdHexagon(vec2 p, float r) {
    const vec3 k = vec3(-0.866025404, 0.5, 0.577350269);
    p = abs(p);
    p -= 2.0 * min(dot(k.xy, p), 0.0) * k.xy;
    p -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
    return length(p) * sign(p.y);
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.012, 0.015, 0.02);

    if (uSubPhase == 0) {
        // [阶段 0]: EGGPLANT & NUTRIENTS (有机紫罗兰与细胞营养素分子晶格)
        vec2 p = uv;
        float r = length(p);

        // 旋转营养素微粒群
        float angle = atan(p.y, p.x) + uTime * 1.5;
        vec2 rotP = vec2(cos(angle), sin(angle)) * r;

        // 细胞核晶格
        vec2 cellGrid = abs(fract(rotP * 4.0 - 0.5) - 0.5);
        float cells = smoothstep(0.08, 0.0, min(cellGrid.x, cellGrid.y));

        // 营养脉动同心环
        float ring = fract(r * 3.5 - uTime * 2.0);
        float ringLine = smoothstep(0.06, 0.0, abs(ring - 0.5));

        vec3 nutrientPurple = vec3(0.65, 0.1, 0.9);
        vec3 nutrientGreen = vec3(0.1, 0.9, 0.4);

        finalColor += nutrientPurple * ringLine * (1.2 + uBeatPulse * 0.6);
        finalColor += nutrientGreen * cells * 0.4;
        finalColor += vec3(0.4, 0.1, 0.6) * exp(-r * 3.5);

    } else if (uSubPhase == 1) {
        // [阶段 1]: TOMATO & ANTIOXIDANTS (抗氧化六边形分子链与番茄红)
        float hexDist1 = abs(sdHexagon(uv, 0.35));
        float hexDist2 = abs(sdHexagon(uv - vec2(0.45, 0.25), 0.22));
        float hexDist3 = abs(sdHexagon(uv + vec2(0.45, 0.25), 0.22));

        float hexLines = drawLine(hexDist1, 0.008) + drawLine(hexDist2, 0.006) + drawLine(hexDist3, 0.006);

        // 分子共价键连接线与自由基清除脉冲
        float bondLine = drawLine(uv.y - uv.x * 0.55, 0.004) * step(abs(uv.x), 0.55);

        vec3 tomatoRed = vec3(1.0, 0.15, 0.25);
        vec3 antioxidantGold = vec3(1.0, 0.8, 0.2);

        finalColor += tomatoRed * hexLines * (1.1 + uBeatPulse * 0.7);
        finalColor += antioxidantGold * bondLine * 0.9;
        finalColor += vec3(0.5, 0.05, 0.1) * exp(-length(uv) * 4.0);

    } else if (uSubPhase == 2) {
        // [阶段 2]: TABBY CAT & ENJOYMENT (猫耳几何剪影与呼噜声波形)
        // 呼噜声波正弦脉动: y = A * sin(k*x)
        float purrWave = 0.08 * sin(uv.x * 16.0 - uTime * 14.0) * exp(-abs(uv.x) * 1.5);
        float purrLine = drawLine(uv.y - purrWave + 0.15, 0.007);

        // 极简几何猫耳 (左耳与右耳线框)
        vec2 earP = uv - vec2(0.0, 0.15);
        float leftEar = drawLine(abs(earP.x + 0.25) + earP.y * 0.8 - 0.25, 0.007) * step(earP.y, 0.3) * step(-0.05, earP.y);
        float rightEar = drawLine(abs(earP.x - 0.25) + earP.y * 0.8 - 0.25, 0.007) * step(earP.y, 0.3) * step(-0.05, earP.y);
        float headBase = drawLine(earP.y + 0.05, 0.007) * step(abs(earP.x), 0.35);

        // 温暖的琥珀金与桃橘暖光
        vec3 purrGold = vec3(1.0, 0.75, 0.2);
        vec3 amberGlow = vec3(1.0, 0.5, 0.15);

        finalColor += purrGold * purrLine * (1.2 + uBeatPulse * 0.8);
        finalColor += amberGlow * (leftEar + rightEar + headBase) * 0.85;
        finalColor += vec3(0.3, 0.15, 0.05) * exp(-length(uv) * 3.0);

    } else {
        // [阶段 3]: GOD & EXISTENCE (唯一神明圣光放射与存在证明法阵)
        float r = length(uv);
        float angle = atan(uv.y, uv.x);

        // 圣光万丈射线 (8 条主光芒 + 16 条细光条)
        float rays = sin(angle * 16.0 + uTime * 2.0);
        rays = smoothstep(0.2, 0.9, rays);

        // 存在之证明同心法阵
        float ring1 = drawLine(r - 0.38, 0.008);
        float ring2 = drawLine(r - 0.58, 0.005);
        float ring3 = drawLine(r - 0.72, 0.004);

        // 旋转内接正八边形/正方形十字
        float crossRot = angle + uTime * 0.8;
        float divineStar = smoothstep(0.08, 0.0, abs(sin(crossRot * 4.0)) * r - 0.08);

        vec3 divineGold = vec3(1.0, 0.9, 0.5);
        vec3 pureWhite = vec3(1.0, 1.0, 1.0);

        finalColor += divineGold * (ring1 + ring2 + ring3) * (1.3 + uBeatPulse * 0.7);
        finalColor += divineGold * rays * exp(-r * 1.5) * 0.75;
        finalColor += pureWhite * divineStar;
        // 核心奇点辉光
        finalColor += vec3(1.0, 0.95, 0.8) * exp(-r * 4.5) * 2.0;
    }

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""