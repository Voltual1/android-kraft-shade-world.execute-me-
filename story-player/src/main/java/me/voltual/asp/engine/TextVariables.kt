package me.voltual.asp.engine

private val TEXT_VARIABLE_RE = Regex("""\{@([a-zA-Z0-9_.]+)\}""")
private val BUILTIN_TEXT_VARIABLES = mapOf<String, Any>(
    "nbs" to "\u00A0"
)

var globalStoryNickname: String = "博士"

fun getStoryNickname(): String = globalStoryNickname

fun expandStoryText(
    text: String,
    variables: Map<String, Any?> = emptyMap()
): String {
    return TEXT_VARIABLE_RE.replace(text) { matchResult ->
        val rawName = matchResult.groupValues[1]
        val name = rawName.lowercase()
        if (name == "nickname") {
            getStoryNickname()
        } else {
            val value = variables[name] ?: BUILTIN_TEXT_VARIABLES[name]
            value?.toString() ?: ""
        }
    }
}