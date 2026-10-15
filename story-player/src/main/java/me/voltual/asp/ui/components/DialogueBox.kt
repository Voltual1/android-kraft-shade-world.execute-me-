package me.voltual.asp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.R
import me.voltual.asp.engine.rendering.DialogueVisualState

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
    if (!dialogue.visible) return

    Box(modifier = modifier.fillMaxSize()) {
        // Top edge shadow cutscene
        Image(
            painter = painterResource(id = R.drawable.sprite_avg_cutscene),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp)
                .align(Alignment.TopCenter)
                .scale(scaleX = 1f, scaleY = -1f)
        )

        // Bottom dialog frame cutscene
        Image(
            painter = painterResource(id = R.drawable.sprite_avg_cutscene),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(182.dp)
                .align(Alignment.BottomCenter)
        )

        // Bottom Content Area (Height = 182dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(182.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
        ) {
            // Speaker Name (Right aligned inside name region)
            if (dialogue.speaker.isNotEmpty()) {
                Text(
                    text = dialogue.speaker,
                    color = Color(0xFF929292),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .offset(x = 20.dp, y = 30.dp)
                        .width(312.dp)
                )
            }

            // Message text
            val messageAnnotated = parseTaggedTextToAnnotated(dialogue.text)
            Text(
                text = messageAnnotated,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                modifier = Modifier
                    .offset(x = 360.dp, y = 34.dp)
                    .width(735.dp)
            )
        }
    }
}