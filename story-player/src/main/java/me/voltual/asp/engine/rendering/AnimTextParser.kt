package me.voltual.asp.engine.rendering

private val SPLIT_TAG_REGEX = Regex("""<p=(\d+)>([\s\S]*?)</>""", RegexOption.IGNORE_CASE)

data class AnimTextParsedContent(
    val mainText: String = "",
    val subText: String = ""
)

fun parseAnimTextContent(content: String): AnimTextParsedContent {
    if (content.isEmpty()) return AnimTextParsedContent()

    val unescaped = content.replace("\\n", "\n")
    val matches = SPLIT_TAG_REGEX.findAll(unescaped).toList()
    if (matches.isEmpty()) {
        return AnimTextParsedContent(mainText = unescaped, subText = "")
    }

    var main = ""
    var sub = ""

    for (match in matches) {
        val id = match.groupValues[1].toIntOrNull() ?: continue
        val innerContent = match.groupValues[2]
        // Strip other inner rich text tags for clean stamp display
        val cleanText = innerContent.replace(Regex("""<[^>]+>"""), "")
        if (id == 1) {
            main = cleanText
        } else if (id == 2) {
            sub = cleanText
        }
    }

    return AnimTextParsedContent(mainText = main, subText = sub)
}