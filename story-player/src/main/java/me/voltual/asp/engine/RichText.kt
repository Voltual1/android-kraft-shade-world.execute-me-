package me.voltual.asp.engine

data class RichChar(
    val char: Char,
    val color: String?,
    val bold: Boolean,
    val italic: Boolean
)

data class RichTagStyle(
    val fill: String? = null,
    val fontStyle: String? = null,
    val fontWeight: String? = null
)

typealias RichTagStyles = Map<String, RichTagStyle>

private enum class RichTagKind { B, COLOR, I }

private data class RichTag(
    val close: Boolean,
    val end: Int,
    val kind: RichTagKind,
    val start: Int,
    val value: String?
)

private val RICH_TAG_RE = Regex("""<(?:(b|i)|color=(#[^<>]+)|/(b|i|color))>""", RegexOption.IGNORE_CASE)

private fun scanRichTags(text: String): List<RichTag> {
    return RICH_TAG_RE.findAll(text).map { match ->
        val fullMatch = match.value
        val styleOpen = match.groups[1]?.value
        val colorValue = match.groups[2]?.value
        val closeKind = match.groups[3]?.value
        val kindStr = (styleOpen ?: closeKind ?: "color").lowercase()
        val kind = when (kindStr) {
            "b" -> RichTagKind.B
            "i" -> RichTagKind.I
            else -> RichTagKind.COLOR
        }
        RichTag(
            close = closeKind != null,
            end = match.range.first + fullMatch.length,
            kind = kind,
            start = match.range.first,
            value = colorValue
        )
    }.toList()
}

fun parseRichChars(text: String): List<RichChar> {
    val tags = scanRichTags(text)
    val paired = mutableSetOf<RichTag>()
    val open = mutableListOf<RichTag>()

    for (tag in tags) {
        if (!tag.close) {
            open.add(tag)
            continue
        }
        for (i in open.indices.reversed()) {
            if (open[i].kind == tag.kind) {
                paired.add(open[i])
                paired.add(tag)
                open.removeAt(i)
                break
            }
        }
    }

    val chars = mutableListOf<RichChar>()
    val active = mutableListOf<RichTag>()

    fun pushText(segment: String) {
        val color = active.findLast { it.kind == RichTagKind.COLOR }?.value
        val bold = active.any { it.kind == RichTagKind.B }
        val italic = active.any { it.kind == RichTagKind.I }
        for (ch in segment) {
            chars.add(RichChar(char = ch, color = color, bold = bold, italic = italic))
        }
    }

    var cursor = 0
    for (tag in tags) {
        if (!paired.contains(tag)) continue
        pushText(text.substring(cursor, tag.start))
        cursor = tag.end
        if (!tag.close) {
            active.add(tag)
        } else {
            val idx = active.indexOfLast { it.kind == tag.kind }
            if (idx != -1) active.removeAt(idx)
        }
    }
    pushText(text.substring(cursor))
    return chars
}

fun colorTagName(color: String): String = "_c" + color.replace("#", "")

private const val BOLD_TAG = "_b"
private const val ITALIC_TAG = "_i"

private fun runTags(char: RichChar): List<String> {
    val tags = mutableListOf<String>()
    if (char.bold) tags.add(BOLD_TAG)
    if (char.italic) tags.add(ITALIC_TAG)
    if (char.color != null) tags.add(colorTagName(char.color))
    return tags
}

fun richCharsToTaggedText(chars: List<RichChar>): String {
    val parts = StringBuilder()
    var runKey: String? = null
    var runClosers = ""

    for (char in chars) {
        val tags = runTags(char)
        val key = tags.joinToString(" ")
        if (key != runKey) {
            parts.append(runClosers)
            parts.append(tags.joinToString("") { "<$it>" })
            runClosers = tags.reversed().joinToString("") { "</$it>" }
            runKey = key
        }
        parts.append(char.char)
    }
    parts.append(runClosers)
    return parts.toString()
}

fun buildTagStyles(chars: List<RichChar>): RichTagStyles {
    val styles = mutableMapOf<String, RichTagStyle>()
    for (char in chars) {
        if (char.bold) styles[BOLD_TAG] = RichTagStyle(fontWeight = "bold")
        if (char.italic) styles[ITALIC_TAG] = RichTagStyle(fontStyle = "italic")
        if (char.color != null) styles[colorTagName(char.color)] = RichTagStyle(fill = char.color)
    }
    return styles
}