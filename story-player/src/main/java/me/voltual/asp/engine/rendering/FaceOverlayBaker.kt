package me.voltual.asp.engine.rendering

import android.content.Context as AndroidContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.LruCache
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.voltual.asp.Context
import me.voltual.asp.engine.StoryCharacterGroup
import me.voltual.asp.engine.StoryLinkNode
import me.voltual.asp.engine.resolveStoryCharacterAssetByKey

sealed interface CharacterArtVisual {
    val linkNode: StoryLinkNode?
    val sourceWidth: Float
    val sourceHeight: Float

    data class SingleUrl(
        val url: String,
        override val linkNode: StoryLinkNode?,
        override val sourceWidth: Float = 0f,
        override val sourceHeight: Float = 0f
    ) : CharacterArtVisual

    data class BakedBitmap(
        val bitmap: Bitmap,
        override val linkNode: StoryLinkNode?,
        override val sourceWidth: Float = bitmap.width.toFloat(),
        override val sourceHeight: Float = bitmap.height.toFloat()
    ) : CharacterArtVisual
}

object FaceOverlayBaker {
    // Cache up to 32 composite bitmaps in memory
    private val memoryCache = object : LruCache<String, Bitmap>(32) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Let GC handle bitmap recycling safely
        }
    }

    private fun Bitmap.toSoftwareBitmap(): Bitmap {
        return if (config == Bitmap.Config.HARDWARE) {
            copy(Bitmap.Config.ARGB_8888, false) ?: this
        } else {
            this
        }
    }

    suspend fun resolveAndBake(
        context: Context,
        characterKey: String?,
        expression: String?,
        androidContext: AndroidContext
    ): CharacterArtVisual? = withContext(Dispatchers.IO) {
        if (characterKey == null) return@withContext null
        val link = context.linkMap[characterKey.lowercase()] ?: return@withContext null
        val item = link.array.find { it.name.equals(expression, ignoreCase = true) }
            ?: link.array.firstOrNull() ?: return@withContext null

        // 1. Single complete image
        if (item.group == -1 && !item.image.isNullOrEmpty()) {
            val url = resolveStoryCharacterAssetByKey(item.image) ?: return@withContext null
            val imageLoader = ImageLoader(androidContext)
            val request = ImageRequest.Builder(androidContext).data(url).build()
            val result = imageLoader.execute(request)
            val (w, h) = if (result is SuccessResult) {
                result.image.width.toFloat() to result.image.height.toFloat()
            } else {
                1024f to 1024f
            }
            return@withContext CharacterArtVisual.SingleUrl(
                url = url,
                linkNode = link,
                sourceWidth = w,
                sourceHeight = h
            )
        }

        // 2. Face overlay group
        if (item.group >= 0 && !item.face.isNullOrEmpty()) {
            val group = link.groups.getOrNull(item.group)
            if (group is StoryCharacterGroup.FaceOverlay) {
                val cacheKey = "${group.base}|${item.face}|${group.faceRect.x},${group.faceRect.y},${group.faceRect.w},${group.faceRect.h}"
                val cached = memoryCache.get(cacheKey)
                if (cached != null) {
                    return@withContext CharacterArtVisual.BakedBitmap(cached, link)
                }

                val baseUrl = resolveStoryCharacterAssetByKey(group.base) ?: return@withContext null
                val faceUrl = resolveStoryCharacterAssetByKey(item.face) ?: return@withContext null

                val imageLoader = ImageLoader(androidContext)
                val baseRequest = ImageRequest.Builder(androidContext)
                    .data(baseUrl)
                    .allowHardware(false)
                    .build()
                val faceRequest = ImageRequest.Builder(androidContext)
                    .data(faceUrl)
                    .allowHardware(false)
                    .build()

                val baseResult = imageLoader.execute(baseRequest)
                val faceResult = imageLoader.execute(faceRequest)

                if (baseResult is SuccessResult && faceResult is SuccessResult) {
                    val baseBmp = baseResult.image.toBitmap().toSoftwareBitmap()
                    val faceBmp = faceResult.image.toBitmap().toSoftwareBitmap()

                    val baked = Bitmap.createBitmap(baseBmp.width, baseBmp.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(baked)
                    canvas.drawBitmap(baseBmp, 0f, 0f, null)

                    val destRect = Rect(
                        group.faceRect.x,
                        group.faceRect.y,
                        group.faceRect.x + group.faceRect.w,
                        group.faceRect.y + group.faceRect.h
                    )
                    canvas.drawBitmap(faceBmp, null, destRect, null)

                    memoryCache.put(cacheKey, baked)
                    return@withContext CharacterArtVisual.BakedBitmap(baked, link)
                }
            }
        }

        null
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}