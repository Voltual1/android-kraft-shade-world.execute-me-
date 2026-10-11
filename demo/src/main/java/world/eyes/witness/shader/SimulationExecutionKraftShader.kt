package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class SimulationExecutionKraftShader(
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

    override fun loadFragmentShader(): String = SIMULATION_EXECUTION_FRAGMENT_SHADER
}

@Language("GLSL")
private const val SIMULATION_EXECUTION_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uSubPhase;
uniform float uBeatPulse;

float drawLine(float d, float thickness) {
    return smoothstep(thickness, 0.0, abs(d));
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.012, 0.015, 0.02);

    if (uSubPhase == 0) {
        // [阶段 0]: STIMULATIONS (高频电荷刺激与放射脉冲)
        float r = length(uv);
        float angle = atan(uv.y, uv.x);

        // 神经突触电脉冲射线
        float spikes = sin(angle * 24.0 + uTime * 35.0) * sin(r * 30.0 - uTime * 20.0);
        spikes = smoothstep(0.4, 0.95, spikes);

        // 能量激波环
        float ring = fract(r * 4.0 - uTime * 3.0);
        float ringGlow = smoothstep(0.08, 0.0, abs(ring - 0.5));

        vec3 stimColor = mix(vec3(0.0, 1.0, 0.8), vec3(1.0, 0.9, 0.1), spikes);
        finalColor += stimColor * (spikes + ringGlow * 0.6) * (1.0 + uBeatPulse * 0.7);

    } else if (uSubPhase == 1) {
        // [阶段 1]: SATISFACTION (同心能量光环与 100% 过载充能)
        float dist = length(uv);

        // 仪表盘同心充能弧
        float ring1 = abs(dist - 0.35);
        float ring2 = abs(dist - 0.55);
        float ring3 = abs(dist - 0.75);

        float gaugeLines = drawLine(ring1, 0.008) + drawLine(ring2, 0.006) + drawLine(ring3, 0.005);

        // 旋转扫描光芒
        float scanAngle = atan(uv.y, uv.x) - uTime * 4.0;
        float beam = smoothstep(0.7, 1.0, cos(scanAngle));

        vec3 satColor = vec3(1.0, 0.5, 0.9);
        finalColor += satColor * gaugeLines * (1.2 + uBeatPulse * 0.8);
        finalColor += vec3(1.0, 0.85, 0.2) * beam * smoothstep(0.8, 0.2, dist) * 0.8;

    } else if (uSubPhase == 2) {
        // [阶段 2]: EXECUTION (红黑警示斜纹、处决十字准星与震慑强闪)
        // 45 度警告危险条纹
        float stripe = sin((uv.x + uv.y) * 18.0 + uTime * 6.0);
        float hazard = step(0.0, stripe);

        // 背景暗红交替
        finalColor = mix(vec3(0.05, 0.0, 0.0), vec3(0.4, 0.02, 0.02), hazard);

        // 重型处决锁定准星
        float targetRing = drawLine(length(uv) - 0.45, 0.012);
        float crossX = drawLine(uv.y, 0.005) * step(abs(uv.x), 0.65);
        float crossY = drawLine(uv.x, 0.005) * step(abs(uv.y), 0.65);

        vec3 alertColor = vec3(1.0, 0.1, 0.15);
        finalColor += alertColor * (targetRing + crossX + crossY) * 1.5;

        // 拍点全屏反色冲击闪光
        if (uBeatPulse > 0.65) {
            finalColor = vec3(1.0) - finalColor;
        }

    } else {
        // [阶段 3]: TRAPPED IN SIMULATION (激光囚笼收缩与物理锁定框)
        // 动态向内收紧的囚笼
        float boxSize = 0.58 + sin(uTime * 2.0) * 0.03;
        vec2 boxDist = abs(uv) - vec2(boxSize * aspect * 0.6, boxSize);

        // 牢笼竖栅栏与横栅栏
        float gridX = drawLine(fract(uv.x * 12.0) - 0.5, 0.08);
        float gridY = drawLine(fract(uv.y * 12.0) - 0.5, 0.08);
        float cage = (gridX + gridY) * step(max(boxDist.x, boxDist.y), 0.0);

        // 锁定边框
        float border = drawLine(max(boxDist.x, boxDist.y), 0.01);

        vec3 cageColor = vec3(0.2, 0.6, 1.0);
        finalColor += cageColor * cage * 0.25;
        finalColor += vec3(0.0, 0.9, 1.0) * border * (1.2 + uBeatPulse * 0.5);

        // 外部禁闭区虚化
        if (max(boxDist.x, boxDist.y) > 0.0) {
            finalColor += vec3(0.08, 0.02, 0.02);
        }
    }

    // 暗角与扫描暗纹
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.3), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""