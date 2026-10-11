package world.eyes.witness.shader

import com.cardinalblue.kraftshade.shader.KraftShader
import com.cardinalblue.kraftshade.shader.util.GlUniformDelegate
import org.intellij.lang.annotations.Language

class GenderRolesKraftShader(
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

    override fun loadFragmentShader(): String = GENDER_ROLES_FRAGMENT_SHADER
}

@Language("GLSL")
private const val GENDER_ROLES_FRAGMENT_SHADER = """
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

    vec3 finalColor = vec3(0.015, 0.01, 0.02);

    if (uSubPhase == 0) {
        // [阶段 0]: GENDER F / M 翻转与符号几何叠加
        float isM = step(0.5, fract(uTime * 2.0)); // F 与 M 快速交替
        float r = length(uv);

        // 核心圆环
        float baseRing = drawLine(r - 0.28, 0.008);

        float symbolExtra = 0.0;
        if (isM > 0.5) {
            // ♂ 雄性箭头: 东北向延伸与横竖交叉箭头
            vec2 dir = normalize(vec2(1.0, 1.0));
            float proj = dot(uv, dir);
            if (proj >= 0.28 && proj <= 0.58) {
                float perp = length(uv - dir * proj);
                symbolExtra += drawLine(perp, 0.007);
            }
            // 箭头尖角线
            vec2 tip = dir * 0.58;
            symbolExtra += drawLine(uv.y - tip.y, 0.007) * step(tip.x - 0.12, uv.x) * step(uv.x, tip.x);
            symbolExtra += drawLine(uv.x - tip.x, 0.007) * step(tip.y - 0.12, uv.y) * step(uv.y, tip.y);

            finalColor += vec3(0.0, 0.75, 1.0) * (baseRing + symbolExtra) * (1.2 + uBeatPulse * 0.6);
        } else {
            // ♀ 雌性十字: 向南垂直竖线与水平横杠
            if (uv.y <= -0.28 && uv.y >= -0.62) {
                symbolExtra += drawLine(uv.x, 0.007);
            }
            if (abs(uv.y - (-0.45)) <= 0.007 && abs(uv.x) <= 0.12) {
                symbolExtra += 1.0;
            }

            finalColor += vec3(1.0, 0.2, 0.65) * (baseRing + symbolExtra) * (1.2 + uBeatPulse * 0.6);
        }

    } else if (uSubPhase == 1) {
        // [阶段 1]: A.M. TO P.M. 极速日晷时钟与昼夜交替
        float dist = length(uv);
        float angle = atan(uv.y, uv.x);

        // 表盘刻度
        float dialRing = drawLine(dist - 0.55, 0.008);
        float ticks = drawLine(fract(angle * 12.0 / 6.28318) - 0.5, 0.08) * step(0.48, dist) * step(dist, 0.55);

        // 指针飞速旋转 (时针与分针)
        float handAngle = uTime * 12.0;
        vec2 handDir = vec2(cos(handAngle), sin(handAngle));
        float handProj = dot(uv, handDir);
        float hand = 0.0;
        if (handProj >= 0.0 && handProj <= 0.48) {
            hand = drawLine(length(uv - handDir * handProj), 0.008);
        }

        // AM (日出金光) 与 PM (深蓝夜幕) 旋转扫描
        float isDay = sin(uTime * 3.0) * 0.5 + 0.5;
        vec3 timeColor = mix(vec3(0.05, 0.2, 0.6), vec3(1.0, 0.7, 0.1), isDay);

        finalColor += timeColor * (dialRing + ticks + hand) * (1.1 + uBeatPulse * 0.7);

    } else if (uSubPhase == 2) {
        // [阶段 2]: ROLE S / M 锁链与反转符号
        // 环形锁链几何阵列
        float chainRadius = 0.45;
        float numLinks = 10.0;
        float angle = atan(uv.y, uv.x) + uTime * 2.0;
        float linkSector = fract(angle * numLinks / 6.28318);
        float linkGlow = drawLine(linkSector - 0.5, 0.12) * drawLine(length(uv) - chainRadius, 0.04);

        // 强对比黑红与金色闪烁
        vec3 roleColor = (sin(uTime * 6.0) > 0.0) ? vec3(1.0, 0.1, 0.2) : vec3(0.9, 0.75, 0.1);
        finalColor += roleColor * linkGlow * (1.3 + uBeatPulse * 0.8);

        // 交叉锁链破损线条
        float x1 = drawLine(uv.y - uv.x * 0.8, 0.006) * step(length(uv), 0.6);
        float x2 = drawLine(uv.y + uv.x * 0.8, 0.006) * step(length(uv), 0.6);
        finalColor += vec3(0.8, 0.8, 0.8) * (x1 + x2) * 0.6;

    } else {
        // [阶段 3]: THE TRANCE (同心催眠漩涡阿基米德螺线与空间恍惚)
        float dist = length(uv);
        float angle = atan(uv.y, uv.x);

        // 旋转催眠螺线: r - k * theta
        float spiral = sin(dist * 28.0 - angle * 5.0 - uTime * 15.0);
        float tranceWave = smoothstep(0.2, 0.85, spiral);

        // 恍惚紫色与霓虹绿迷幻色调
        vec3 tranceColor = mix(vec3(0.6, 0.0, 1.0), vec3(0.0, 1.0, 0.7), sin(uTime * 4.0 + dist * 6.0) * 0.5 + 0.5);

        finalColor += tranceColor * tranceWave * (1.2 + uBeatPulse * 0.9);
        // 中心升格强光点
        finalColor += vec3(1.0, 0.9, 1.0) * exp(-dist * 4.0) * 1.8;
    }

    // 暗角与扫描暗纹
    vec2 tc = textureCoordinate;
    float vig = tc.x * tc.y * (1.0 - tc.x) * (1.0 - tc.y);
    vig = clamp(pow(16.0 * vig, 0.28), 0.0, 1.0);
    finalColor *= vig;

    gl_FragColor = vec4(finalColor, 1.0);
}
"""