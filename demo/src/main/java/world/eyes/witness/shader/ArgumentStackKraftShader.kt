package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class ArgumentStackKraftShader(
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

    override fun loadFragmentShader(): String = ARGUMENT_STACK_FRAGMENT_SHADER
}

@Language("GLSL")
private const val ARGUMENT_STACK_FRAGMENT_SHADER = """
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

    vec3 finalColor = vec3(0.015, 0.012, 0.02);

    // 1. 十六进制数据雨/内存转储下落流 (Hex Dump Waterfall)
    vec2 grid = vec2(floor(uv.x * 24.0), floor((uv.y - uTime * 6.0) * 16.0));
    float charRand = hash(grid);
    float streamIntensity = smoothstep(0.4, 0.95, charRand);

    // 渐进加速的流速与亮度
    vec3 streamColor = mix(vec3(0.1, 0.8, 0.4), vec3(1.0, 0.15, 0.2), uProgress);
    finalColor += streamColor * streamIntensity * 0.45;

    // 2. 栈帧物理层叠线框 (Stack Frame Visualizer)
    float stackY = fract(uv.y * 3.5 + uTime * 1.5);
    float stackLine = drawLine(stackY - 0.5, 0.02);

    // 栈溢出边缘警戒线 (左右红色阻隔栏)
    float leftBorder = drawLine(uv.x + 0.85, 0.008);
    float rightBorder = drawLine(uv.x - 0.85, 0.008);
    float borders = leftBorder + rightBorder;

    vec3 alertColor = vec3(1.0, 0.1, 0.2);
    finalColor += alertColor * borders * (1.2 + uBeatPulse * 0.8);
    finalColor += vec3(0.8, 0.4, 0.1) * stackLine * 0.35;

    // 3. 栈溢出压力核心仪表 (Central Stack Pressure Ring)
    float r = length(uv);
    float pressureRing = drawLine(r - (0.35 + uProgress * 0.2), 0.01);
    finalColor += alertColor * pressureRing * (1.5 + uBeatPulse * 1.2);

    // 伴随节奏的剧烈频闪故障
    if (uBeatPulse > 0.7) {
        finalColor += vec3(0.3, 0.05, 0.05);
    }

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.3), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""