package me.voltual.asp.engine

object StoryParser {

    private val COMMAND_REGEX = Regex("^\\[\\s*(?:(.*?)\\((.*)\\)|(?:([.|\\w]*)|(.*)))\\s*\\]\\s*(.*)")
    private val DIALOGUE_INNER_REGEX = Regex("^name\\s*=\\s*\"((?:\\\\.|[^\"])*)\"$")

    private val FIT_MODE_BY_VALUE = mapOf(
        0 to "DEFAULT",
        1 to "BLACK_MASK"
    )

    private val CHARACTER_SORT_TYPE_BY_VALUE = mapOf(
        0 to "BY_LEVEL_UP", 1 to "BY_LEVEL_DOWN", 2 to "BY_RARITY_UP", 3 to "BY_RARITY_DOWN",
        4 to "BY_GAIN_TIME_UP", 5 to "BY_GAIN_TIME_DOWN", 6 to "BY_NAME_UP", 7 to "BY_NAME_DOWN",
        8 to "BY_COST_UP", 9 to "BY_COST_DOWN", 10 to "BY_HP_UP", 11 to "BY_HP_DOWN",
        12 to "BY_ATK_UP", 13 to "BY_ATK_DOWN", 14 to "BY_DEF_UP", 15 to "BY_DEF_DOWN",
        16 to "BY_RES_UP", 17 to "BY_RES_DOWN", 18 to "BY_FAVOR_UP", 19 to "BY_FAVOR_DOWN",
        20 to "BY_RESPAWN_UP", 21 to "BY_RESPAWN_DOWN", 22 to "BY_BLOCKNUM_UP", 23 to "BY_BLOCKNUM_DOWN",
        24 to "BY_ATKSPEED_UP", 25 to "BY_ATKSPEED_DOWN", 26 to "BY_HANDBOOKSTAGE_UP", 27 to "BY_HANDBOOKSTAGE_DOWN"
    )

    private fun splitArgs(raw: String): List<String> {
        val output = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var quote: Char? = null
        var escaped = false

        for (char in raw) {
            if (escaped) {
                current.append(char)
                escaped = false
                continue
            }
            if (char == '\\' && quote != null) {
                current.append(char)
                escaped = true
                continue
            }
            if (quote != null) {
                current.append(char)
                if (char == quote) quote = null
                continue
            }
            if (char == '"' || char == '\'') {
                quote = char
                current.append(char)
                continue
            }
            if (char == '(') {
                depth += 1
            } else if (char == ')') {
                depth = Math.max(0, depth - 1)
            } else if (char == ',' && depth == 0) {
                if (current.isNotBlank()) output.add(current.toString().trim())
                current.clear()
                continue
            }
            current.append(char)
        }

        if (current.isNotBlank()) output.add(current.toString().trim())
        return output
    }

    private fun unescapeQuoted(value: String): String {
        val quote = value[0]
        val body = value.substring(1, value.length - 1)
        return body.replace(Regex("\\\\(.)")) { matchResult ->
            when (val escaped = matchResult.groupValues[1]) {
                "n" -> "\n"
                "r" -> "\r"
                "t" -> "\t"
                quote.toString(), "\\" -> escaped
                else -> "\\$escaped"
            }
        }
    }

    private fun parseValue(raw: String): Any {
        val value = raw.trim()
        if (value.length >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return unescapeQuoted(value)
        }
        if (value == "true") return true
        if (value == "false") return false
        if (value.matches(Regex("^-?\\d+(?:\\.\\d+)?$"))) {
            return if (value.contains(".")) value.toFloat() else value.toInt()
        }
        return value
    }

    private fun parseArgs(raw: String): Map<String, Any> {
        val args = mutableMapOf<String, Any>()
        for (part in splitArgs(raw)) {
            val separator = part.indexOf("=")
            if (separator == -1) continue
            val key = part.substring(0, separator).trim()
            if (key.isNotEmpty()) {
                args[key] = parseValue(part.substring(separator + 1))
            }
        }
        return args
    }

    private fun parseDialogueSpeaker(inner: String): String? {
        val match = DIALOGUE_INNER_REGEX.find(inner)
        return match?.let { unescapeQuoted("\"${it.groupValues[1]}\"") }
    }

    fun parseLine(raw: String, lineNumber: Int = 1): ParsedLine {
        val match = COMMAND_REGEX.find(raw)
        if (match != null) {
            val parenthesizedName = match.groups[1]?.value
            val parenthesizedArgs = match.groups[2]?.value
            val bareName = match.groups[3]?.value
            val bareFallback = match.groups[4]?.value
            val content = match.groups[5]?.value ?: ""
            val speaker = if (!bareFallback.isNullOrBlank()) parseDialogueSpeaker(bareFallback.trim()) else null

            if (speaker != null) {
                return ParsedDialogueLine(
                    lineNumber = lineNumber,
                    raw = raw,
                    speaker = speaker,
                    text = content
                )
            }

            val commandName = (bareName ?: parenthesizedName ?: "dialog").trim().lowercase()
            if (commandName.isNotEmpty()) {
                return ParsedCommandLine(
                    lineNumber = lineNumber,
                    raw = raw,
                    command = commandName,
                    args = parseArgs(parenthesizedArgs ?: bareFallback ?: ""),
                    content = content,
                    paramPresent = parenthesizedArgs != null || !bareFallback.isNullOrBlank(),
                    trailingText = content
                )
            }
        }

        return ParsedNarrationLine(
            lineNumber = lineNumber,
            raw = raw,
            text = raw
        )
    }

    private data class LogicalLine(val lineNumber: Int, val raw: String)

    private fun logicalLines(source: String): List<LogicalLine> {
        val physicalLines = source.replace(Regex("\\r\\n?"), "\n").split("\n")
        val output = mutableListOf<LogicalLine>()

        var index = 0
        while (index < physicalLines.size) {
            val lineNumber = index + 1
            var raw = physicalLines[index]
            while (raw.endsWith("\\") && index + 1 < physicalLines.size) {
                raw = raw.dropLast(1) + physicalLines[index + 1]
                index += 1
            }
            if (raw.isNotBlank() && !raw.trimStart().startsWith("//")) {
                output.add(LogicalLine(lineNumber, raw))
            }
            index++
        }
        return output
    }

    fun parseScript(source: String): List<ParsedLine> {
        val sourceLines = logicalLines(source)
        val lines = sourceLines.map { parseLine(it.raw, it.lineNumber) }.toMutableList()
        
        if (lines.isNotEmpty()) {
            val last = lines.last()
            if (!(last is ParsedCommandLine && last.command == "endtip")) {
                val lineNumber = sourceLines.lastOrNull()?.lineNumber ?: 1
                lines.add(
                    ParsedCommandLine(
                        lineNumber = lineNumber,
                        raw = "[endtip(block=true)]",
                        command = "endtip",
                        args = mapOf("block" to true),
                        content = "",
                        paramPresent = true,
                        trailingText = ""
                    )
                )
            }
        }
        return lines
    }

    private fun resolveEnumName(value: Any?, byValue: Map<Int, String>, fallback: String): String {
        val raw = value?.toString() ?: ""
        if (raw.isEmpty()) return fallback
        val trimmed = raw.trim()
        if (trimmed.matches(Regex("^[-+]?\\d+$"))) {
            return byValue[trimmed.toInt()] ?: fallback
        }
        val name = trimmed.uppercase()
        return if (byValue.values.contains(name)) name else fallback
    }

    data class ParseStoryResult(val lines: List<ParsedLine>, val metadata: StoryMetadata)

    fun parseStory(source: String): ParseStoryResult {
        val lines = parseScript(source)
        val first = lines.firstOrNull()
        val header = if (first is ParsedCommandLine && first.command == "header") first else null
        
        val headerArgs = header?.args ?: emptyMap()
        
        val fitMode = resolveEnumName(headerArgs["fit_mode"], FIT_MODE_BY_VALUE, "DEFAULT")
        val isTutorial = headerArgs["is_tutorial"] == true
        val isVideoOnly = headerArgs["is_video_only"] == true
        val autoable = headerArgs["is_autoable"] as? Boolean
        val skippable = headerArgs["is_skippable"] as? Boolean

        val metadata = StoryMetadata(
            args = headerArgs,
            characterSortType = resolveEnumName(headerArgs["char_sort_type"], CHARACTER_SORT_TYPE_BY_VALUE, "BY_GAIN_TIME_DOWN"),
            denyAutoSwitchScene = headerArgs["deny_auto_switch_scene"] == true,
            dontClearGameObjectPoolOnStart = headerArgs["dont_clear_gameobjectpool_onstart"] == true,
            fitMode = if (fitMode == "BLACK_MASK") "BLACK_MASK" else "DEFAULT",
            id = (headerArgs["key"]?.toString() ?: ""),
            isAutoable = autoable ?: (!isTutorial && !isVideoOnly),
            isSkippable = skippable ?: !isTutorial,
            isTutorial = isTutorial,
            isVideoOnly = isVideoOnly,
            title = header?.content ?: ""
        )

        return ParseStoryResult(lines, metadata)
    }
}