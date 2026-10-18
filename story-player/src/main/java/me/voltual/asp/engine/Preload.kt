package me.voltual.asp.engine

import android.content.Context as AndroidContext
import android.util.Log
import coil3.ImageLoader
import coil3.request.ImageRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.discard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.voltual.asp.Context
import java.util.concurrent.atomic.AtomicInteger

/**
 * 角色脸部差分资源定义，与 Web 版 StoryCharacterFaceAsset 保持一致
 */
data class StoryCharacterFaceAsset(
    val baseUrl: String,
    val expression: String,
    val faceRect: StoryFaceRect,
    val faceUrl: String,
    val used: Boolean
)

/**
 * 剧情资源完整清单
 */
data class ContextAssetManifest(
    val faceAssets: List<StoryCharacterFaceAsset>,
    val urls: List<String>
)

private fun argAsString(raw: Any?): String {
    return when (raw) {
        is String -> raw.trim()
        null -> ""
        else -> raw.toString().trim()
    }
}

private data class GroupedImageSelection(
    val asBackground: Boolean,
    val group: String
)

private fun resolveGroupedImageSelection(
    imageGroup: String,
    cgGroup: String,
    isBackgroundCommand: Boolean
): GroupedImageSelection? {
    if (imageGroup.isNotEmpty()) {
        return GroupedImageSelection(asBackground = isBackgroundCommand, group = imageGroup)
    }
    if (cgGroup.isNotEmpty()) {
        return GroupedImageSelection(asBackground = false, group = cgGroup)
    }
    return null
}

private fun addImageUrl(
    urls: MutableSet<String>,
    rawKey: String,
    family: StoryAssetFamily
) {
    val key = rawKey.trim()
    if (key.isEmpty()) return

    val rawUrl = resolveStoryAssetByKey(key, family) ?: return
    urls.add(resolveAssetUrl(rawUrl))
}

private fun addAudioUrl(
    urls: MutableSet<String>,
    context: Context,
    rawKey: String
) {
    val key = rawKey.trim()
    if (key.isEmpty()) return

    val rawUrl = resolveStoryAudioByKey(key, context.audioVariables) ?: return
    urls.add(rawUrl)
}

private fun resolveCharacterUrl(rawKey: String): String? {
    val key = rawKey.trim()
    if (key.isEmpty()) return null

    val rawUrl = resolveStoryCharacterAssetByKey(key) ?: return null
    return resolveAssetUrl(rawUrl)
}

private fun addCharacterUrl(urls: MutableSet<String>, rawKey: String): String? {
    val url = resolveCharacterUrl(rawKey) ?: return null
    urls.add(url)
    return url
}

/**
 * 静态扫描脚本中声明的静态图片指令依赖（对齐 Web 版 addScriptImageUrls）
 */
private fun addScriptImageUrls(urls: MutableSet<String>, lines: List<ParsedLine>) {
    for (line in lines) {
        if (line !is ParsedLine.Command) continue

        val image = argAsString(line.args["image"])
        when (line.command) {
            "background" -> {
                addImageUrl(urls, image, StoryAssetFamily.BACKGROUND)
            }
            "avgdisplay" -> {
                val rawStyle = argAsString(line.args["style"])
                val styleMap = mapOf("5" to "bg", "7" to "character")
                val style = styleMap[rawStyle] ?: rawStyle
                val name = argAsString(line.args["name"])
                if (style == "bg") {
                    addImageUrl(urls, name, StoryAssetFamily.BACKGROUND)
                } else if (style == "character") {
                    addCharacterUrl(urls, name)
                }
            }
            "image", "showitem", "cgitem", "blocker" -> {
                addImageUrl(urls, image, StoryAssetFamily.IMAGE)
            }
            "interlude" -> {
                val typeStr = argAsString(line.args["type"])
                val numericType = (line.args["type"] as? Number)?.toInt() ?: typeStr.toIntOrNull()
                val name = argAsString(line.args["name"])
                if (typeStr == "bg" || numericType == 2) {
                    addImageUrl(urls, name, StoryAssetFamily.BACKGROUND)
                } else if (typeStr == "uichar" || numericType == 1) {
                    addImageUrl(urls, name, StoryAssetFamily.CUTIN)
                }
            }
            "gridbg", "verticalbg", "largebg", "largeimg" -> {
                val imageGroup = argAsString(line.args["imagegroup"])
                val cgGroup = argAsString(line.args["cggroup"])
                val groupSelection = resolveGroupedImageSelection(
                    imageGroup = imageGroup,
                    cgGroup = cgGroup,
                    isBackgroundCommand = line.command.endsWith("bg")
                ) ?: continue

                for (key in groupSelection.group.split("/")) {
                    val family = if (groupSelection.asBackground) StoryAssetFamily.BACKGROUND else StoryAssetFamily.IMAGE
                    addImageUrl(urls, key, family)
                }
            }
        }
    }
}

/**
 * 静态扫描音频依赖（对齐 Web 版 addScriptAudioUrls）
 */
private fun addScriptAudioUrls(
    urls: MutableSet<String>,
    context: Context,
    lines: List<ParsedLine>
) {
    for (line in lines) {
        if (line !is ParsedLine.Command) continue

        if (line.command != "playmusic" && line.command != "playsound") continue

        addAudioUrl(urls, context, argAsString(line.args["key"]))
        if (line.command == "playmusic") {
            addAudioUrl(urls, context, argAsString(line.args["intro"]))
        }
    }
}

/**
 * 静态扫描角色立绘与表情差分贴图（对齐 Web 版 addCharacterUrls）
 */
private fun addCharacterUrls(
    urls: MutableSet<String>,
    context: Context,
    lines: List<ParsedLine>
): List<StoryCharacterFaceAsset> {
    val faceAssets = mutableMapOf<String, StoryCharacterFaceAsset>()

    fun addFaceGroup(
        link: StoryLinkNode,
        groupIndex: Int,
        baseUrl: String,
        usedExpression: String? = null
    ) {
        val group = link.groups.getOrNull(groupIndex)
        if (group !is StoryCharacterGroup.FaceOverlay) return

        for (candidate in link.array) {
            if (candidate.group != groupIndex || candidate.face.isNullOrEmpty()) continue

            val faceUrl = resolveCharacterUrl(candidate.face) ?: continue
            val rect = group.faceRect
            val key = "$baseUrl\u0000$faceUrl\u0000${rect.x},${rect.y},${rect.w},${rect.h}"
            val previous = faceAssets[key]

            faceAssets[key] = StoryCharacterFaceAsset(
                baseUrl = baseUrl,
                expression = previous?.expression ?: candidate.name,
                faceRect = rect,
                faceUrl = faceUrl,
                used = previous?.used == true || candidate.name == usedExpression
            )
        }
    }

    for (line in lines) {
        if (line !is ParsedLine.Command) continue

        val refs = mutableListOf<String>()
        when (line.command) {
            "charslot" -> refs.add(argAsString(line.args["name"]))
            "character" -> {
                refs.add(argAsString(line.args["name"]))
                refs.add(argAsString(line.args["name2"]))
            }
            "interlude" -> {
                val typeNum = (line.args["type"] as? Number)?.toInt() ?: argAsString(line.args["type"]).toIntOrNull()
                if (typeNum == 3 || argAsString(line.args["type"]) == "char") {
                    refs.add(argAsString(line.args["name"]))
                }
            }
            else -> continue
        }

        for (nameRef in refs) {
            if (nameRef.isEmpty()) continue

            val resolved = resolveCharacterSelection(context.linkMap, nameRef) ?: continue
            val link = context.linkMap[resolved.base] ?: continue
            val item = link.array.find { it.name == resolved.expression } ?: continue

            if (item.group == -1 && !item.image.isNullOrEmpty()) {
                val imageUrl = addCharacterUrl(urls, item.image)
                if (imageUrl != null) {
                    for ((groupIndex, group) in link.groups.withIndex()) {
                        if (group is StoryCharacterGroup.FaceOverlay && resolveCharacterUrl(group.base) == imageUrl) {
                            addFaceGroup(link, groupIndex, imageUrl)
                        }
                    }
                }
                continue
            }

            if (item.face.isNullOrEmpty() || item.group < 0) continue

            val group = link.groups.getOrNull(item.group)
            if (group !is StoryCharacterGroup.FaceOverlay) continue

            val usedFaceUrl = addCharacterUrl(urls, item.face)
            val baseUrl = addCharacterUrl(urls, group.base)
            if (usedFaceUrl == null || baseUrl == null) continue

            addFaceGroup(link, item.group, baseUrl, item.name)
        }
    }

    return faceAssets.values.toList()
}

private fun parseContextScript(context: Context): List<ParsedLine> {
    val text = context.scriptText ?: context.script.joinToString("\n")
    return parseScript(text)
}

/**
 * 构建完整的剧情资源清单
 */
fun buildContextAssetManifest(
    context: Context,
    lines: List<ParsedLine> = parseContextScript(context)
): ContextAssetManifest {
    val urls = mutableSetOf<String>()
    context.charMap?.values?.forEach { rawUrl ->
        urls.add(resolveAssetUrl(rawUrl))
    }

    val faceAssets = addCharacterUrls(urls, context, lines)
    addScriptAudioUrls(urls, context, lines)
    addScriptImageUrls(urls, lines)

    return ContextAssetManifest(
        faceAssets = faceAssets,
        urls = urls.toList()
    )
}

fun collectContextAssetManifest(context: Context): ContextAssetManifest {
    return buildContextAssetManifest(context)
}

fun collectContextAssetUrls(context: Context): List<String> {
    return collectContextAssetManifest(context).urls
}

private fun isAudioUrl(url: String): Boolean {
    val clean = url.substringBefore('?').substringBefore('#').lowercase()
    return clean.endsWith(".mp3") || clean.endsWith(".ogg") || clean.endsWith(".wav") ||
            clean.endsWith(".m4a") || clean.endsWith(".aac") || clean.endsWith(".flac") ||
            clean.contains("/audio/")
}

/**
 * 剧情资源异步批量预加载器。
 * - 图片资源通过 Coil3 ImageLoader 预取至缓存（支持网络及 file:///android_asset）；
 * - 音频与多媒体数据如果是远程 URL 通过 Ktor HttpClient 请求预热，若是本地 asset 则无需网络请求；
 * - 具备最大并发数控制与原子平滑进度报告。
 */
suspend fun preloadContextAssets(
    context: Context,
    androidContext: AndroidContext,
    httpClient: HttpClient? = null,
    maxConcurrency: Int = 8,
    onProgress: ((Float) -> Unit)? = null
): Unit = withContext(Dispatchers.IO) {
    val manifest = collectContextAssetManifest(context)
    val urls = manifest.urls
    if (urls.isEmpty()) {
        onProgress?.invoke(1f)
        return@withContext
    }

    val totalCount = urls.size
    val loadedCount = AtomicInteger(0)
    val semaphore = Semaphore(maxConcurrency.coerceAtLeast(1))

    val imageLoader = ImageLoader(androidContext)
    val client = httpClient ?: HttpClient()
    val shouldCloseClient = httpClient == null

    try {
        coroutineScope {
            urls.map { url ->
                async {
                    semaphore.withPermit {
                        try {
                            if (isAudioUrl(url)) {
                                if (!url.startsWith("file:", ignoreCase = true) &&
                                    !url.startsWith("asset:", ignoreCase = true)
                                ) {
                                    // 仅对网络音频数据通过 Ktor 预热
                                    val response = client.get(url)
                                    val channel = response.bodyAsChannel()
                                    channel.discard()
                                }
                            } else {
                                // 使用 Coil 3 预热图片（原生支持 file:///android_asset/...）
                                val request = ImageRequest.Builder(androidContext)
                                    .data(url)
                                    .build()
                                imageLoader.execute(request)
                            }
                        } catch (e: Throwable) {
                            Log.w("StoryPlayerPreload", "Asset preload failed: $url (${e.message})")
                        } finally {
                            val current = loadedCount.incrementAndGet()
                            onProgress?.invoke(current.toFloat() / totalCount.toFloat())
                        }
                    }
                }
            }.awaitAll()
        }
    } finally {
        if (shouldCloseClient) {
            client.close()
        }
        onProgress?.invoke(1f)
    }
}