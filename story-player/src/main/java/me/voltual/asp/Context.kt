package me.voltual.asp

import android.content.Context as AndroidContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import me.voltual.asp.engine.*
import java.net.HttpURLConnection
import java.net.URL

private const val RAW_ASSET_BASE = "https://torappu.prts.wiki/assets"
private const val STORY_BASE = "https://torappu.prts.wiki/gamedata/latest/story"

data class Context(
    val script: List<String>,
    val scriptText: String? = null,
    val storyMetadata: StoryMetadata? = null,
    val audioVariables: Map<String, Any?> = emptyMap(),
    val charMap: Map<String, String>? = null,
    val linkMap: Map<String, StoryLinkNode> = emptyMap(),
    val backgroundPpuMap: Map<String, Float>? = null
)

fun assetUrl(path: String): String {
    val normalized = path.replace(Regex("^/+"), "")
    return resolveAssetUrl("$RAW_ASSET_BASE/$normalized")
}

fun storyUrl(path: String): String {
    val normalized = path.replace(Regex("^/+"), "")
    return "$STORY_BASE/$normalized"
}

private fun jsonToNumber(element: JsonElement?, fallback: Float = 0f): Float {
    if (element == null || element is JsonNull) return fallback
    return element.jsonPrimitive.content.toFloatOrNull() ?: fallback
}

fun normalizeCharacterMap(rootElement: JsonElement): Map<String, StoryLinkNode> {
    val jsonObject = rootElement.jsonObject
    val output = mutableMapOf<String, StoryLinkNode>()

    for ((base, rawNode) in jsonObject) {
        val nodeObj = rawNode.jsonObject
        val posObj = nodeObj["pos"]?.jsonObject
        val sizeObj = nodeObj["size"]?.jsonObject

        val groupsRaw = nodeObj["groups"]?.jsonArray ?: JsonArray(emptyList())
        val groups = groupsRaw.map { rawGroup ->
            val groupObj = rawGroup.jsonObject
            val mode = groupObj["mode"]?.jsonPrimitive?.content ?: "single"
            if (mode == "face_overlay") {
                val baseImg = groupObj["base"]?.jsonPrimitive?.content ?: ""
                val faceRectObj = groupObj["faceRect"]?.jsonObject
                StoryCharacterGroup.FaceOverlay(
                    base = baseImg,
                    faceRect = StoryFaceRect(
                        h = faceRectObj?.get("h")?.jsonPrimitive?.intOrNull ?: 0,
                        w = faceRectObj?.get("w")?.jsonPrimitive?.intOrNull ?: 0,
                        x = faceRectObj?.get("x")?.jsonPrimitive?.intOrNull ?: 0,
                        y = faceRectObj?.get("y")?.jsonPrimitive?.intOrNull ?: 0
                    )
                )
            } else {
                StoryCharacterGroup.Single()
            }
        }

        val arrayRaw = nodeObj["array"]?.jsonArray ?: JsonArray(emptyList())
        val array = mutableListOf<StoryCharacterEntry>()
        for (rawItem in arrayRaw) {
            val itemObj = rawItem.jsonObject
            val name = itemObj["name"]?.jsonPrimitive?.content ?: continue
            val alias = itemObj["alias"]?.jsonPrimitive?.content ?: ""
            val group = itemObj["group"]?.jsonPrimitive?.intOrNull ?: -1

            if (group == -1) {
                array.add(
                    StoryCharacterEntry(
                        alias = alias,
                        group = -1,
                        image = itemObj["image"]?.jsonPrimitive?.content ?: name,
                        name = name
                    )
                )
                continue
            }

            val face = itemObj["face"]?.jsonPrimitive?.content
            if (face != null) {
                array.add(
                    StoryCharacterEntry(
                        alias = alias,
                        face = face,
                        group = group,
                        name = name
                    )
                )
                continue
            }

            val image = itemObj["image"]?.jsonPrimitive?.content
            if (image != null) {
                array.add(
                    StoryCharacterEntry(
                        alias = alias,
                        group = -1,
                        image = image,
                        name = name
                    )
                )
            }
        }

        output[base.lowercase()] = StoryLinkNode(
            array = array,
            groups = groups,
            pos = Vec2(
                x = jsonToNumber(posObj?.get("x"), 0f),
                y = jsonToNumber(posObj?.get("y"), 0f)
            ),
            size = Vec2(
                x = jsonToNumber(sizeObj?.get("x"), 0f),
                y = jsonToNumber(sizeObj?.get("y"), 0f)
            )
        )
    }

    return output
}

private val cachedStoryAudioVariables = mutableMapOf<String, Any?>()
private var storyVariablesLoaded = false

private fun mergeStoryVariables(rootElement: JsonElement) {
    val jsonObject = rootElement.jsonObject
    for ((key, value) in jsonObject) {
        val primitive = value.jsonPrimitive
        val content = if (primitive.isString) primitive.content else primitive.content
        cachedStoryAudioVariables[key.lowercase()] = content
    }
}

suspend fun ensureStoryVariables(): Map<String, Any?> = withContext(Dispatchers.IO) {
    if (!storyVariablesLoaded) {
        try {
            val url = URL(storyUrl("story_variables.json"))
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (connection.responseCode in 200..299) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = Json.parseToJsonElement(text)
                mergeStoryVariables(parsed)
                storyVariablesLoaded = true
            }
        } catch (_: Exception) {
            // Keep runtime resilient
        }
    }
    cachedStoryAudioVariables.toMap()
}

fun resolveStoryScriptUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    return storyUrl(trimmed)
}

suspend fun fetchStoryCharacterMap(): Map<String, StoryLinkNode> = withContext(Dispatchers.IO) {
    val url = URL(assetUrl("avg/character.json"))
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 15_000
    }
    if (connection.responseCode !in 200..299) {
        throw IllegalStateException("failed to fetch avg/character.json: HTTP ${connection.responseCode}")
    }
    val text = connection.inputStream.bufferedReader().use { it.readText() }
    val parsed = Json.parseToJsonElement(text)
    normalizeCharacterMap(parsed)
}

fun normalizeBackgroundPpuMap(rootElement: JsonElement): Map<String, Float> {
    val jsonObject = rootElement.jsonObject
    val output = mutableMapOf<String, Float>()
    for ((key, value) in jsonObject) {
        val ppu = value.jsonPrimitive.content.toFloatOrNull()
        if (ppu != null && ppu > 0f) {
            output[key.lowercase()] = ppu
        }
    }
    return output
}

suspend fun fetchBackgroundPpuMap(): Map<String, Float> = withContext(Dispatchers.IO) {
    try {
        val url = URL(assetUrl("avg/background.json"))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        if (connection.responseCode !in 200..299) return@withContext emptyMap()
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        val parsed = Json.parseToJsonElement(text)
        normalizeBackgroundPpuMap(parsed)
    } catch (_: Exception) {
        emptyMap()
    }
}

suspend fun loadContextByScript(scriptText: String): Context = coroutineScope {
    val audioVarsDeferred = async { ensureStoryVariables() }
    val linkMapDeferred = async { fetchStoryCharacterMap() }
    val bgPpuDeferred = async { fetchBackgroundPpuMap() }

    val audioVariables = audioVarsDeferred.await()
    val linkMap = linkMapDeferred.await()
    val backgroundPpuMap = bgPpuDeferred.await()

    val (_, metadata) = parseStory(scriptText)

    Context(
        script = scriptText.replace("\r\n", "\n").replace("\r", "\n").split("\n"),
        scriptText = scriptText,
        storyMetadata = metadata,
        audioVariables = audioVariables,
        linkMap = linkMap,
        backgroundPpuMap = backgroundPpuMap
    )
}

/**
 * 直接从本地 assets 目录（如 "story/W2G_BEG"）读取并组装完整的剧情 Context，
 * 支持完全离线播放。
 */
suspend fun loadContextFromAssets(
    androidContext: AndroidContext,
    storyDir: String = "story/W2G_BEG"
): Pair<String, Context> = withContext(Dispatchers.IO) {
    val assetManager = androidContext.assets
    val cleanDir = storyDir.trim().removePrefix("/").removeSuffix("/")

    // 1. 设置自定义本地静态资产根目录
    customAssetOrigin = "file:///android_asset/$cleanDir"

    // 2. 读取剧本 story.txt
    val scriptText = assetManager.open("$cleanDir/story.txt").bufferedReader().use { it.readText() }

    // 3. 读取并解析 character.json
    val linkMap = try {
        val charJsonText = assetManager.open("$cleanDir/character.json").bufferedReader().use { it.readText() }
        normalizeCharacterMap(Json.parseToJsonElement(charJsonText))
    } catch (_: Throwable) {
        emptyMap()
    }

    // 4. 读取并解析 background.json
    val backgroundPpuMap = try {
        val bgJsonText = assetManager.open("$cleanDir/background.json").bufferedReader().use { it.readText() }
        normalizeBackgroundPpuMap(Json.parseToJsonElement(bgJsonText))
    } catch (_: Throwable) {
        emptyMap()
    }

    // 5. 读取并解析 story_variables.json
    val audioVariables = try {
        val varJsonText = assetManager.open("$cleanDir/story_variables.json").bufferedReader().use { it.readText() }
        val root = Json.parseToJsonElement(varJsonText).jsonObject
        root.mapValues { (_, v) -> v.jsonPrimitive.content }
    } catch (_: Throwable) {
        emptyMap()
    }

    val (_, metadata) = parseStory(scriptText)

    val ctx = Context(
        script = scriptText.replace("\r\n", "\n").replace("\r", "\n").split("\n"),
        scriptText = scriptText,
        storyMetadata = metadata,
        audioVariables = audioVariables,
        linkMap = linkMap,
        backgroundPpuMap = backgroundPpuMap
    )

    Pair(scriptText, ctx)
}