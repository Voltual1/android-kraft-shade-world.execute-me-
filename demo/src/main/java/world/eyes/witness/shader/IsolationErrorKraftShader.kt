package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class IsolationErrorKraftShader(
    time: Float = 0f,
    subPhase: Int = 0,
    beatPulse: Float = 0f,
    glitchIntensity: Float = 0f
) : KraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var subPhase: Int by GlUniformDelegate("uSubPhase", required = false)
    var beatPulse: Float by GlUniformDelegate("uBeatPulse", required = false)
    var glitchIntensity: Float by GlUniformDelegate("uGlitchIntensity", required = false)

    init {
        this.time = time
        this.subPhase = subPhase
        this.beatPulse = beatPulse
        this.glitchIntensity = glitchIntensity
    }

    override fun loadFragmentShader(): String = ISOLATION_ERROR_FRAGMENT_SHADER
}

@Language("GLSL")
private const val ISOLATION_ERROR_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform int uSubPhase;
uniform float uBeatPulse;
uniform float uGlitchIntensity;

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

    // 故障毛刺位移 (Glitch Jitter)
    if (uGlitchIntensity > 0.0) {
        float noiseY = step(0.85, hash(vec2(uTime * 25.0, floor(uv.y * 15.0))));
        uv.x += (hash(vec2(uTime * 30.0, uv.y)) - 0.5) * 0.18 * noiseY * uGlitchIntensity;
    }

    vec3 finalColor = vec3(0.01, 0.01, 0.015);

    if (uSubPhase == 0) {
        // [阶段 0]: VIBRATIONS & COMPLETION (谐波振荡与晶体补完)
        float r = length(uv);
        float angle = atan(uv.y, uv.x);

        // 多频共振波纹
        float vib = sin(r * 26.0 - uTime * 18.0) * sin(angle * 8.0);
        float vibRing = smoothstep(0.3, 0.9, vib);

        // 补完晶体外壳
        float poly = abs(max(abs(uv.x), abs(uv.y)) - 0.48);
        float completionBox = drawLine(poly, 0.008);

        vec3 compColor = mix(vec3(0.0, 1.0, 0.8), vec3(0.6, 0.1, 1.0), vibRing);
        finalColor += compColor * (vibRing * 0.7 + completionBox * 1.3) * (1.1 + uBeatPulse * 0.6);

    } else if (uSubPhase == 1) {
        // [阶段 1]: ISOLATION (孤立坍缩、逐层熄灭与心跳血斑)
        float dist = length(uv);

        // 5 次沉重寻觅的视野黑洞收缩
        float shrink = clamp(1.0 - (uTime / 7.5), 0.15, 1.0);
        float island = smoothstep(shrink * 0.6, shrink * 0.15, dist);

        // 孤岛边缘颤抖光环
        float ring = drawLine(dist - shrink * 0.4, 0.007);

        // 孤独心跳暗红微光
        vec3 isolColor = vec3(1.0, 0.05, 0.15);
        finalColor += isolColor * ring * (1.5 + uBeatPulse * 1.2);
        finalColor += vec3(0.15, 0.0, 0.03) * island * (uBeatPulse * 0.8 + 0.2);

    } else if (uSubPhase == 2) {
        // [阶段 2]: FRAGMENTS & DISHEARTENED (破碎数据碎片与残损记忆)
        vec2 grid = fract(uv * 6.0 + vec2(uTime * 0.2, -uTime * 0.5)) - 0.5;
        float fragDist = length(grid);
        float fragParticle = smoothstep(0.12, 0.0, fragDist);

        // 内存擦除失败警告线
        float scanline = drawLine(fract(uv.y * 8.0 - uTime * 2.0) - 0.5, 0.06);

        vec3 fragColor = vec3(0.4, 0.6, 0.8);
        finalColor += fragColor * fragParticle * 0.8;
        finalColor += vec3(0.2, 0.05, 0.1) * scanline * 0.6;

    } else {
        // [阶段 3]: ILLEGAL ARGUMENTS (严重红屏死机、异常抛出毛刺与警戒)
        // 剧烈屏幕撕裂
        float tear = step(0.7, hash(vec2(floor(uv.y * 20.0), uTime * 15.0)));
        vec3 errorRed = vec3(0.9, 0.02, 0.02);

        // 全屏警戒闪烁
        float flash = step(0.5, sin(uTime * 35.0));
        finalColor += errorRed * (0.3 + flash * 0.4);

        // 核心报错十字瞄准框
        float target = drawLine(length(uv) - 0.5, 0.015);
        float cross = drawLine(uv.x, 0.006) + drawLine(uv.y, 0.006);
        finalColor += vec3(1.0, 0.9, 0.2) * (target + cross) * (1.2 + uBeatPulse);

        // 模拟色彩分离
        finalColor.r += tear * 0.6;
        finalColor.b += (1.0 - tear) * 0.3;
    }

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.3), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""