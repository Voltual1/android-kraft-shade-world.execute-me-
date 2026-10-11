package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class CurrentConversionKraftShader(
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

    override fun loadFragmentShader(): String = CURRENT_CONVERSION_FRAGMENT_SHADER
}

@Language("GLSL")
private const val CURRENT_CONVERSION_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uSubPhase;
uniform float uBeatPulse;

// 绘制抗锯齿平滑线
float drawGlowLine(float dist, float width, float glow) {
    return (smoothstep(width, 0.0, abs(dist)) + exp(-abs(dist) * glow) * 0.4);
}

// 伪随机噪声
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.01, 0.015, 0.025);

    if (uSubPhase == 0) {
        // [阶段 0]: AC / DC 电流切换与高压电弧
        float isDC = step(0.5, fract(uTime * 1.5)); // 交替切换
        float waveY = 0.0;

        if (isDC > 0.5) {
            // DC: 直流平直脉冲方波
            waveY = sign(sin(uv.x * 6.0)) * 0.25;
            finalColor += vec3(0.0, 0.9, 1.0) * drawGlowLine(uv.y - waveY, 0.008, 25.0);
        } else {
            // AC: 交流正弦波伴随高频噪声抖动
            float noise = (hash(vec2(uv.x * 20.0, uTime * 30.0)) - 0.5) * 0.08;
            waveY = sin(uv.x * 8.0 - uTime * 20.0) * 0.35 + noise;
            finalColor += vec3(1.0, 0.85, 0.1) * drawGlowLine(uv.y - waveY, 0.008, 20.0);
        }

        // 随机电压击穿电弧
        if (hash(vec2(uTime * 15.0, uv.y)) > 0.82) {
            finalColor += vec3(0.8, 0.9, 1.0) * 0.45;
        }

    } else if (uSubPhase == 1) {
        // [阶段 1]: DIZZY 眩晕漩涡与视界失明色差
        float dist = length(uv);
        float angle = atan(uv.y, uv.x);

        // 漩涡畸变旋转
        float twist = (1.0 - smoothstep(0.0, 1.2, dist)) * 4.5 * sin(uTime * 3.0);
        angle += twist;

        // 旋转回直角坐标
        vec2 swirlUv = vec2(cos(angle), sin(angle)) * dist;

        // 动态放射环与同心波
        float rings = sin(dist * 25.0 - uTime * 12.0);
        float ringGlow = smoothstep(0.3, 1.0, rings);

        // RGB 分离色差
        vec3 dizzyColor = vec3(
            sin(dist * 22.0 - uTime * 10.0),
            cos(dist * 24.0 - uTime * 10.0),
            sin(dist * 26.0 + uTime * 10.0)
        ) * 0.5 + 0.5;

        finalColor += dizzyColor * ringGlow * (0.8 + uBeatPulse * 0.6);
        // 失明中心黑洞效应
        finalColor *= smoothstep(0.05, 0.35, dist);

    } else if (uSubPhase == 2) {
        // [阶段 2]: A.D. / B.C. 时间穿梭虫洞隧道
        float r = length(uv);
        float theta = atan(uv.y, uv.x);

        // 极坐标条纹向中心吸入
        float speed = uTime * 6.0;
        float tunnelDepth = 1.0 / (r + 0.08);
        float streaks = sin(theta * 16.0 + speed * 2.0) * sin(tunnelDepth * 8.0 - speed * 5.0);
        streaks = smoothstep(0.2, 0.9, streaks);

        vec3 warpColor = mix(vec3(0.2, 0.4, 1.0), vec3(0.9, 0.1, 0.8), sin(tunnelDepth * 2.0) * 0.5 + 0.5);
        finalColor += warpColor * streaks * (0.7 + uBeatPulse * 0.5);

        // 中心时空奇点辉光
        finalColor += vec3(1.0, 0.9, 0.8) * exp(-r * 6.0) * 1.5;

    } else {
        // [阶段 3]: UNITE 双核星轨螺旋融合
        float rot = uTime * 4.0;
        float orbitR = 0.45 * (1.0 - smoothstep(0.0, 4.0, mod(uTime, 4.2))); // 半径逐步缩向核心

        vec2 coreMe = vec2(cos(rot), sin(rot)) * orbitR;
        vec2 coreYou = vec2(cos(rot + 3.14159), sin(rot + 3.14159)) * orbitR;

        float distMe = length(uv - coreMe);
        float distYou = length(uv - coreYou);

        // me (青色)
        float glowMe = exp(-distMe * 7.0);
        finalColor += vec3(0.0, 0.9, 1.0) * glowMe * 1.2;

        // you (粉紫色)
        float glowYou = exp(-distYou * 7.0);
        finalColor += vec3(1.0, 0.2, 0.6) * glowYou * 1.2;

        // 融合核心中点能量环
        float centerDist = length(uv);
        float bridge = exp(-centerDist * 4.0) * (uBeatPulse * 0.8 + 0.3);
        finalColor += vec3(0.9, 0.8, 1.0) * bridge;
    }

    // 胶片边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""