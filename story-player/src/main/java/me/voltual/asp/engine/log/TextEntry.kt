package me.voltual.asp.engine.log

import me.voltual.asp.engine.expandStoryText
import me.voltual.asp.engine.parseRichChars

fun toSpans(
    text: String,
    variables: Map<String, Any?>
): List<LogTextSpan> {
    val chars = parseRichChars(expandStoryText(text, variables))
    val spans = mutableListOf<LogTextSpan>()
    var current: LogTextSpan? = null

    for (richChar in chars) {
        val sameFormat = current != null &&
                current.color == richChar.color &&
                current.bold == richChar.bold &&
                current.italic == richChar.italic

        if (!sameFormat) {
            current = LogTextSpan(
                text = "",
                color = richChar.color,
                bold = richChar.bold,
                italic = richChar.italic
            )
            spans.add(current)
        }
        current = spans.last().copy(text = spans.last().text + richChar.char)
        spans[spans.lastIndex] = current
    }
    return spans
}

fun buildLineEntry(
    lineIndex: Int,
    speaker: String,
    text: String,
    source: LogLineSource,
    variables: Map<String, Any?>
): LogLineEntry {
    return LogLineEntry(
        lineIndex = lineIndex,
        speaker = expandStoryText(speaker, variables),
        spans = toSpans(text, variables),
        source = source
    )
}

fun entryContentKey(entry: LogLineEntry): String {
    val spans = entry.spans.joinToString("\u0001") { span ->
        val color = span.color ?: ""
        val b = if (span.bold) "b" else ""
        val i = if (span.italic) "i" else ""
        "$color$b$i\u0003${span.text}"
    }
    return "${entry.lineIndex}\u0002${entry.speaker}\u0002${entry.source}\u0002$spans"
}