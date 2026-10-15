package me.voltual.asp.engine

private val COMMAND_REGEX = Regex("""^\[\s*(?:(.*?)\((.*)\)|(?:([.|\w]*)|(.*)))\s*\]\s*(.*)""")
private val DIALOGUE_INNER_REGEX = Regex("""^name\s*=\s*"((?:\\.|[^"])*)"$""")

fun splitArgs(raw: String): List<String> {
    val output = mutableListOf<String>()
    var current = StringBuilder()
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
            depth = maxOf(0, depth - 1)
        } else if (char == ',' && depth == 0) {
            val trimmed = current.toString().trim()
            if (trimmed.isNotEmpty()) output.add(trimmed)
            current = StringBuilder()
            continue
        }
        current.append(char)
    }

    val finalTrimmed = current.toString().trim()
    if (finalTrimmed.isNotEmpty()) output.add(finalTrimmed)
    return output
}

fun unescapeQuoted(value: String): String {
    val quote = value.first()
    val body = value.substring(1, value.length - 1)
    val sb = StringBuilder()
    var i = 0
    while (i < body.length) {
        val c = body[i]
        if (c == '\\' && i + 1 < body.length) {
            val next = body[i + 1]
            when (next) {
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                quote, '\\' -> sb.append(next)
                else -> {
                    sb.append('\\')
                    sb.append(next)
                }
            }
            i += 2
        } else {
            sb.append(c)
            i += 1
        }
    }
    return sb.toString()
}

fun parseValue(raw: String): StoryCommandValue {
    val value = raw.trim()
    if (value.length >= 2 &&
        ((value.startsWith('"') && value.endsWith('"')) ||
         (value.startsWith('\'') && value.endsWith('\'')))
    ) {
        return unescapeQuoted(value)
    }
    if (value.equals("true", ignoreCase = true)) return true
    if (value.equals("false", ignoreCase = true)) return false
    val num = value.toDoubleOrNull()
    if (num != null) {
        return if (value.contains('.') || value.contains('e', true)) num else value.toIntOrNull() ?: num
    }
    return value
}

fun parseArgs(raw: String): StoryCommandArgs {
    val args = mutableMapOf<String, StoryCommandValue>()
    for (part in splitArgs(raw)) {
        val separator = part.indexOf('=')
        if (separator == -1) continue
        val key = part.substring(0, separator).trim()
        if (key.isNotEmpty()) {
            args[key] = parseValue(part.substring(separator + 1))
        }
    }
    return args
}

fun parseDialogueSpeaker(inner: String): String? {
    val match = DIALOGUE_INNER_REGEX.find(inner) ?: return null
    val speakerGroup = match.groupValues[1]
    return unescapeQuoted("\"$speakerGroup\"")
}

fun parseLine(raw: String, lineNumber: Int = 1): ParsedLine {
    val match = COMMAND_REGEX.find(raw)
    if (match != null) {
        val parenthesizedName = match.groupValues[1].takeIf { it.isNotEmpty() }
        val parenthesizedArgs = match.groupValues[2].takeIf { it.isNotEmpty() }
        val bareName = match.groupValues[3].takeIf { it.isNotEmpty() }
        val bareFallback = match.groupValues[4].takeIf { it.isNotEmpty() }
        val content = match.groupValues[5]

        val speaker = bareFallback?.trim()?.let { parseDialogueSpeaker(it) }
        if (speaker != null) {
            return ParsedLine.Dialogue(
                lineNumber = lineNumber,
                raw = raw,
                speaker = speaker,
                text = content
            )
        }

        val commandCandidate = (bareName ?: parenthesizedName ?: "dialog").trim().lowercase()
        if (commandCandidate.isNotEmpty()) {
            val argsRaw = parenthesizedArgs ?: bareFallback ?: ""
            return ParsedLine.Command(
                lineNumber = lineNumber,
                raw = raw,
                command = commandCandidate,
                args = parseArgs(argsRaw),
                content = content,
                paramPresent = parenthesizedArgs != null || !bareFallback.isNullOrEmpty(),
                trailingText = content
            )
        }
    }

    return ParsedLine.Narration(
        lineNumber = lineNumber,
        raw = raw,
        text = raw
    )
}

data class LogicalLine(val lineNumber: Int, val raw: String)

fun logicalLines(source: String): List<LogicalLine> {
    val physicalLines = source.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    val output = mutableListOf<LogicalLine>()

    var index = 0
    while (index < physicalLines.size) {
        val lineNumber = index + 1
        var raw = physicalLines[index]
        while (raw.endsWith("\\") && index + 1 < physicalLines.size) {
            raw = raw.dropLast(1) + physicalLines[index + 1]
            index += 1
        }
        if (raw.isNotEmpty() && !raw.all { it.isWhitespace() } && !raw.trimStart().startsWith("//")) {
            output.add(LogicalLine(lineNumber, raw))
        }
        index += 1
    }
    return output
}

fun parseScript(source: String): List<ParsedLine> {
    val sourceLines = logicalLines(source)
    val lines = sourceLines.map { parseLine(it.raw, it.lineNumber) }.toMutableList()
    if (lines.isNotEmpty()) {
        val last = lines.lastOrNull()
        if (last !is ParsedLine.Command || last.command != "endtip") {
            val lineNumber = sourceLines.lastOrNull()?.lineNumber ?: 1
            lines.add(
                ParsedLine.Command(
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

private val FIT_MODE_BY_VALUE = mapOf(
    0 to "DEFAULT",
    1 to "BLACK_MASK"
)

private val CHARACTER_SORT_TYPE_BY_VALUE = mapOf(
    0 to "BY_LEVEL_UP",
    1 to "BY_LEVEL_DOWN",
    2 to "BY_RARITY_UP",
    3 to "BY_RARITY_DOWN",
    4 to "BY_GAIN_TIME_UP",
    5 to "BY_GAIN_TIME_DOWN",
    6 to "BY_NAME_UP",
    7 to "BY_NAME_DOWN",
    8 to "BY_COST_UP",
    9 to "BY_COST_DOWN",
    10 to "BY_HP_UP",
    11 to "BY_HP_DOWN",
    12 to "BY_ATK_UP",
    13 to "BY_ATK_DOWN",
    14 to "BY_DEF_UP",
    15 to "BY_DEF_DOWN",
    16 to "BY_RES_UP",
    17 to "BY_RES_DOWN",
    18 to "BY_FAVOR_UP",
    19 to "BY_FAVOR_DOWN",
    20 to "BY_RESPAWN_UP",
    21 to "BY_RESPAWN_DOWN",
    22 to "BY_BLOCKNUM_UP",
    23 to "BY_BLOCKNUM_DOWN",
    24 to "BY_ATKSPEED_UP",
    25 to "BY_ATKSPEED_DOWN",
    26 to "BY_HANDBOOKSTAGE_UP",
    27 to "BY_HANDBOOKSTAGE_DOWN"
)

private fun resolveEnumName(value: StoryCommandValue?, byValue: Map<Int, String>, fallback: String): String {
    if (value == null) return fallback
    val raw = value.toString().trim()
    if (raw.isEmpty()) return fallback
    val intVal = raw.toIntOrNull()
    if (intVal != null) {
        return byValue[intVal] ?: fallback
    }
    val upper = raw.uppercase()
    return if (byValue.values.contains(upper)) upper else fallback
}

fun parseStory(source: String): Pair<List<ParsedLine>, StoryMetadata> {
    val lines = parseScript(source)
    val first = lines.firstOrNull()
    val header = if (first is ParsedLine.Command && first.command == "header") first else null

    val fitMode = resolveEnumName(header?.args?.get("fit_mode"), FIT_MODE_BY_VALUE, "DEFAULT")
    val isTutorial = header?.args?.get("is_tutorial") == true
    val isVideoOnly = header?.args?.get("is_video_only") == true
    val autoable = header?.args?.get("is_autoable") as? Boolean
    val skippable = header?.args?.get("is_skippable") as? Boolean

    val metadata = StoryMetadata(
        args = header?.args ?: emptyMap(),
        characterSortType = resolveEnumName(header?.args?.get("char_sort_type"), CHARACTER_SORT_TYPE_BY_VALUE, "BY_GAIN_TIME_DOWN"),
        denyAutoSwitchScene = header?.args?.get("deny_auto_switch_scene") == true,
        dontClearGameObjectPoolOnStart = header?.args?.get("dont_clear_gameobjectpool_onstart") == true,
        fitMode = if (fitMode == "BLACK_MASK") "BLACK_MASK" else "DEFAULT",
        id = header?.args?.get("key")?.toString() ?: "",
        isAutoable = autoable ?: (!isTutorial && !isVideoOnly),
        isSkippable = skippable ?: (!isTutorial),
        isTutorial = isTutorial,
        isVideoOnly = isVideoOnly,
        title = header?.content ?: ""
    )

    return Pair(lines, metadata)
}