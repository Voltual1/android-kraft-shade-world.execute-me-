package me.voltual.asp.engine.rendering

private val SPELL_SPLIT_REGEX = Regex("""<p=(\d+)>([\s\S]*?)</>""", RegexOption.IGNORE_CASE)

fun parseSpellStickerContent(content: String): Map<Int, String> {
    val parts = mutableMapOf<Int, String>()
    if (content.isEmpty()) return parts

    val unescaped = content.replace("\\n", "\n")
    for (match in SPELL_SPLIT_REGEX.findAll(unescaped)) {
        val id = match.groupValues[1].toIntOrNull() ?: continue
        val text = match.groupValues[2].replace(Regex("""<[^>]+>"""), "")
        parts[id] = text
    }
    return parts
}