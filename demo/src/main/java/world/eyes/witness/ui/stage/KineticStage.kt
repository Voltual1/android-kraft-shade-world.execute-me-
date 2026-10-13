package world.eyes.witness.ui.stage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import world.eyes.witness.director.SceneState
import world.eyes.witness.director.SceneType
import world.eyes.witness.timeline.TimelineSnapshot
import world.eyes.witness.ui.theme.MvFonts
import world.eyes.witness.data.GodDrinksJavaSource

@Composable
fun KineticStageOverlay(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val rawCurrentText = snapshot.currentLine?.text ?: ""
    val rawNextText = snapshot.nextLine?.text ?: ""

    // 过滤时间轴括号注释（间奏、开场与结尾），不作为歌词渲染
    val isComment = rawCurrentText.startsWith("(") && rawCurrentText.endsWith(")")
    val currentText = if (isComment) "" else rawCurrentText
    val nextText = if (rawNextText.startsWith("(") && rawNextText.endsWith(")")) "" else rawNextText

    val scaleAnim = remember { Animatable(1f) }
    val alphaAnim = remember { Animatable(1f) }
    val transYAnim = remember { Animatable(0f) }

    val isImpactWord = currentText in listOf(
        "PROTECTION", "OBJECT CREATION", "INITIALIZATION", "SIMULATION",
        "DIMENSION", "CIRCUMFERENCE", "TANGENTS", "LIMITATIONS",
        "STIMULATIONS", "SATISFACTION", "EXECUTION", "COMPLETION",
        "ISOLATION", "ILLEGAL ARGUMENTS", "LO-O-OVE"
    ) || currentText.contains("EXECUTION")

    LaunchedEffect(currentText) {
        if (currentText.isNotEmpty()) {
            if (isImpactWord) {
                scaleAnim.snapTo(1.65f)
                scaleAnim.animateTo(1.0f, tween(240, easing = FastOutSlowInEasing))
                transYAnim.snapTo(16f)
                transYAnim.animateTo(0f, tween(200))
                alphaAnim.snapTo(0.3f)
                alphaAnim.animateTo(1.0f, tween(160))
            } else {
                scaleAnim.snapTo(1.15f)
                scaleAnim.animateTo(1.0f, tween(180))
                transYAnim.snapTo(-8f)
                transYAnim.animateTo(0f, tween(180))
                alphaAnim.snapTo(0.6f)
                alphaAnim.animateTo(1.0f, tween(150))
            }
        }
    }

    // 根据歌词行号与场景，自适应动态位置锚点（避免死焊在中央遮挡核心Shader内容）
    val alignment = when {
        isImpactWord -> Alignment.Center
        sceneState.scene == SceneType.GEOMETRY_MATH -> Alignment.CenterStart
        sceneState.scene == SceneType.ALGEBRAIC_LOVE -> Alignment.BottomCenter
        snapshot.currentLineIndex % 3 == 0 -> Alignment.CenterStart
        snapshot.currentLineIndex % 3 == 1 -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 背景代码流 (低透明度信息密度填充)
        val scrollY = -(sceneState.globalTimeSeconds * 40f % 2000f).dp
        Text(
            text = GodDrinksJavaSource.FULL_CODE + "\n\n" + GodDrinksJavaSource.FULL_CODE,
            color = Color.White.copy(alpha = 0.08f),
            fontFamily = MvFonts.CodeRegular,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = scrollY)
                .padding(horizontal = 24.dp)
        )

        // 核心动力学单元：歌词与对应的 Java 运行代码紧紧相随
        if (currentText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isImpactWord) 20.dp else 40.dp, vertical = 48.dp),
                contentAlignment = alignment
            ) {
                Column(
                    horizontalAlignment = when (alignment) {
                        Alignment.CenterStart -> Alignment.Start
                        Alignment.CenterEnd -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    },
                    modifier = Modifier
                        .offset(y = transYAnim.value.dp)
                        .scale(scaleAnim.value)
                        .alpha(alphaAnim.value)
                ) {
                    // 1. 紧随歌词顶部的元数据注释 / 方法签名
                    Text(
                        text = getCodePrefix(sceneState, currentText),
                        color = Color.Cyan.copy(alpha = 0.55f + sceneState.beatPulse * 0.3f),
                        fontFamily = MvFonts.Terminal,
                        fontSize = 13.sp,
                        textAlign = when (alignment) {
                            Alignment.CenterStart -> TextAlign.Start
                            Alignment.CenterEnd -> TextAlign.End
                            else -> TextAlign.Center
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. 歌词主文本
                    if (isImpactWord) {
                        Text(
                            text = currentText,
                            color = getImpactColor(sceneState),
                            fontFamily = MvFonts.DisplayBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = if (currentText.length > 12) 38.sp else 48.sp,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 52.sp
                        )
                    } else {
                        Text(
                            text = currentText,
                            color = Color.White,
                            fontFamily = MvFonts.CodeBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = 1.sp,
                            textAlign = when (alignment) {
                                Alignment.CenterStart -> TextAlign.Start
                                Alignment.CenterEnd -> TextAlign.End
                                else -> TextAlign.Center
                            },
                            lineHeight = 36.sp
                        )
                    }

                    // 3. 紧随歌词底部的对应实装代码（带终端光标感）
                    val boundCode = getBoundCode(sceneState, currentText)
                    if (boundCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$boundCode _",
                            color = Color(0xFF80D8FF).copy(alpha = 0.85f),
                            fontFamily = MvFonts.CodeRegular,
                            fontSize = 12.sp,
                            textAlign = when (alignment) {
                                Alignment.CenterStart -> TextAlign.Start
                                Alignment.CenterEnd -> TextAlign.End
                                else -> TextAlign.Center
                            }
                        )
                    }

                    // 4. 预览下一句词
                    if (nextText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = ">> $nextText",
                            color = Color.White.copy(alpha = 0.3f),
                            fontFamily = MvFonts.Terminal,
                            fontSize = 14.sp,
                            textAlign = when (alignment) {
                                Alignment.CenterStart -> TextAlign.Start
                                Alignment.CenterEnd -> TextAlign.End
                                else -> TextAlign.Center
                            }
                        )
                    }
                }
            }
        }

        // 轻量级系统状态指示
        CornerMetadataOverlay(sceneState = sceneState)
    }
}

@Composable
private fun CornerMetadataOverlay(sceneState: SceneState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SYS::${sceneState.scene.name}",
            color = Color.White.copy(alpha = 0.35f),
            fontFamily = MvFonts.Terminal,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        Text(
            text = "${String.format("%.2f", sceneState.globalTimeSeconds)}s / 130BPM",
            color = Color.White.copy(alpha = 0.35f),
            fontFamily = MvFonts.Terminal,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

private fun getCodePrefix(sceneState: SceneState, currentText: String): String {
    return when {
        currentText.contains("EXECUTION") -> "// [CRITICAL] world.runExecution()"
        currentText.contains("LO-O-OVE") -> "// [MATH_MODEL] algebraic(love)"
        sceneState.scene == SceneType.GEOMETRY_MATH -> "// [TOPOLOGY] point_set.dimension"
        sceneState.scene == SceneType.CURRENT_CONVERSION -> "// [POWER] current.convert"
        sceneState.scene == SceneType.BIOLOGY_EXISTENCE -> "// [BIOLOGY] entity.nutrient"
        sceneState.scene == SceneType.GENDER_AND_ROLES -> "// [STATE] role.toggle"
        sceneState.scene == SceneType.ISOLATION_AND_ERROR -> "// [EXCEPTION] null_pointer"
        else -> "// [EXEC] goddrinksjava.Runtime"
    }
}

private fun getBoundCode(sceneState: SceneState, currentText: String): String {
    return when {
        currentText.contains("power line") -> "world.startSimulation();"
        currentText.contains("PROTECTION") -> "you.addAttribute(me.getDimensions().toAttribute());"
        currentText.contains("OBJECT CREATION") -> "world.addThing(new Lovable(\"Me\"));"
        currentText.contains("INITIALIZATION") -> "world.addThing(new Lovable(\"You\"));"
        currentText.contains("SIMULATION") -> "world.startSimulation();"
        currentText.contains("DIMENSION") -> "you.addAttribute(me.getDimensions().toAttribute());"
        currentText.contains("CIRCUMFERENCE") -> "you.addAttribute(me.getCircumference().toAttribute());"
        currentText.contains("TANGENTS") -> "you.addAction(\"sit\", me.getTangent(you.getXPosition()));"
        currentText.contains("LIMITATIONS") -> "me.setLimit(you.toLimit());"
        currentText.contains("AC") || currentText.contains("DC") -> "me.toggleCurrent();"
        currentText.contains("dizzy") -> "me.canSee(false); me.addFeeling(\"dizzy\");"
        currentText.contains("A.D.") || currentText.contains("B.C.") -> "world.timeTravelForTwo(\"AC\", 617, me, you);"
        currentText.contains("unite") || currentText.contains("deeply") -> "world.unite(me, you);"
        currentText.contains("STIMULATIONS") -> "you.setSatisfaction(me.toSatisfaction());"
        currentText.contains("SATISFACTION") -> "you.setSatisfaction(100);"
        currentText.contains("happy") || (currentText.contains("EXECUTION") && sceneState.scene == SceneType.SIMULATION_EXECUTION) -> "me.requestExecution(world);"
        currentText.contains("trapped") -> "world.lockThing(me); world.lockThing(you);"
        currentText.contains("NUTRIENTS") || currentText.contains("eggplant") -> "you.addAttribute(me.getNutrients().toAttribute());"
        currentText.contains("ANTIOXIDANTS") || currentText.contains("tomato") -> "you.addAttribute(me.getAntioxidants().toAttribute());"
        currentText.contains("cat") || currentText.contains("purr") || currentText.contains("ENJOYMENT") -> "me.purr();"
        currentText.contains("God") || currentText.contains("EXISTENCE") -> "me.setProof(you.toProof());"
        currentText.contains("gender") || currentText.contains("F, to M") -> "me.toggleGender(); world.procreate(me, you);"
        currentText.contains("A.M. to P.M.") -> "// diurnal_cycle_loop"
        currentText.contains("role") || currentText.contains("S, to M") -> "me.toggleRoleBDSM();"
        currentText.contains("trance") -> "world.makeHigh(me); world.makeHigh(you);"
        currentText.contains("VIBRATIONS") || currentText.contains("COMPLETION") -> "me.addFeeling(\"complete\");"
        currentText.contains("left me in") || currentText.contains("ISOLATION") -> "world.removeThing(you); // not found"
        currentText.contains("FRAGMENTS") || currentText.contains("DISHEARTENED") -> "if (me.getMemory().isErasable()) me.removeFeeling(\"disheartened\");"
        currentText.contains("ILLEGAL ARGUMENTS") -> "throw new IllegalArgumentException(\"God is always true\");"
        currentText.contains("Ein") || currentText.contains("Trois") || currentText.contains("Fem") -> "world.announce(\"countdown\");"
        currentText.contains("EXECUTION") -> "world.runExecution();"
        currentText.contains("LO-O-OVE") || currentText.contains("algebraic") -> "(x^2 + y^2 - 1)^3 - x^2 * y^3 = 0;"
        currentText.contains("execute(me)") -> "world.execute(me); // exit 0"
        else -> ""
    }
}

private fun getImpactColor(sceneState: SceneState): Color {
    return when (sceneState.scene) {
        SceneType.BOOT_SEQUENCE -> Color(0xFF00FF66)
        SceneType.GEOMETRY_MATH -> Color(0xFF00E5FF)
        SceneType.CURRENT_CONVERSION -> Color(0xFFFFD600)
        SceneType.SIMULATION_EXECUTION,
        SceneType.RAPID_EXECUTION_STORM,
        SceneType.DESPERATE_EXECUTION -> Color(0xFFFF1744)
        SceneType.BIOLOGY_EXISTENCE -> Color(0xFFFFD54F)
        SceneType.GENDER_AND_ROLES -> Color(0xFFE040FB)
        SceneType.ISOLATION_AND_ERROR,
        SceneType.ARGUMENT_STACK -> Color(0xFFFF3D00)
        SceneType.ALGEBRAIC_LOVE -> Color(0xFFFF4081)
        else -> Color.White
    }
}