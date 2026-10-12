package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class WorldBootGridKraftShader(
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

    override fun loadFragmentShader(): String = WORLD_BOOT_GRID_FRAGMENT_SHADER
}

@Language("GLSL")
private const val WORLD_BOOT_GRID_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform float uBeatPulse;
uniform float uProgress;

void main() {
    // 归一化中心坐标 (-1.0 ~ 1.0)
    vec2 p = (textureCoordinate * 2.0 - 1.0);
    p.y = -p.y;

    vec3 finalColor = vec3(0.01, 0.02, 0.03);

    // 地平线高度与消隐点
    float horizon = 0.05;

    if (p.y < horizon) {
        // 3D 透视地面投影
        float depth = 1.0 / max(horizon - p.y, 0.001);
        float forwardSpeed = uTime * 3.5;

        vec2 gridPos = vec2(p.x * depth * 1.5, depth + forwardSpeed);

        // 兼容 GLES 2.0 的抗锯齿网格计算（替代 fwidth）
        vec2 gridFract = abs(fract(gridPos - 0.5) - 0.5);
        float lineWidth = clamp(0.06 * (depth * 0.08 + 1.0), 0.03, 0.25);
        float lineX = smoothstep(lineWidth, 0.0, gridFract.x);
        float lineY = smoothstep(lineWidth, 0.0, gridFract.y);
        float line = max(lineX, lineY);

        // 钢琴重音脉冲产生网格辉光加成
        float pulseGlow = uBeatPulse * 0.45;
        vec3 gridColor = mix(vec3(0.1, 0.5, 0.8), vec3(0.0, 1.0, 0.6), uProgress);

        // 随距离产生雾化消隐
        float fog = clamp(1.0 / (depth * 0.15 + 1.0), 0.0, 1.0);
        finalColor += gridColor * line * (0.8 + pulseGlow) * fog;

        // 地面中央发光导轨
        float centerTrack = smoothstep(0.12, 0.0, abs(p.x * depth * 0.4));
        finalColor += vec3(0.2, 0.8, 1.0) * centerTrack * 0.4 * fog;
    } else {
        // 天空背景与星芒/数据粒子
        float skyY = p.y - horizon;
        finalColor += vec3(0.02, 0.06, 0.1) * max(1.0 - skyY * 1.2, 0.0);

        // 天际线发光条
        float horizonGlow = smoothstep(0.06, 0.0, abs(skyY));
        finalColor += vec3(0.3, 0.8, 1.0) * horizonGlow * (0.7 + uBeatPulse * 0.5);

        // 悬浮数字刻度线
        float horizLine = abs(fract(skyY * 8.0) - 0.5);
        if (horizLine < 0.04 && abs(p.x) < 0.65) {
            finalColor += vec3(0.0, 0.4, 0.7) * (0.3 + uBeatPulse * 0.2);
        }
    }

    // 全息准星 (Crosshair)
    float crosshairDist = length(p);
    float ring = abs(crosshairDist - 0.35);
    if (ring < 0.005) {
        finalColor += vec3(0.0, 1.0, 0.8) * 0.6;
    }

    // 细微暗角
    vec2 uv = textureCoordinate;
    float vignette = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vignette = clamp(pow(16.0 * max(vignette, 0.0), 0.25), 0.0, 1.0);
    finalColor *= vignette;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""