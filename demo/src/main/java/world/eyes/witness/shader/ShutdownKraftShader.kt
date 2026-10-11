package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class ShutdownKraftShader(
    time: Float = 0f,
    beatPulse: Float = 0f,
    progress: Float = 0f,
    isFinalExecution: Float = 0f
) : KraftShader() {

    var time: Float by GlUniformDelegate("uTime", required = false)
    var beatPulse: Float by GlUniformDelegate("uBeatPulse", required = false)
    var progress: Float by GlUniformDelegate("uProgress", required = false)
    var isFinalExecution: Float by GlUniformDelegate("uIsFinalExecution", required = false)

    init {
        this.time = time
        this.beatPulse = beatPulse
        this.progress = progress
        this.isFinalExecution = isFinalExecution
    }

    override fun loadFragmentShader(): String = SHUTDOWN_FRAGMENT_SHADER
}

@Language("GLSL")
private const val SHUTDOWN_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform vec2 resolution;

uniform float uTime;
uniform float uBeatPulse;
uniform float uProgress;
uniform float uIsFinalExecution;

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

    vec3 finalColor = vec3(0.01, 0.012, 0.015);

    // 03:13.460 ~ 03:25.811: 前段死循环解构 (uTime < 12.351s)
    // 03:25.811 ~ 03:26.620: 终局 EXECUTION (12.351s <= uTime < 13.160s)
    // 03:26.620 ~ 03:31.984: CRT 电视关机坍缩 (uTime >= 13.160s)

    if (uTime < 13.160) {
        // [前奏与终局处决]: 开放死循环 (Open Loop) 螺旋消散
        float r = length(uv);
        float angle = atan(uv.y, uv.x);

        // 缓缓消散的莫比乌斯 / 开放圆环
        float loopRing = drawLine(r - 0.45, 0.008);
        float loopGap = smoothstep(0.0, 0.8, sin(angle + uTime * 1.5));
        loopRing *= loopGap;

        // 磷光余辉粒子
        vec2 grid = fract(uv * 8.0 - vec2(0.0, uTime * 0.5)) - 0.5;
        float particle = smoothstep(0.08, 0.0, length(grid));

        vec3 loopColor = vec3(0.0, 0.85, 0.65);
        finalColor += loopColor * loopRing * (1.2 + uBeatPulse * 0.6);
        finalColor += vec3(0.0, 0.4, 0.3) * particle * 0.4;

        // 终局 EXECUTION 白热强闪与高危死刑十字
        if (uIsFinalExecution > 0.0) {
            float strikeCross = drawLine(uv.x, 0.008) + drawLine(uv.y, 0.008);
            float strikeRing = drawLine(r - 0.5, 0.015);
            finalColor += vec3(1.0, 0.1, 0.15) * (strikeCross + strikeRing) * 2.0;

            // 全屏反相冲击
            if (uIsFinalExecution > 0.5) {
                finalColor = vec3(1.0) - finalColor;
            }
        }
    } else {
        // [尾奏关机]: CRT 显像管断电坍缩 (Power-off Collapse)
        float shutdownTime = uTime - 13.160;
        float shutdownProgress = clamp(shutdownTime / 5.364, 0.0, 1.0);

        // 第 1 阶段 (0.0 ~ 0.45): 垂直收缩为水平白线
        // 第 2 阶段 (0.45 ~ 0.85): 水平白线收缩为中心白点
        // 第 3 阶段 (0.85 ~ 1.0): 白点辉光熄灭彻底黑屏

        float verticalScale = 1.0 - smoothstep(0.0, 0.42, shutdownProgress);
        float horizontalScale = 1.0 - smoothstep(0.38, 0.85, shutdownProgress);
        float glowFade = 1.0 - smoothstep(0.80, 1.0, shutdownProgress);

        verticalScale = max(verticalScale, 0.002);
        horizontalScale = max(horizontalScale, 0.002);

        vec2 crtUv = uv;
        crtUv.y /= verticalScale;
        crtUv.x /= horizontalScale;

        // 限制在收缩后的窗口内部
        if (abs(crtUv.x) <= aspect && abs(crtUv.y) <= 1.0) {
            float lineIntensity = 1.0 / (abs(uv.y) * 80.0 + 1.0);
            float pointIntensity = 1.0 / (length(uv) * 40.0 + 1.0);

            vec3 crtWhite = vec3(1.0, 0.98, 0.95);
            finalColor = crtWhite * (lineIntensity + pointIntensity * 2.0) * glowFade;
        } else {
            finalColor = vec3(0.0);
        }
    }

    // 边缘暗角
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.3), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""