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

float dot2(vec2 v) { 
    return dot(v, v); 
}

// Inigo Quilez 精确心形距离场 (Exact Heart SDF)
// 这是一个完美的、平滑连接的数学心形
float sdHeart(vec2 p) {
    p.x = abs(p.x);
    if(p.y + p.x > 1.0) {
        return sqrt(dot2(p - vec2(0.25, 0.75))) - sqrt(2.0) / 4.0;
    }
    return sqrt(min(dot2(p - vec2(0.00, 1.00)),
                    dot2(p - 0.5 * max(p.x + p.y, 0.0)))) * sign(p.x - p.y);
}

void main() {
    // 归一化屏幕坐标
    vec2 uv = (textureCoordinate * 2.0 - 1.0);
    // Y 轴翻转，保持标准的笛卡尔正立坐标系 (上正下负)
    uv.y = -uv.y;
    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    uv.x *= aspect;

    vec3 finalColor = vec3(0.015, 0.01, 0.02);

    // 心跳微弱脉动与冲击缩放
    float beatScale = 1.0 + sin(uTime * 6.0) * 0.03 + uBeatPulse * 0.15;
    vec2 p = uv / beatScale;

    // 缩放并平移坐标系，使 SDF 完美心形居中并填满屏幕合适比例
    p *= 1.6;
    p.y += 0.65;

    // 1. 获取精确心形距离场
    float hSdf = sdHeart(p);
    
    // 恢复到屏幕真实比例的距离
    float trueDist = hSdf / 1.6;

    // 完美心形轮廓线
    float heartOutline = drawLine(trueDist, 0.015);
    // 内部柔和充盈发光
    float heartFill = smoothstep(0.02, -0.25, trueDist);

    // 霓虹粉与红紫渐变
    vec3 heartColor = mix(vec3(1.0, 0.15, 0.6), vec3(1.0, 0.85, 0.95), uBeatPulse);
    finalColor += heartColor * heartOutline * (2.0 + uBeatPulse * 1.5);
    finalColor += vec3(0.65, 0.05, 0.35) * heartFill * (0.6 + uBeatPulse * 0.8);

    // 2. 外部扩散的多重同心心形声波
    // 严格贴合真正的心形轮廓向外辐射
    float waveDist = fract(trueDist * 5.0 - uTime * 3.0);
    float heartWave = smoothstep(0.15, 0.0, abs(waveDist - 0.5)) * step(0.0, trueDist);
    finalColor += vec3(0.9, 0.25, 0.65) * heartWave * 0.55;

    // 3. 背景极坐标射线
    float angle = atan(uv.y, uv.x);
    float rays = smoothstep(0.2, 0.9, sin(angle * 16.0 + uTime * 2.0));
    finalColor += vec3(0.5, 0.1, 0.7) * rays * exp(-length(uv) * 1.8) * 0.35;

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * max(vig, 0.0), 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""