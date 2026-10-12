package world.eyes.witness.ui.stage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.rotate
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

@Composable
fun KineticStageOverlay(
    sceneState: SceneState,
    snapshot: TimelineSnapshot,
    modifier: Modifier = Modifier
) {
    val rawCurrentText = snapshot.currentLine?.text ?: ""
    val rawNextText = snapshot.nextLine?.text ?: ""

    // 过滤时间轴括号注释（间奏、开场与结尾），不作为大歌词显示
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
                transYAnim.snapTo(18f)
                transYAnim.animateTo(0f, tween(200))
                alphaAnim.snapTo(0.3f)
                alphaAnim.animateTo(1.0f, tween(160))
            } else {
                scaleAnim.snapTo(1.15f)
                scaleAnim.animateTo(1.0f, tween(180))
                transYAnim.snapTo(-10f)
                transYAnim.animateTo(0f, tween(180))
                alphaAnim.snapTo(0.6f)
                alphaAnim.animateTo(1.0f, tween(150))
            }
        }
    }

    // 根据歌词行号与场景，自适应动态位置锚点（打破永远定死在中心的呆板）
    val alignment = when {
        isImpactWord -> Alignment.Center
        snapshot.currentLineIndex % 3 == 0 -> Alignment.CenterStart
        snapshot.currentLineIndex % 3 == 1 -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 全屏动态背景代码流动层
        BackgroundStreamingCode(sceneState = sceneState)

        // 2. 动力学歌词主体（间奏时完全隐藏，保证画面纯净）
        if (currentText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isImpactWord) 20.dp else 40.dp, vertical = 32.dp),
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

                    if (nextText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = ">> $nextText",
                            color = Color.White.copy(alpha = 0.35f),
                            fontFamily = MvFonts.Terminal,
                            fontSize = 15.sp,
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

        // 3. 四角轻量级微标
        CornerMetadataOverlay(sceneState = sceneState)
    }
}

@Composable
private fun BackgroundStreamingCode(sceneState: SceneState) {
    val codeSnippet = when (sceneState.scene) {
        SceneType.BOOT_SEQUENCE -> "World world = new World(5); world.addThing(me); world.addThing(you);"
        SceneType.WORLD_BOOT_INSTRUMENTAL -> "world.startSimulation(); // 3D Perspective Vector Space"
        SceneType.GEOMETRY_MATH -> "you.addAttribute(me.getDimensions().toAttribute()); me.resetDimentions();"
        SceneType.CURRENT_CONVERSION -> "me.toggleCurrent(); world.timeTravelForTwo(\"AC\", 617, me, you);"
        SceneType.SIMULATION_EXECUTION -> "if (you.getFeelingIndex(\"happy\") != -1) me.requestExecution(world);"
        SceneType.BIOLOGY_EXISTENCE -> "if (world.getGod().equals(me)) me.setProof(you.toProof());"
        SceneType.GENDER_AND_ROLES -> "me.toggleGender(); world.makeHigh(me); me.toggleRoleBDSM();"
        SceneType.ISOLATION_AND_ERROR -> "world.removeThing(you); // java.lang.IllegalArgumentException"
        SceneType.ARGUMENT_STACK -> "at goddrinksjava.GodDrinksJava.main(GodDrinksJava.java:112)"
        SceneType.RAPID_EXECUTION_STORM -> "for (int i=0; i<12; i++) world.runExecution();"
        SceneType.DESPERATE_EXECUTION -> "you.setExecution(me.toExecution()); me.escape();"
        SceneType.ALGEBRAIC_LOVE -> "(x^2 + y^2 - 1)^3 - x^2 * y^3 = 0 // LO-O-OVE"
        SceneType.OPEN_LOOP_SHUTDOWN -> "world.execute(me); // exit 0"
    }

    // 动态斜向背景代码瀑布流
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.16f + sceneState.beatPulse * 0.14f)
            .rotate(-7f)
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            repeat(3) {
                Text(
                    text = codeSnippet,
                    color = Color.Cyan,
                    fontFamily = MvFonts.CodeRegular,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
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
            color = Color.White.copy(alpha = 0.4f),
            fontFamily = MvFonts.Terminal,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        Text(
            text = "${String.format("%.2f", sceneState.globalTimeSeconds)}s / 130BPM",
            color = Color.White.copy(alpha = 0.4f),
            fontFamily = MvFonts.Terminal,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
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