package me.voltual.asp.engine

object StoryAsset {

    const val TORAPPU_ORIGIN = "https://torappu.prts.wiki"

    enum class StoryAssetFamily(val folder: String) {
        BACKGROUND("background"),
        IMAGE("images"),
        CUTIN("cutin")
    }

    fun resolveAssetUrl(raw: String): String = raw

    fun resolveStoryAssetByKey(rawKey: String, family: StoryAssetFamily): String? {
        val key = rawKey.trim().lowercase()
        if (key.isEmpty()) return null
        return "$TORAPPU_ORIGIN/assets/avg/${family.folder}/$key.png"
    }

    fun resolveStoryCharacterAssetByKey(rawKey: String): String? {
        val key = rawKey.trim()
        if (key.isEmpty()) return null
        return "$TORAPPU_ORIGIN/assets/avg/characters/${encodePathSegments(key)}.png"
    }

    fun resolveStoryVideoByKey(rawKey: String): String? {
        val key = rawKey.trim().replace(Regex("^/+"), "")
        if (key.isEmpty()) return null
        if (key.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE))) return key
        return "$TORAPPU_ORIGIN/assets/${encodePathSegments(key.lowercase())}"
    }

    fun resolveStoryAudioByKey(rawKey: String, variables: Map<String, Any>? = null): String? {
        val key = rawKey.trim()
        if (key.isEmpty()) return null

        if (key.startsWith("$")) {
            if (variables == null) return null
            val variableKey = key.substring(1).lowercase()
            val variableValue = variables[variableKey] as? String ?: return null
            return audioUrlFromValue(variableValue)
        }

        if (key.startsWith("@")) return "$TORAPPU_ORIGIN/assets/${key.substring(1)}"

        if (key.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE))) return key

        return audioUrlFromValue(key)
    }

    private fun audioUrlFromValue(value: String): String {
        val assetsBase = "$TORAPPU_ORIGIN/assets/"
        return "${value.lowercase().replace("sound_beta_2", "${assetsBase}audio")}.mp3"
    }

    private fun encodePathSegments(path: String): String {
        return path.split("/").joinToString("/") { 
            java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") 
        }
    }
}