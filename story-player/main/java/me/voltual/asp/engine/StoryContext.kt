package me.voltual.asp.engine

import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.serialization.json.*

/**
 * Interface for the host app to provide resources to the StoryPlayer.
 * Utilizing kotlinx.io.Source for stream-based reading to support external assets.
 */
interface StoryResourceProvider {
    suspend fun fetchCharacterJson(): Source?
    suspend fun fetchBackgroundPpuJson(): Source?
    suspend fun fetchStoryVariablesJson(): Source?
    suspend fun fetchAssetSource(url: String): Source?
}

data class StoryContext(
    val script: List<String>,
    val scriptText: String,
    val storyMetadata: StoryMetadata,
    val audioVariables: Map<String, Any>,
    val linkMap: Map<String, StoryLinkNode>,
    val backgroundPpuMap: Map<String, Float>
)

object StoryContextHelper {

    fun normalizeCharacterMap(raw: JsonObject): Map<String, StoryLinkNode> {
        val output = mutableMapOf<String, StoryLinkNode>()

        for ((base, rawNodeElement) in raw) {
            val node = rawNodeElement.jsonObject ?: continue

            val posRaw = node["pos"]?.jsonObject
            val posX = posRaw?.get("x")?.jsonPrimitive?.floatOrNull ?: 0f
            val posY = posRaw?.get("y")?.jsonPrimitive?.floatOrNull ?: 0f

            val sizeRaw = node["size"]?.jsonObject
            val sizeX = sizeRaw?.get("x")?.jsonPrimitive?.floatOrNull ?: 0f
            val sizeY = sizeRaw?.get("y")?.jsonPrimitive?.floatOrNull ?: 0f

            val groupsRaw = node["groups"]?.jsonArray ?: JsonArray(emptyList())
            val groups = groupsRaw.mapNotNull { rawGroup ->
                val group = rawGroup.jsonObject ?: return@mapNotNull StoryCharacterGroupSingle
                val mode = group["mode"]?.jsonPrimitive?.content
                if (mode == "face_overlay") {
                    val faceRectRaw = group["faceRect"]?.jsonObject
                    StoryCharacterGroupFaceOverlay(
                        base = group["base"]?.jsonPrimitive?.content ?: "",
                        faceRect = StoryFaceRect(
                            x = faceRectRaw?.get("x")?.jsonPrimitive?.floatOrNull?.toInt() ?: 0,
                            y = faceRectRaw?.get("y")?.jsonPrimitive?.floatOrNull?.toInt() ?: 0,
                            w = faceRectRaw?.get("w")?.jsonPrimitive?.floatOrNull?.toInt() ?: 0,
                            h = faceRectRaw?.get("h")?.jsonPrimitive?.floatOrNull?.toInt() ?: 0
                        )
                    )
                } else {
                    StoryCharacterGroupSingle
                }
            }

            val arrayRaw = node["array"]?.jsonArray ?: JsonArray(emptyList())
            val array = mutableListOf<StoryCharacterEntry>()

            for (rawItem in arrayRaw) {
                val item = rawItem.jsonObject ?: continue
                val name = item["name"]?.jsonPrimitive?.content ?: continue
                val alias = item["alias"]?.jsonPrimitive?.content ?: ""
                val groupIndex = item["group"]?.jsonPrimitive?.floatOrNull?.toInt() ?: -1

                if (groupIndex == -1) {
                    array.add(
                        StoryCharacterEntry(
                            alias = alias,
                            group = -1,
                            image = item["image"]?.jsonPrimitive?.content ?: name,
                            name = name,
                            face = null
                        )
                    )
                    continue
                }

                val faceStr = item["face"]?.jsonPrimitive?.content
                if (faceStr != null) {
                    array.add(
                        StoryCharacterEntry(
                            alias = alias,
                            face = faceStr,
                            group = groupIndex,
                            name = name,
                            image = null
                        )
                    )
                    continue
                }

                val imageStr = item["image"]?.jsonPrimitive?.content
                if (imageStr != null) {
                    array.add(
                        StoryCharacterEntry(
                            alias = alias,
                            group = -1,
                            image = imageStr,
                            name = name,
                            face = null
                        )
                    )
                }
            }

            output[base.lowercase()] = StoryLinkNode(
                array = array,
                groups = groups,
                pos = Pair(posX, posY),
                size = Pair(sizeX, sizeY)
            )
        }

        return output
    }

    fun normalizeBackgroundPpuMap(raw: JsonObject): Map<String, Float> {
        val output = mutableMapOf<String, Float>()
        for ((key, element) in raw) {
            val ppu = element.jsonPrimitive.floatOrNull
            if (ppu != null && ppu > 0f) {
                output[key.lowercase()] = ppu
            }
        }
        return output
    }

    fun parseStoryVariables(raw: JsonObject): Map<String, Any> {
        val output = mutableMapOf<String, Any>()
        for ((key, element) in raw) {
            element.jsonPrimitive.contentOrNull?.let { content ->
                output[key.lowercase()] = content
            }
        }
        return output
    }

    private suspend fun fetchStoryCharacterMap(resourceProvider: StoryResourceProvider): Map<String, StoryLinkNode> {
        val source = resourceProvider.fetchCharacterJson() ?: return emptyMap()
        val jsonString = source.readString()
        source.close()
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        return normalizeCharacterMap(jsonObject)
    }

    private suspend fun fetchBackgroundPpuMap(resourceProvider: StoryResourceProvider): Map<String, Float> {
        val source = resourceProvider.fetchBackgroundPpuJson() ?: return emptyMap()
        val jsonString = source.readString()
        source.close()
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        return normalizeBackgroundPpuMap(jsonObject)
    }

    private suspend fun fetchStoryVariables(resourceProvider: StoryResourceProvider): Map<String, Any> {
        val source = resourceProvider.fetchStoryVariablesJson() ?: return emptyMap()
        val jsonString = source.readString()
        source.close()
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        return parseStoryVariables(jsonObject)
    }

    suspend fun loadContextByScript(
        scriptText: String,
        resourceProvider: StoryResourceProvider
    ): StoryContext {
        val audioVariables = fetchStoryVariables(resourceProvider)
        val linkMap = fetchStoryCharacterMap(resourceProvider)
        val backgroundPpuMap = fetchBackgroundPpuMap(resourceProvider)

        val parseResult = StoryParser.parseStory(scriptText)

        return StoryContext(
            script = scriptText.replace(Regex("\\r\\n?"), "\n").split("\n"),
            scriptText = scriptText,
            storyMetadata = parseResult.metadata,
            audioVariables = audioVariables,
            linkMap = linkMap,
            backgroundPpuMap = backgroundPpuMap
        )
    }
}