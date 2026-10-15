package world.eyes.witness.shader

import com.cardinalblue.kraftshade.model.GlVec2
import com.cardinalblue.kraftshade.shader.TextureInputKraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class BreakpointFreezeKraftShader : TextureInputKraftShader() {
    var freezeCenter: GlVec2 by GlUniformDelegate("uFreezeCenter", required = false)
    var freezeProgress: Float by GlUniformDelegate("uFreezeProgress", required = false)
    var time: Float by GlUniformDelegate("uTime", required = false)

    init {
        freezeCenter = GlVec2(0.5f, 0.5f)
        freezeProgress = 0f
        time = 0f
    }

    override fun loadFragmentShader(): String = BREAKPOINT_FREEZE_FRAGMENT_SHADER
}

@Language("GLSL")
private const val BREAKPOINT_FREEZE_FRAGMENT_SHADER = """
precision highp float;

varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform vec2 resolution;

uniform vec2 uFreezeCenter;
uniform float uFreezeProgress;
uniform float uTime;

void main() {
    vec2 uv = textureCoordinate;

    if (uFreezeProgress <= 0.001) {
        gl_FragColor = texture2D(inputImageTexture, uv);
        return;
    }

    float aspect = resolution.x / (resolution.y > 0.0 ? resolution.y : 1.0);
    vec2 pUv = vec2(uv.x * aspect, uv.y);
    vec2 pCenter = vec2(uFreezeCenter.x * aspect, uFreezeCenter.y);

    float dist = distance(pUv, pCenter);

    // 暂停冻结引力半径 (随时间微弱呼吸)
    float baseRadius = 0.35 + sin(uTime * 3.0) * 0.02;
    float influence = smoothstep(baseRadius, 0.0, dist) * uFreezeProgress;

    // 1. 引力透镜色相分离 (Chromatic Aberration)
    vec2 dir = (dist > 0.0001) ? normalize(uv - uFreezeCenter) : vec2(0.0);
    vec2 offset = dir * influence * 0.035;

    float r = texture2D(inputImageTexture, uv + offset * 1.5).r;
    float g = texture2D(inputImageTexture, uv + offset * 0.5).g;
    float b = texture2D(inputImageTexture, uv - offset * 1.5).b;
    vec3 color = vec3(r, g, b);

    // 2. 暂停全域冰晶扫描暗调
    color = mix(color, color * vec3(0.5, 0.75, 1.0) + vec3(0.0, 0.08, 0.15), uFreezeProgress * 0.5);

    // 3. 动态扩散的断点雷达同心环
    float pulseDist = fract(dist * 4.0 - uTime * 1.5);
    float pulseRing = smoothstep(0.1, 0.0, abs(pulseDist - 0.5));
    color += vec3(0.0, 1.0, 0.85) * pulseRing * influence * 1.2;

    // 4. 核心断点十字线与旋转准星
    float crossX = smoothstep(0.002, 0.0, abs(pUv.x - pCenter.x)) * step(dist, baseRadius * 0.8);
    float crossY = smoothstep(0.002, 0.0, abs(pUv.y - pCenter.y)) * step(dist, baseRadius * 0.8);
    color += vec3(1.0, 0.9, 0.2) * (crossX + crossY) * uFreezeProgress;

    // 5. 旋转全息外刻度环
    float ringBound = smoothstep(0.008, 0.0, abs(dist - 0.22));
    float angle = atan(pUv.y - pCenter.y, pUv.x - pCenter.x) + uTime * 2.0;
    float ticks = step(0.6, sin(angle * 12.0));
    color += vec3(0.0, 0.9, 1.0) * ringBound * ticks * uFreezeProgress * 1.5;

    gl_FragColor = vec4(color, 1.0);
}
"""