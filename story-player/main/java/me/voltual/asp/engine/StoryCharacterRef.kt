package me.voltual.asp.engine

data class NativeCharacterRef(
    val alias: String?,
    val base: String,
    val group: Int?,
    val index: Int
)

data class CharacterRefSelection(
    val base: String,
    val expression: String
)

object StoryCharacterRef {

    fun nativeCharacterFadeIdentity(ref: String): String {
        val alias = ref.lastIndexOf('@')
        if (alias != -1) return ref.substring(0, alias)

        val index = ref.lastIndexOf('#')
        if (index == -1) return ref
        return ref.substring(0, index)
    }

    private fun isDotNetWhite(text: String, at: Int): Boolean {
        if (at >= text.length) return false
        val code = text[at].code
        return code == 0x20 || (code in 0x09..0x0d)
    }

    fun tryParseInt32(text: String): Int? {
        var i = 0
        while (i < text.length && isDotNetWhite(text, i)) i++

        var negative = false
        if (i < text.length && (text[i] == '+' || text[i] == '-')) {
            negative = text[i] == '-'
            i++
        }

        val digitsStart = i
        while (i < text.length && text[i] in '0'..'9') i++
        if (i == digitsStart) return null

        val magnitudeStr = text.substring(digitsStart, i)
        val magnitude = magnitudeStr.toLongOrNull() ?: return null

        while (i < text.length && isDotNetWhite(text, i)) i++
        if (i != text.length) return null

        val value = if (negative) -magnitude else magnitude
        if (value < -2147483648L || value > 2147483647L) return null
        return value.toInt()
    }

    fun parseNativeCharacterRef(ref: String?): NativeCharacterRef? {
        if (ref.isNullOrBlank()) return null

        var value = ref
        var group: Int? = null

        val dollar = value.lastIndexOf('$')
        if (dollar != -1) {
            val parsed = tryParseInt32(value.substring(dollar + 1))
            if (parsed != null) {
                group = parsed - 1
                value = value.substring(0, dollar)
            }
        }

        val at = value.lastIndexOf('@')
        if (at != -1) {
            return NativeCharacterRef(
                alias = value.substring(at),
                base = value.substring(0, at),
                group = group,
                index = 0
            )
        }

        var index = 0
        val hash = value.lastIndexOf('#')
        if (hash != -1) {
            val parsed = tryParseInt32(value.substring(hash + 1))
            if (parsed != null) {
                index = parsed - 1
                value = value.substring(0, hash)
            }
        }

        return NativeCharacterRef(
            alias = null,
            base = value,
            group = group,
            index = index
        )
    }

    fun resolveCharacterSelection(
        linkMap: Map<String, StoryLinkNode>,
        rawRef: String?
    ): CharacterRefSelection? {
        if (rawRef.isNullOrBlank()) return null
        val normalized = rawRef.lowercase()

        val directLink = linkMap[normalized]
        if (directLink?.array?.firstOrNull()?.name != null) {
            return CharacterRefSelection(base = normalized, expression = directLink.array[0].name)
        }

        for ((base, link) in linkMap) {
            val prefix = "$base-"
            if (!normalized.startsWith(prefix)) continue

            val expression = normalized.substring(prefix.length)
            if (expression.isEmpty()) continue

            val found = link.array.find { it.name.lowercase() == expression }
            if (found != null) return CharacterRefSelection(base = base, expression = found.name)
        }

        val parsed = parseNativeCharacterRef(normalized) ?: return null
        val link = linkMap[parsed.base] ?: return null

        if (parsed.alias != null) {
            val wanted = parsed.alias.substring(1)
            val found = link.array.find { it.alias.lowercase() == wanted }
            val expression = found?.name ?: link.array.firstOrNull()?.name
            return if (expression != null) CharacterRefSelection(parsed.base, expression) else null
        }

        if (parsed.group != null) {
            val suffix = "$${parsed.group + 1}"
            val grouped = link.array.filter { it.name.endsWith(suffix) }
            if (grouped.isEmpty()) return null
            val picked = grouped.getOrNull(Math.max(0, parsed.index)) ?: grouped.firstOrNull()
            return if (picked?.name != null) CharacterRefSelection(parsed.base, picked.name) else null
        }

        val entry = link.array.getOrNull(Math.max(0, parsed.index)) ?: link.array.firstOrNull()
        return if (entry?.name != null) CharacterRefSelection(parsed.base, entry.name) else null
    }
}