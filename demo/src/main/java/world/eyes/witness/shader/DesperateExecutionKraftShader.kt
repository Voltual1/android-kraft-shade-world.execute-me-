package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class DesperateExecutionKraftShader(
    time: Float = 0f,
    beatPulse: Float = 0f,
    progress: Float = 0f
) : KraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var beatPulse: Float by GlUniformDelegate("uBeatPulse", required = false)
    var progress: Float by GlUniformDelegate("uProgress", required = false)

    init {
        this.time = time
        this.beatPulse = beatPulse
        this.progress = progress
    }

    override fun loadFragmentShader(): String = DESPERATE_EXECUTION_FRAGMENT_SHADER
}

@Language("GLSL")
private const val DESPERATE_EXECUTION_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform float uBeatPulse;
uniform float uProgress;

float drawLine(float d, float thickness) {
    return smoothstep(thickness, 0.0, abs(d));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.015, 0.01, 0.015);

    // 1. 虚空碎裂网格背景 (Void Fracture Lattice)
    vec2 gridPos = uv * 6.0;
    vec2 gridDist = abs(fract(gridPos - 0.5) - 0.5);
    float lattice = smoothstep(0.05, 0.0, min(gridDist.x, gridDist.y));
    finalColor += vec3(0.12, 0.02, 0.05) * lattice;

    // 2. 纵向多重处决光柱 (Vertical Guillotine Pillars)
    float pillarCount = 5.0;
    float col = floor(uv.x * pillarCount + 2.5);
    float colFrac = fract(uv.x * pillarCount + 2.5) - 0.5;
    float pillarPulse = sin(col * 2.0 + uTime * 12.0) * 0.5 + 0.5;
    float pillarLine = smoothstep(0.12, 0.0, abs(colFrac));

    vec3 pillarColor = mix(vec3(0.8, 0.05, 0.1), vec3(1.0, 0.2, 0.4), pillarPulse);
    finalColor += pillarColor * pillarLine * (0.6 + uBeatPulse * 0.8);

    // 3. 尝试逃逸粒子的震荡轨迹 (Failed Escape Trajectory)
    // 快速挣扎打转但逃不出的粒子轨迹
    float escapeAngle = uTime * 8.0;
    float escapeRadius = 0.38 + sin(uTime * 15.0) * 0.08;
    vec2 escapePt = vec2(cos(escapeAngle), sin(escapeAngle)) * escapeRadius;
    float ptDist = length(uv - escapePt);
    float escapeGlow = exp(-ptDist * 8.0);

    // 逃逸粒子青白微光与碰撞红闪
    finalColor += vec3(0.2, 0.9, 1.0) * escapeGlow * 1.5;

    // 4. 收拢绝杀死刑警戒圆环 (Constricting Kill-Zone)
    float r = length(uv);
    float boundary = 0.55 - uProgress * 0.15; // 逐步收窄牢笼
    float boundaryLine = drawLine(r - boundary, 0.01);
    finalColor += vec3(1.0, 0.1, 0.2) * boundaryLine * (1.3 + uBeatPulse * 1.2);

    // 击发闪屏
    if (uBeatPulse > 0.75) {
        finalColor += vec3(0.35, 0.05, 0.05);
    }

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.3), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""