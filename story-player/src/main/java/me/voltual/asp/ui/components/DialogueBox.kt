package me.voltual.asp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.R
import me.voltual.asp.engine.STORY_HEIGHT
import me.voltual.asp.engine.STORY_WIDTH
import me.voltual.asp.engine.rendering.DialogueVisualState
import kotlin.math.max

private const val BOTTOM_HEIGHT = 182f
private const val NAME_LEFT = 20.7501f
private const val NAME_WIDTH = 312.3f
private const val NAME_TOP = 84.9f
private const val NAME_FONT_MAX = 30
private const val NAME_FONT_MIN = 25
private const val MESSAGE_LEFT = 23.88f
private const val MESSAGE_ASIDE_LEFT = -86f
private const val MESSAGE_ANCHOR_X = 0.28f
private const val MESSAGE_TOP = 93.5f
private const val MESSAGE_WIDTH = 735f
private const val MESSAGE_MAX_HEIGHT = 89f
private const val NAME_MAX_HEIGHT = 65f
private const val HIDE_FADE_MS = 150

private fun parseTaggedTextToAnnotated(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val tagRegex = Regex("""<(/?)(_b|_i|_c[0-9a-fA-F]+)>""")
        val matches = tagRegex.findAll(text).toList()

        var boldDepth = 0
        var italicDepth = 0
        var currentColor: Color? = null

        fun pushCurrentStyle() {
            var style = SpanStyle()
            if (boldDepth > 0) style = style.copy(fontWeight = FontWeight.Bold)
            if (italicDepth > 0) style = style.copy(fontStyle = FontStyle.Italic)
            if (currentColor != null) style = style.copy(color = currentColor!!)
            pushStyle(style)
        }

        for (match in matches) {
            val literalText = text.substring(cursor, match.range.first)
            if (literalText.isNotEmpty()) {
                append(literalText)
            }
            cursor = match.range.last + 1

            val isClose = match.groupValues[1] == "/"
            val tag = match.groupValues[2]

            when {
                tag == "_b" -> boldDepth = if (isClose) maxOf(0, boldDepth - 1) else boldDepth + 1
                tag == "_i" -> italicDepth = if (isClose) maxOf(0, italicDepth - 1) else italicDepth + 1
                tag.startsWith("_c") -> {
                    currentColor = if (isClose) null else {
                        val hex = tag.removePrefix("_c")
                        try {
                            Color(android.graphics.Color.parseColor("#$hex"))
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
            pushCurrentStyle()
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@Composable
fun DialogueBox(
    dialogue: DialogueVisualState,
    modifier: Modifier = Modifier
) {
    // 150ms Linear fade-in / fade-out aligned with Web DialogPanel.HIDE_FADE_MS
    val animatedAlpha by animateFloatAsState(
        targetValue = if (dialogue.visible) 1f else 0f,
        animationSpec = tween(durationMillis = HIDE_FADE_MS, easing = LinearEasing),
        label = "dialogue_fade"
    )

    if (animatedAlpha <= 0f && !dialogue.visible) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // State preserved across typewriter frames for the same command
    var currentSpeakerFontSize by remember { mutableIntStateOf(NAME_FONT_MAX) }
    var currentSpeakerY by remember { mutableFloatStateOf(STORY_HEIGHT - BOTTOM_HEIGHT + NAME_TOP) }
    var currentMessageX by remember { mutableFloatStateOf(STORY_WIDTH * MESSAGE_ANCHOR_X + MESSAGE_LEFT) }
    var currentMessageY by remember { mutableFloatStateOf(STORY_HEIGHT - MESSAGE_TOP) }
    var isMessageWidthExpanded by remember { mutableStateOf(false) }

    val rawTextForMeasure = dialogue.layout?.text ?: dialogue.text

    // When text or speaker changes, calculate BestFit and Y-floating offset
    LaunchedEffect(rawTextForMeasure, dialogue.speaker) {
        val speaker = dialogue.speaker
        var bestFontSize = NAME_FONT_MAX
        val nameWidthLimitPx = with(density) { NAME_WIDTH.dp.toPx() }

        if (speaker.isNotEmpty()) {
            for (size in NAME_FONT_MAX downTo NAME_FONT_MIN) {
                bestFontSize = size
                val measureResult = textMeasurer.measure(
                    text = AnnotatedString(speaker),
                    style = TextStyle(
                        fontSize = size.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                )
                if (measureResult.size.width <= nameWidthLimitPx) {
                    break
                }
            }
        }
        currentSpeakerFontSize = bestFontSize

        val speakerMeasureHeight = if (speaker.isNotEmpty()) {
            val res = textMeasurer.measure(
                text = AnnotatedString(speaker),
                style = TextStyle(
                    fontSize = bestFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            )
            with(density) { res.size.height.toDp().value }
        } else {
            0f
        }
        currentSpeakerY = (STORY_HEIGHT - BOTTOM_HEIGHT + NAME_TOP) - max(0f, speakerMeasureHeight - NAME_MAX_HEIGHT)

        val isAside = dialogue.layout?.executor == "aside"
        currentMessageX = STORY_WIDTH * MESSAGE_ANCHOR_X + if (isAside) MESSAGE_ASIDE_LEFT else MESSAGE_LEFT

        val baseWidthPx = with(density) { MESSAGE_WIDTH.dp.toPx() }.toInt()
        val firstMeasure = textMeasurer.measure(
            text = AnnotatedString(rawTextForMeasure),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 32.sp
            ),
            constraints = Constraints(maxWidth = baseWidthPx)
        )
        val firstHeightDp = with(density) { firstMeasure.size.height.toDp().value }

        var finalHeightDp = firstHeightDp
        if (dialogue.layout?.executor != "endtip" && firstHeightDp >= MESSAGE_MAX_HEIGHT) {
            isMessageWidthExpanded = true
            val expandedWidthPx = with(density) { (MESSAGE_WIDTH + 24f).dp.toPx() }.toInt()
            val secondMeasure = textMeasurer.measure(
                text = AnnotatedString(rawTextForMeasure),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 32.sp
                ),
                constraints = Constraints(maxWidth = expandedWidthPx)
            )
            finalHeightDp = with(density) { secondMeasure.size.height.toDp().value }
        } else {
            isMessageWidthExpanded = false
        }

        currentMessageY = (STORY_HEIGHT - MESSAGE_TOP) - max(0f, finalHeightDp - MESSAGE_MAX_HEIGHT)
    }

    // Restore word wrap width when finishDialogueCommand is triggered
    LaunchedEffect(dialogue.isExpanded) {
        if (!dialogue.isExpanded) {
            isMessageWidthExpanded = false
        }
    }

    val activeWordWrapWidth = if (isMessageWidthExpanded) MESSAGE_WIDTH + 24f else MESSAGE_WIDTH

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(animatedAlpha.coerceIn(0f, 1f))
    ) {
        // Top edge shadow cutscene (102dp height, upside-down)
        Image(
            painter = painterResource(id = R.drawable.sprite_avg_cutscene),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp)
                .align(Alignment.TopCenter)
                .scale(scaleX = 1f, scaleY = -1f)
                .alpha(0.7059f)
        )

        // Bottom dialog frame cutscene (182dp height)
        Image(
            painter = painterResource(id = R.drawable.sprite_avg_cutscene),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_HEIGHT.dp)
                .align(Alignment.BottomCenter)
        )

        // Speaker Name (Right aligned inside name region, anchor (1, 0))
        if (dialogue.speaker.isNotEmpty()) {
            Text(
                text = dialogue.speaker,
                color = Color(0xFF929292),
                fontSize = currentSpeakerFontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .offset(x = NAME_LEFT.dp, y = currentSpeakerY.dp)
                    .width(NAME_WIDTH.dp)
            )
        }

        // Message body with rich-text annotations and dynamic positioning
        val messageAnnotated = parseTaggedTextToAnnotated(dialogue.text)
        Text(
            text = messageAnnotated,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 32.sp,
            modifier = Modifier
                .offset(x = currentMessageX.dp, y = currentMessageY.dp)
                .width(activeWordWrapWidth.dp)
        )
    }
}