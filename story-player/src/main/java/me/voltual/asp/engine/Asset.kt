package me.voltual.asp.engine

import java.net.URLEncoder

const val TORAPPU_ORIGIN: String = "https://torappu.prts.wiki"

/** 可选的本地或自定义资源起点（例如 "file:///android_asset/story/W2G_BEG"） */
var customAssetOrigin: String? = null

fun getAssetOrigin(): String = customAssetOrigin ?: TORAPPU_ORIGIN

enum class StoryAssetFamily(val folder: String) {
    BACKGROUND("background"),
    IMAGE("images"),
    CUTIN("cutin")
}

fun normalizeImageKey(rawKey: String): String = rawKey.trim().lowercase()

fun encodePathSegments(path: String): String {
    return path.split("/").joinToString("/") { part ->
        URLEncoder.encode(part, "UTF-8").replace("+", "%20")
    }
}

/**
 * 将音频 key 或变量值解析为完整的音频文件路径：
 * 1. 若以 sound_beta_2 开头，替换为 assets/audio；
 * 2. 若是未带目录的短音效名（如 d_gen_xxx, b_char_xxx），自动匹配到对应的子目录。
 */
private fun audioUrlFromValue(value: String): String {
    val origin = getAssetOrigin()
    val assetsBase = "$origin/assets/"
    val lower = value.lowercase()

    if (lower.contains("sound_beta_2")) {
        val replaced = lower.replace("sound_beta_2", "${assetsBase}audio")
        return "$replaced.mp3"
    }

    // 处理短音效路径映射（对齐方舟内部音频包分类）
    val subPath = when {
        lower.startsWith("d_gen_") || lower.startsWith("d_sp_") -> "avg/$lower"
        lower.startsWith("b_char_") -> "battle/b_char/$lower"
        lower.startsWith("d_avg_") -> "dialog/$lower"
        lower.startsWith("e_atk_") -> "enemy/e_atk/$lower"
        lower.startsWith("g_ui_") -> "general/g_ui/$lower"
        lower.contains('/') -> lower
        else -> "avg/$lower"
    }

    return "${assetsBase}audio/$subPath.mp3"
}

private fun normalizeAssetPath(rawPath: String): String {
    return rawPath.trim().replace(Regex("^/+"), "")
}

fun resolveAssetUrl(raw: String): String = raw

fun resolveStoryAssetByKey(rawKey: String, family: StoryAssetFamily): String? {
    val key = normalizeImageKey(rawKey)
    if (key.isEmpty()) return null
    val origin = getAssetOrigin()
    return "$origin/assets/avg/${family.folder}/$key.png"
}

fun resolveStoryCharacterAssetByKey(rawKey: String): String? {
    val key = rawKey.trim()
    if (key.isEmpty()) return null
    val origin = getAssetOrigin()
    return "$origin/assets/avg/characters/${encodePathSegments(key)}.png"
}

fun resolveStoryVideoByKey(rawKey: String): String? {
    val key = normalizeAssetPath(rawKey)
    if (key.isEmpty()) return null
    if (key.startsWith("http://", ignoreCase = true) ||
        key.startsWith("https://", ignoreCase = true) ||
        key.startsWith("file://", ignoreCase = true)
    ) {
        return key
    }
    val origin = getAssetOrigin()
    return "$origin/assets/${encodePathSegments(key.lowercase())}"
}

fun resolveStoryAudioByKey(
    rawKey: String,
    variables: Map<String, Any?>? = null
): String? {
    val key = rawKey.trim()
    if (key.isEmpty()) return null

    if (key.startsWith("$")) {
        if (variables == null) return null
        val variableKey = key.substring(1).lowercase()
        val variableValue = variables[variableKey] as? String ?: return null
        return audioUrlFromValue(variableValue)
    }

    if (key.startsWith("@")) {
        val origin = getAssetOrigin()
        return "$origin/assets/${key.substring(1)}"
    }

    if (key.startsWith("http://", ignoreCase = true) ||
        key.startsWith("https://", ignoreCase = true) ||
        key.startsWith("file://", ignoreCase = true)
    ) {
        return key
    }

    return audioUrlFromValue(key)
}