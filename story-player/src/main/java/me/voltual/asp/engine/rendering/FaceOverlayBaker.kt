package me.voltual.asp.engine.rendering

import android.content.Context as AndroidContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.LruCache
import coil3.ImageLoader
import coil3.request.ImageRequest
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
    data class SingleUrl(val url: String, override val linkNode: StoryLinkNode?) : CharacterArtVisual
    data class BakedBitmap(val bitmap: Bitmap, override val linkNode: StoryLinkNode?) : CharacterArtVisual
}

object FaceOverlayBaker {
    // Cache up to 32 composite bitmaps in memory
    private val memoryCache = object : LruCache<String, Bitmap>(32) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Let GC handle bitmap recycling safely
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
            return@withContext CharacterArtVisual.SingleUrl(url, link)
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
                val baseRequest = ImageRequest.Builder(androidContext).data(baseUrl).build()
                val faceRequest = ImageRequest.Builder(androidContext).data(faceUrl).build()

                val baseResult = imageLoader.execute(baseRequest)
                val faceResult = imageLoader.execute(faceRequest)

                if (baseResult is SuccessResult && faceResult is SuccessResult) {
                    val baseBmp = baseResult.image.toBitmap()
                    val faceBmp = faceResult.image.toBitmap()

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