package me.voltual.asp.engine

object StorySplitContent {

    private val SPLIT_REGEX = Regex("<p=(\\d+)>([\\s\\S]*?)</>", RegexOption.IGNORE_CASE)

    fun parse(content: String): Pair<String, String> {
        val map = mutableMapOf<Int, String>()
        for (match in SPLIT_REGEX.findAll(content)) {
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val text = match.groupValues[2]
            map[index] = text
        }
        val mainText = map[1] ?: content.replace(Regex("<[/]?[^>]+>"), "")
        val subText = map[2] ?: ""
        return Pair(mainText, subText)
    }
}