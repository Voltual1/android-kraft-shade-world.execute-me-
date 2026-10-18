package me.voltual.asp.engine.rendering

import kotlinx.coroutines.*
import me.voltual.asp.Context
import me.voltual.asp.engine.*
import me.voltual.asp.engine.rendering.core.buildShakePath
import me.voltual.asp.engine.rendering.core.dotweenEaseCurve
import me.voltual.asp.engine.rendering.core.sampleShakePath
import kotlin.math.max
import kotlin.random.Random

private val CHARSLOT_POINT_DEFAULT = Vec2(1f, 1f)

private data class SlotMoveRange(val from: Vec2, val to: Vec2)
private data class SlotZoomRange(
    val fromScaleX: Float,
    val fromScaleY: Float,
    val fromShiftX: Float,
    val fromShiftY: Float,
    val toScaleX: Float,
    val toScaleY: Float,
    val toShiftX: Float,
    val toShiftY: Float
)

private sealed interface StickerTweenStep {
    val durationMs: Long
    val join: Boolean

    data class Position(
        override val durationMs: Long,
        override val join: Boolean,
        val from: Vec2,
        val to: Vec2
    ) : StickerTweenStep

    data class Alpha(
        override val durationMs: Long,
        override val join: Boolean,
        val from: Float,
        val to: Float
    ) : StickerTweenStep

    data class Rotation(
        override val durationMs: Long,
        override val join: Boolean,
        val from: Float,
        val to: Float
    ) : StickerTweenStep
}

private data class TimedStickerStep(
    val startMs: Long,
    val step: StickerTweenStep
)

private fun formatTimer(totalSeconds: Long): String {
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

class ComposeStoryRenderer(
    private val context: Context,
    val state: StoryRendererState = StoryRendererState(),
    private val coroutineScope: CoroutineScope,
    private val onWarning: ((String) -> Unit)? = null
) : StoryRenderer {

private var imageTweenSessionId = 0
private var backgroundTweenSessionId = 0

    private var activeShakeJob: Job? = null
    private var pendingDecisionDeferred: CompletableDeferred<DecisionSelection>? = null
    private var activeVideoDeferred: CompletableDeferred<Unit>? = null
    private var isTextTypingComplete = true
    private val cgItemJobs = mutableMapOf<String, MutableList<Job>>()
    private val animTextJobs = mutableListOf<Job>()
    private var animTextSessionCounter = 0
    private val charSlotJobs = mutableMapOf<String, MutableList<Job>>()
    private val interludeJobs = mutableMapOf<Int, MutableList<Job>>()
    private val focusJobs = mutableMapOf<String, Job>()
    private val internalFocusStates = mutableMapOf<String, Float>()
    private val avgDisplayJobs = mutableMapOf<String, Job>()

    private val stickerTweenQueues = mutableMapOf<String, MutableList<StickerTweenStep>>()
    private val stickerTweenSessionIds = mutableMapOf<String, Int>()
    private val stickerJobs = mutableMapOf<String, MutableList<Job>>()

    private var timerCountJob: Job? = null
    private var timerFadeJob: Job? = null

    private fun cancelCharSlotJobs(slot: String) {
        charSlotJobs[slot]?.forEach { it.cancel() }
        charSlotJobs[slot]?.clear()
    }

    private fun addCharSlotJob(slot: String, job: Job) {
        val list = charSlotJobs.getOrPut(slot) { mutableListOf() }
        list.add(job)
        job.invokeOnCompletion { list.remove(job) }
    }

    private fun cancelInterludeJobs(channel: Int) {
        interludeJobs[channel]?.forEach { it.cancel() }
        interludeJobs[channel]?.clear()
    }

    private fun addInterludeJob(channel: Int, job: Job) {
        val list = interludeJobs.getOrPut(channel) { mutableListOf() }
        list.add(job)
        job.invokeOnCompletion { list.remove(job) }
    }

    private fun cancelStickerJobs(id: String) {
        stickerJobs[id]?.forEach { it.cancel() }
        stickerJobs[id]?.clear()
    }

    private fun addStickerJob(id: String, job: Job) {
        val list = stickerJobs.getOrPut(id) { mutableListOf() }
        list.add(job)
        job.invokeOnCompletion { list.remove(job) }
    }

    private suspend fun tween(
        durationMs: Long,
        ease: String = "Linear",
        onUpdate: (Float) -> Unit
    ) {
        if (durationMs <= 0) {
            onUpdate(1f)
            return
        }
        val easeCurve = dotweenEaseCurve(ease)
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            onUpdate(easeCurve(rawProgress))
            if (rawProgress >= 1f) break
            delay(16)
        }
        onUpdate(1f)
    }

    override suspend fun clearCgItems(key: String?, fadeMs: Long, ease: String, block: Boolean) {
        val easeCurve = dotweenEaseCurve(ease)
        if (key != null) {
            val slot = state.cgItems[key] ?: return
            cgItemJobs[key]?.forEach { it.cancel() }
            cgItemJobs.remove(key)

            if (fadeMs <= 0) {
                state.cgItems.remove(key)
                return
            }

            val job = coroutineScope.launch {
                val startAlpha = slot.alpha
                tween(fadeMs, ease) { progress ->
                    state.cgItems[key] = slot.copy(alpha = startAlpha * (1f - progress))
                }
                state.cgItems.remove(key)
            }
            if (block) job.join()
        } else {
            cgItemJobs.values.flatten().forEach { it.cancel() }
            cgItemJobs.clear()

            if (fadeMs <= 0) {
                state.cgItems.clear()
                return
            }

            coroutineScope.launch {
                val snapshot = state.cgItems.toMap()
                tween(fadeMs, ease) { progress ->
                    snapshot.forEach { (k, item) ->
                        state.cgItems[k] = item.copy(alpha = item.alpha * (1f - progress))
                    }
                }
                state.cgItems.clear()
            }
        }
    }

    override suspend fun clearAvgDisplays() {
        avgDisplayJobs.values.forEach { it.cancel() }
        avgDisplayJobs.clear()
        state.avgDisplays.clear()
    }

    override suspend fun clearAnimTexts() {
        animTextJobs.forEach { it.cancel() }
        animTextJobs.clear()
        state.animTexts.clear()
    }

    override suspend fun setCameraEffect(
        effect: String,
        amount: Float,
        durationMs: Long,
        block: Boolean,
        keep: Boolean,
        initialAmount: Float?
    ) {
        val fromGray = initialAmount ?: state.camera.grayscale

        if (effect == "Colorinverse") {
            state.camera = state.camera.copy(colorInverse = if (keep) 1f else 0f)
            return
        }

        val job = coroutineScope.launch {
            tween(durationMs, "OutQuad") { progress ->
                val gray = fromGray + (amount - fromGray) * progress
                state.camera = state.camera.copy(grayscale = gray)
            }
            if (!keep) {
                state.camera = state.camera.copy(grayscale = 0f)
            }
        }
        if (block && durationMs > 0) job.join()
    }

    override suspend fun setFocusOut(input: FocusOutInput) {
        val key = "${input.type}\u0000${input.id}"
        val fromAmount = input.from ?: (internalFocusStates[key] ?: 0f)
        val toAmount = input.to
        val durationMs = input.durationMs

        internalFocusStates[key] = fromAmount
        updateAggregatedFocusState()

        focusJobs[key]?.cancel()
        val job = coroutineScope.launch {
            tween(durationMs) { progress ->
                internalFocusStates[key] = fromAmount + (toAmount - fromAmount) * progress
                updateAggregatedFocusState()
            }
            internalFocusStates[key] = toAmount
            updateAggregatedFocusState()
        }
        focusJobs[key] = job
        if (input.block && durationMs > 0) job.join()
    }

    override fun setFocusParam(input: FocusParamInput) {
        state.focus = state.focus.copy(
            blur = input.blur,
            colorMode = input.color
        )
    }

    private fun updateAggregatedFocusState() {
        val amounts = mutableMapOf<String, Float>()
        for ((key, amount) in internalFocusStates) {
            val type = key.substringBefore('\u0000')
            val curMax = amounts[type] ?: 0f
            amounts[type] = maxOf(curMax, amount)
        }
        state.focus = state.focus.copy(amounts = amounts)
    }

    override suspend fun clearBackground(fadeMs: Long, block: Boolean) {
        val current = state.background ?: return
        if (fadeMs <= 0) {
            state.background = null
            return
        }
        val job = coroutineScope.launch {
            val startAlpha = current.alpha
            tween(fadeMs) { progress ->
                state.background = current.copy(alpha = startAlpha * (1f - progress))
            }
            state.background = null
        }
        if (block) job.join()
    }

    override suspend fun clearCharacters(slot: String?, fadeMs: Long) {
        if (slot != null) {
            cancelCharSlotJobs(slot)
            val current = state.characters[slot] ?: return
            if (fadeMs <= 0) {
                state.characters.remove(slot)
                return
            }
            coroutineScope.launch {
                val startAlpha = current.alpha
                tween(fadeMs) { progress ->
                    state.characters[slot] = current.copy(alpha = startAlpha * (1f - progress))
                }
                state.characters.remove(slot)
            }
        } else {
            charSlotJobs.keys.toList().forEach { cancelCharSlotJobs(it) }
            if (fadeMs <= 0) {
                state.characters.clear()
                return
            }
            coroutineScope.launch {
                val snapshot = state.characters.toMap()
                tween(fadeMs) { progress ->
                    snapshot.forEach { (s, c) ->
                        state.characters[s] = c.copy(alpha = c.alpha * (1f - progress))
                    }
                }
                state.characters.clear()
            }
        }
    }

    override suspend fun clearCurtains(fadeMs: Long, block: Boolean) {
        if (fadeMs <= 0) {
            state.curtains.clear()
            return
        }
        val job = coroutineScope.launch {
            val snapshot = state.curtains.toMap()
            tween(fadeMs) { progress ->
                snapshot.forEach { (dir, cur) ->
                    state.curtains[dir] = cur.copy(fill = cur.fill * (1f - progress))
                }
            }
            state.curtains.clear()
        }
        if (block) job.join()
    }

    override suspend fun clearGridBackground(fadeMs: Long, block: Boolean) {
        val current = state.gridBackground ?: return
        if (fadeMs <= 0) {
            state.gridBackground = null
            return
        }
        val job = coroutineScope.launch {
            val startAlpha = current.alpha
            tween(fadeMs) { progress ->
                state.gridBackground = current.copy(alpha = startAlpha * (1f - progress))
            }
            state.gridBackground = null
        }
        if (block) job.join()
    }

    override suspend fun clearImage(fadeMs: Long, block: Boolean) {
        val current = state.foregroundImage ?: return
        if (fadeMs <= 0) {
            state.foregroundImage = null
            return
        }
        val job = coroutineScope.launch {
            val startAlpha = current.alpha
            tween(fadeMs) { progress ->
                state.foregroundImage = current.copy(alpha = startAlpha * (1f - progress))
            }
            state.foregroundImage = null
        }
        if (block) job.join()
    }

    override suspend fun clearLargeImage(fadeMs: Long, block: Boolean) {
        clearGridBackground(fadeMs, block)
    }

    override suspend fun clearItems(fadeMs: Long, block: Boolean) {
        val current = state.showItem ?: return
        if (fadeMs <= 0) {
            state.showItem = null
            return
        }
        val job = coroutineScope.launch {
            val startAlpha = current.alpha
            tween(fadeMs) { progress ->
                state.showItem = current.copy(alpha = startAlpha * (1f - progress))
            }
            state.showItem = null
        }
        if (block) job.join()
    }

    override suspend fun clearSticker(id: String?, fadeMs: Long) {
        if (id != null) {
            cancelStickerJobs(id)
            stickerTweenQueues.remove(id)
            val current = state.stickers[id] ?: return
            if (fadeMs <= 0) {
                state.stickers.remove(id)
                return
            }
            val job = coroutineScope.launch {
                val startAlpha = current.alpha
                tween(fadeMs) { progress ->
                    state.stickers[id] = current.copy(alpha = startAlpha * (1f - progress))
                }
                state.stickers.remove(id)
            }
            addStickerJob(id, job)
        } else {
            clearStickers(fadeMs)
        }
    }

    override suspend fun clearStickers(fadeMs: Long) {
        stickerJobs.keys.toList().forEach { cancelStickerJobs(it) }
        stickerTweenQueues.clear()
        if (fadeMs <= 0) {
            state.stickers.clear()
            return
        }
        coroutineScope.launch {
            val snapshot = state.stickers.toMap()
            tween(fadeMs) { progress ->
                snapshot.forEach { (id, s) ->
                    state.stickers[id] = s.copy(alpha = s.alpha * (1f - progress))
                }
            }
            state.stickers.clear()
        }
    }

    override suspend fun clearSpellStickers() {
        state.spellStickers.clear()
    }

    override suspend fun hideSpellSticker(id: String) {
        val existing = state.spellStickers[id]
        if (existing != null) {
            state.spellStickers[id] = existing.copy(visible = false)
        }
    }

    override suspend fun setSpellSticker(input: SpellStickerInput) {
        val style = input.style.lowercase().let { if (it == "fire" || it == "sami") it else "sami" }
        val existing = state.spellStickers[input.id]
        val (defaultMain, defaultSub) = if (style == "fire") {
            "夜雪无痕，噤声。" to "Ночной снег без следа, молчи."
        } else {
            "不是灾异，而是目光。" to "er eigi ógæfa, heldr augna máttur."
        }

        val parts = parseSpellStickerContent(input.content)
        val main = parts[1] ?: existing?.mainText ?: defaultMain
        val sub = parts[2] ?: existing?.subText ?: defaultSub

        val target = SpellStickerVisualState(
            id = input.id,
            style = style,
            mainText = main,
            subText = sub,
            visible = true,
            alpha = input.alpha ?: existing?.alpha ?: 1f,
            x = input.x ?: existing?.x ?: 0f,
            y = input.y ?: existing?.y ?: 0f,
            scaleX = input.xScale ?: existing?.scaleX ?: 1f,
            scaleY = input.yScale ?: existing?.scaleY ?: 1f,
            angle = input.angle ?: existing?.angle ?: 0f
        )
        state.spellStickers[input.id] = target
    }

    override suspend fun clearSubtitle(fadeMs: Long) {
        val current = state.subtitle
        if (!current.visible) return
        if (fadeMs <= 0) {
            state.subtitle = current.copy(visible = false, text = "")
            return
        }
        coroutineScope.launch {
            val startAlpha = current.alpha
            tween(fadeMs) { progress ->
                state.subtitle = current.copy(alpha = startAlpha * (1f - progress))
            }
            state.subtitle = current.copy(visible = false, text = "")
        }
    }

    override suspend fun clearTimerSticker(input: TimerClearInput?) {
        timerCountJob?.cancel()
        timerCountJob = null
        timerFadeJob?.cancel()

        val durationMs = input?.durationMs ?: 0L
        if (durationMs <= 0L) {
            state.timerSticker = state.timerSticker.copy(visible = false, alpha = 0f)
            return
        }

        val startAlpha = state.timerSticker.alpha
        timerFadeJob = coroutineScope.launch {
            tween(durationMs) { progress ->
                state.timerSticker = state.timerSticker.copy(alpha = startAlpha * (1f - progress))
            }
            state.timerSticker = state.timerSticker.copy(visible = false, alpha = 0f)
        }
    }

    override suspend fun clearCharacterCutin(widgetId: String?) {
        if (widgetId == null) {
            state.cutinSlots.clear()
        } else {
            state.cutinSlots.remove(widgetId)
        }
    }

    override suspend fun clearInterludes() {
        interludeJobs.values.flatten().forEach { it.cancel() }
        interludeJobs.clear()
        state.interludes.clear()
    }

    override fun destroy() {
        activeShakeJob?.cancel()
        pendingDecisionDeferred?.cancel()
        stopVideo()
        cgItemJobs.values.flatten().forEach { it.cancel() }
        cgItemJobs.clear()
        animTextJobs.forEach { it.cancel() }
        animTextJobs.clear()
        charSlotJobs.values.flatten().forEach { it.cancel() }
        charSlotJobs.clear()
        interludeJobs.values.flatten().forEach { it.cancel() }
        interludeJobs.clear()
        stickerJobs.values.flatten().forEach { it.cancel() }
        stickerJobs.clear()
        stickerTweenQueues.clear()
        focusJobs.values.forEach { it.cancel() }
        focusJobs.clear()
        internalFocusStates.clear()
        timerCountJob?.cancel()
        timerCountJob = null
        timerFadeJob?.cancel()
        timerFadeJob = null
        avgDisplayJobs.values.forEach { it.cancel() }
        avgDisplayJobs.clear()
        state.dialogue = DialogueVisualState(visible = false)
        state.decision = DecisionVisualState(visible = false)
        state.video = VideoVisualState(visible = false)
        state.characters.clear()
        state.curtains.clear()
        state.cutinSlots.clear()
        state.interludes.clear()
        state.cgItems.clear()
        state.animTexts.clear()
        state.spellStickers.clear()
        state.stickers.clear()
        state.avgDisplays.clear()
        state.timerSticker = TimerVisualState(visible = false)
    }

    override fun finishTextTyping(): Boolean {
        if (!isTextTypingComplete) {
            isTextTypingComplete = true
            return true
        }
        return false
    }

    override suspend fun playVideo(url: String) {
        val deferred = CompletableDeferred<Unit>()
        activeVideoDeferred?.cancel()
        activeVideoDeferred = deferred

        state.video = VideoVisualState(
            visible = true,
            url = url,
            onEnded = {
                if (deferred.isActive) {
                    deferred.complete(Unit)
                }
            }
        )

        try {
            deferred.await()
        } finally {
            state.video = VideoVisualState(visible = false, url = "")
            activeVideoDeferred = null
        }
    }

    override suspend fun setAnimText(input: AnimTextInput) {
        if (input.name != "group_location_stamp") {
            onWarning?.invoke("unsupported_visual animtext:${input.name}")
            return
        }

        val parsed = parseAnimTextContent(input.content)
        val session = ++animTextSessionCounter
        val item = AnimTextVisualItem(
            id = input.id,
            sessionId = session,
            mainText = parsed.mainText,
            subText = parsed.subText,
            style = input.style,
            positionX = input.position.x,
            positionY = input.position.y,
            progress = 0f
        )
        state.animTexts.add(item)

        val job = coroutineScope.launch {
            tween(5000L) { progress ->
                val index = state.animTexts.indexOfFirst { it.sessionId == session }
                if (index != -1) {
                    state.animTexts[index] = state.animTexts[index].copy(progress = progress)
                }
            }
            state.animTexts.removeAll { it.sessionId == session }
        }
        animTextJobs.add(job)
        job.invokeOnCompletion { animTextJobs.remove(job) }

        if (input.block) {
            job.join()
        }
    }

    override suspend fun setAvgDisplay(input: AvgDisplayInput) {
        val id = input.id
        if (input.name.isEmpty()) {
            avgDisplayJobs[id]?.cancel()
            avgDisplayJobs.remove(id)
            state.avgDisplays.remove(id)
            return
        }

        avgDisplayJobs[id]?.cancel()
        val existing = state.avgDisplays[id]

        val startX = input.xFrom ?: input.x ?: existing?.x ?: 0f
        val startY = input.yFrom ?: input.y ?: existing?.y ?: 0f
        val endX = input.xTo ?: startX
        val endY = input.yTo ?: startY

        val startScaleX = input.scaleXFrom ?: input.scaleX ?: existing?.scaleX ?: 1f
        val startScaleY = input.scaleYFrom ?: input.scaleY ?: existing?.scaleY ?: 1f
        val endScaleX = input.scaleXTo ?: startScaleX
        val endScaleY = input.scaleYTo ?: startScaleY

        val supportsPosition = input.style in setOf("effect", "bgeffect", "animekv", "character")
        val supportsScale = input.style == "animekv"
        val supportsFade = input.style == "bg"

        val initialAlpha = if (supportsFade) (input.alphaFrom ?: existing?.alpha ?: 1f) else 1f
        val targetAlpha = if (supportsFade) (input.alphaTo ?: 0f) else 1f

        var item = AvgDisplayVisualItem(
            id = id,
            name = input.name,
            style = input.style,
            slot = input.slot,
            layer = input.layer,
            x = if (supportsPosition) startX else 0f,
            y = if (supportsPosition) startY else 0f,
            scaleX = if (supportsScale) startScaleX else 1f,
            scaleY = if (supportsScale) startScaleY else 1f,
            rotationZ = if (input.style in setOf("effect", "bgeffect", "character")) input.rotationZ else 0f,
            alpha = initialAlpha,
            visible = true
        )
        state.avgDisplays[id] = item

        val hasPositionTween = supportsPosition && (startX != endX || startY != endY)
        val hasScaleTween = supportsScale && (startScaleX != endScaleX || startScaleY != endScaleY)
        val hasFadeTween = supportsFade && (initialAlpha != targetAlpha)

        val hasTween = input.durationMs > 0 && (hasPositionTween || hasScaleTween || hasFadeTween)

        if (hasTween) {
            val job = coroutineScope.launch {
                tween(input.durationMs) { progress ->
                    val cur = state.avgDisplays[id] ?: return@tween
                    val curX = if (supportsPosition) startX + (endX - startX) * progress else cur.x
                    val curY = if (supportsPosition) startY + (endY - startY) * progress else cur.y
                    val curScaleX = if (supportsScale) startScaleX + (endScaleX - startScaleX) * progress else cur.scaleX
                    val curScaleY = if (supportsScale) startScaleY + (endScaleY - startScaleY) * progress else cur.scaleY
                    val curAlpha = if (supportsFade) initialAlpha + (targetAlpha - initialAlpha) * progress else cur.alpha

                    state.avgDisplays[id] = cur.copy(
                        x = curX,
                        y = curY,
                        scaleX = curScaleX,
                        scaleY = curScaleY,
                        alpha = curAlpha
                    )
                }
                val cur = state.avgDisplays[id]
                if (cur != null) {
                    state.avgDisplays[id] = cur.copy(
                        x = if (supportsPosition) endX else cur.x,
                        y = if (supportsPosition) endY else cur.y,
                        scaleX = if (supportsScale) endScaleX else cur.scaleX,
                        scaleY = if (supportsScale) endScaleY else cur.scaleY,
                        alpha = if (supportsFade) targetAlpha else cur.alpha
                    )
                }
            }
            avgDisplayJobs[id] = job
            job.invokeOnCompletion { avgDisplayJobs.remove(id) }

            if (input.block) {
                job.join()
            }
        } else {
            state.avgDisplays[id] = item.copy(
                x = if (supportsPosition) endX else item.x,
                y = if (supportsPosition) endY else item.y,
                scaleX = if (supportsScale) endScaleX else item.scaleX,
                scaleY = if (supportsScale) endScaleY else item.scaleY,
                alpha = if (supportsFade) targetAlpha else item.alpha
            )
        }
    }

    override suspend fun setBackground(key: String, input: BackgroundInput?) {
        val inp = input ?: BackgroundInput()
        val target = BackgroundVisualState(
            textureKey = key,
            alpha = if (inp.fadeMs > 0) 0f else 1f,
            scaleX = inp.scaleX,
            scaleY = inp.scaleY,
            x = inp.x,
            y = inp.y,
            widthMultiplier = inp.width ?: 1f,
            heightMultiplier = inp.height ?: 1f,
            tiled = inp.tiled,
            screenAdapt = inp.screenAdapt
        )
        state.background = target
        if (inp.fadeMs > 0) {
            val job = coroutineScope.launch {
                tween(inp.fadeMs) { progress ->
                    state.background = target.copy(alpha = progress)
                }
            }
            if (inp.block) job.join()
        }
    }

    override suspend fun setBackgroundTween(input: BackgroundTweenInput) {
    val current = state.background ?: return
    val sessionId = ++backgroundTweenSessionId
    val fromX = input.xFrom ?: current.x
    val toX = input.xTo ?: current.x
    val fromY = input.yFrom ?: current.y
    val toY = input.yTo ?: current.y
    val fromScaleX = input.xScaleFrom ?: current.scaleX
    val toScaleX = input.xScaleTo ?: current.scaleX
    val fromScaleY = input.yScaleFrom ?: current.scaleY
    val toScaleY = input.yScaleTo ?: current.scaleY

    val job = coroutineScope.launch {
        tween(input.durationMs, input.ease ?: "Linear") { progress ->
            if (backgroundTweenSessionId != sessionId) return@tween
            state.background = current.copy(
                x = fromX + (toX - fromX) * progress,
                y = fromY + (toY - fromY) * progress,
                scaleX = fromScaleX + (toScaleX - fromScaleX) * progress,
                scaleY = fromScaleY + (toScaleY - fromScaleY) * progress
            )
        }
    }
    if (input.block && input.durationMs > 0 && !input.loop) job.join()
}

    override suspend fun setBlocker(input: BlockerInput) {
        val initialAlpha = if (input.fadeMs > 0) input.from.a else input.to.a
        val shouldBeVisible = initialAlpha > 0f || input.to.a > 0f

        val target = BlockerVisualState(
            visible = shouldBeVisible,
            color = input.to,
            style = input.style,
            image = input.image,
            inverse = input.inverse,
            alpha = initialAlpha,
            flipX = input.inverse && input.style == "slider",
            flipY = input.inverse && input.style == "verticalslider"
        )
        state.blocker = target
        if (input.fadeMs > 0) {
            val job = coroutineScope.launch {
                tween(input.fadeMs) { progress ->
                    val curAlpha = input.from.a + (input.to.a - input.from.a) * progress
                    state.blocker = target.copy(
                        alpha = curAlpha,
                        visible = curAlpha > 0f || input.to.a > 0f
                    )
                }
                state.blocker = target.copy(
                    alpha = input.to.a,
                    visible = input.to.a > 0f
                )
            }
            if (input.block) job.join()
        }
    }

    private fun characterSlotMove(current: CharacterVisualSlot, input: CharacterSlotInput): SlotMoveRange? {
        val posTo = input.positionTo ?: CHARSLOT_POINT_DEFAULT
        return when (input.action) {
            null, "move", "setpos" -> {
                if (input.positionFrom == null) null
                else SlotMoveRange(from = input.positionFrom, to = posTo)
            }
            "shakemove" -> {
                SlotMoveRange(from = input.positionFrom ?: CHARSLOT_POINT_DEFAULT, to = posTo)
            }
            "jump" -> {
                if ((input.durationMs ?: 0L) > 0L) {
                    SlotMoveRange(
                        from = Vec2(current.actionX, current.actionY),
                        to = Vec2(current.actionX + posTo.x, current.actionY + posTo.y)
                    )
                } else {
                    SlotMoveRange(from = input.positionFrom ?: CHARSLOT_POINT_DEFAULT, to = posTo)
                }
            }
            else -> null
        }
    }

    private fun characterSlotZoom(current: CharacterVisualSlot, input: CharacterSlotInput): SlotZoomRange? {
        if (input.action != "zoom") return null
        val zoom = input.posZoom
        val pivotValid = zoom == null || (zoom.x in 0f..1f && zoom.y in 0f..1f)
        if (!pivotValid) return null

        val toScaleX = input.scaleX ?: 1f
        val toScaleY = input.scaleY ?: 1f
        val pivotX = zoom?.x ?: 0.5f
        val pivotY = zoom?.y ?: 0.5f

        val charWidth = 1024f
        val charHeight = 1024f

        return SlotZoomRange(
            fromScaleX = current.scaleX,
            fromScaleY = current.scaleY,
            fromShiftX = current.zoomShiftX,
            fromShiftY = current.zoomShiftY,
            toScaleX = toScaleX,
            toScaleY = toScaleY,
            toShiftX = (0.5f - pivotX) * toScaleX * charWidth - ((toScaleX - 1f) * charWidth) / 2f,
            toShiftY = (0.5f - pivotY) * toScaleY * charHeight + ((toScaleY - 1f) * charHeight) / 2f
        )
    }

    override suspend fun setCharacter(input: CharacterSlotInput) {
        val slot = input.slot
        val current = state.characters[slot] ?: CharacterVisualSlot(slot = slot)
        val durationMs = input.durationMs ?: 0L
        val jobs = mutableListOf<Job>()

        if (input.focusMode != null && input.focusSlots != null) {
            val activeSet = input.focusSlots.toSet()
            state.characters.forEach { (s, c) ->
                state.characters[s] = c.copy(dimmed = !activeSet.contains(s))
            }
        }

        var updated = current
        val hasNewChar = input.characterKey != null && input.expression != null
        val isDifferentChar = hasNewChar && (input.characterKey != current.characterKey || input.expression != current.expression)

        if (hasNewChar) {
            val replaceFade = input.replaceFadeMs ?: durationMs
            if (isDifferentChar && replaceFade > 0 && current.characterKey != null) {
                updated = current.copy(
                    characterKey = input.characterKey,
                    expression = input.expression,
                    oldCharacterKey = current.characterKey,
                    oldExpression = current.expression,
                    oldCharAlpha = 1f
                )
                state.characters[slot] = updated
                val fadeJob = coroutineScope.launch {
                    tween(replaceFade) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(oldCharAlpha = 1f - progress)
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(
                            oldCharacterKey = null,
                            oldExpression = null,
                            oldCharAlpha = 0f
                        )
                    }
                }
                jobs.add(fadeJob)
                addCharSlotJob(slot, fadeJob)
            } else {
                updated = current.copy(
                    characterKey = input.characterKey,
                    expression = input.expression,
                    oldCharacterKey = null,
                    oldExpression = null,
                    oldCharAlpha = 0f
                )
                state.characters[slot] = updated
            }
        }

        val move = characterSlotMove(updated, input)
        if (move != null) {
            if (durationMs > 0 && input.action != "setpos") {
                val moveJob = coroutineScope.launch {
                    tween(durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            actionX = move.from.x + (move.to.x - move.from.x) * progress,
                            actionY = move.from.y + (move.to.y - move.from.y) * progress
                        )
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(actionX = move.to.x, actionY = move.to.y)
                    }
                }
                jobs.add(moveJob)
                addCharSlotJob(slot, moveJob)
            } else {
                updated = updated.copy(actionX = move.to.x, actionY = move.to.y)
                state.characters[slot] = updated
            }
        }

        val zoom = characterSlotZoom(updated, input)
        if (zoom != null) {
            if (durationMs > 0) {
                val zoomJob = coroutineScope.launch {
                    tween(durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            scaleX = zoom.fromScaleX + (zoom.toScaleX - zoom.fromScaleX) * progress,
                            scaleY = zoom.fromScaleY + (zoom.toScaleY - zoom.fromScaleY) * progress,
                            zoomShiftX = zoom.fromShiftX + (zoom.toShiftX - zoom.fromShiftX) * progress,
                            zoomShiftY = zoom.fromShiftY + (zoom.toShiftY - zoom.fromShiftY) * progress
                        )
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(
                            scaleX = zoom.toScaleX,
                            scaleY = zoom.toScaleY,
                            zoomShiftX = zoom.toShiftX,
                            zoomShiftY = zoom.toShiftY
                        )
                    }
                }
                jobs.add(zoomJob)
                addCharSlotJob(slot, zoomJob)
            } else {
                updated = updated.copy(
                    scaleX = zoom.toScaleX,
                    scaleY = zoom.toScaleY,
                    zoomShiftX = zoom.toShiftX,
                    zoomShiftY = zoom.toShiftY
                )
                state.characters[slot] = updated
            }
        }

        if ((input.action == "shake" || input.action == "shakemove") && durationMs > 0) {
            val power = max(0f, input.power ?: 10f)
            val randomness = (input.randomness ?: 90).coerceIn(0, 100)
            val times = max(1, input.times ?: 10)
            val interval = max(16L, durationMs / times)
            val shakeJob = coroutineScope.launch {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < durationMs) {
                    val roll = Random.nextInt(1, 101)
                    val factor = if (roll < randomness) Random.nextFloat() * 2f - 1f else 0f
                    val cur = state.characters[slot] ?: break
                    state.characters[slot] = cur.copy(
                        shakeOffsetX = factor * power,
                        shakeOffsetY = factor * power
                    )
                    delay(interval)
                }
                val cur = state.characters[slot]
                if (cur != null) {
                    state.characters[slot] = cur.copy(shakeOffsetX = 0f, shakeOffsetY = 0f)
                }
            }
            jobs.add(shakeJob)
            addCharSlotJob(slot, shakeJob)
        }

        if (input.action == "jump" && durationMs > 0) {
            val power = max(0f, input.power ?: 20f)
            val times = max(1, input.times ?: 1)
            val singleJumpMs = max(16L, durationMs / times)
            val jumpJob = coroutineScope.launch {
                repeat(times) {
                    tween(singleJumpMs) { progress ->
                        val cycle = if (progress <= 0.5f) progress / 0.5f else (1f - progress) / 0.5f
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(jumpOffsetY = power * cycle)
                    }
                }
                val cur = state.characters[slot]
                if (cur != null) {
                    state.characters[slot] = cur.copy(jumpOffsetY = 0f)
                }
            }
            jobs.add(jumpJob)
            addCharSlotJob(slot, jumpJob)
        }

        if (input.angle != null) {
            val fromAngle = updated.rotationDeg
            val dir = if (input.inverse == true) -1f else 1f
            val toAngle = dir * (input.angle + 360f * (input.circles ?: 0))
            if (durationMs > 0) {
                val rotateJob = coroutineScope.launch {
                    tween(durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            rotationDeg = fromAngle + (toAngle - fromAngle) * progress
                        )
                    }
                }
                jobs.add(rotateJob)
                addCharSlotJob(slot, rotateJob)
            } else {
                updated = updated.copy(rotationDeg = toAngle)
                state.characters[slot] = updated
            }
        }

        if (input.alphaFrom != null || input.alphaTo != null) {
            val fromA = input.alphaFrom ?: updated.alpha
            val toA = input.alphaTo ?: fromA
            if (durationMs > 0) {
                val alphaJob = coroutineScope.launch {
                    tween(durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(alpha = fromA + (toA - fromA) * progress)
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(alpha = toA)
                    }
                }
                jobs.add(alphaJob)
                addCharSlotJob(slot, alphaJob)
            } else {
                updated = updated.copy(alpha = toA)
                state.characters[slot] = updated
            }
        }

        if (input.block && jobs.isNotEmpty()) {
            jobs.joinAll()
        }
    }

    override suspend fun setCharacterCutin(input: CharacterCutinInput) {
        val widgetId = input.widgetId
        val existing = state.cutinSlots[widgetId]
        val fadeStyle = input.fadeStyle ?: existing?.fadeStyle ?: "fade"
        val isHorizontalExpand = fadeStyle in setOf("horiz_expand_center", "horiz_expand_left2right", "horiz_expand_right2left")
        val isVerticalExpand = fadeStyle in setOf("vert_expand_center", "vert_expand_top2bottom", "vert_expand_bottom2top")

        if (input.characterKey == null && !input.characterMissing) {
            if (existing != null) {
                if (input.fadeMs <= 0) {
                    state.cutinSlots.remove(widgetId)
                    return
                }
                val job = coroutineScope.launch {
                    val startAlpha = existing.alpha
                    val startW = existing.maskW
                    val startH = existing.maskH
                    tween(input.fadeMs) { progress ->
                        if (fadeStyle == "fade") {
                            state.cutinSlots[widgetId] = existing.copy(alpha = startAlpha * (1f - progress))
                        } else if (isHorizontalExpand) {
                            state.cutinSlots[widgetId] = existing.copy(maskW = startW * (1f - progress))
                        } else {
                            state.cutinSlots[widgetId] = existing.copy(maskH = startH * (1f - progress))
                        }
                    }
                    state.cutinSlots.remove(widgetId)
                }
                if (input.block) job.join()
            }
            return
        }

        val maskH = 720f
        val maskW = input.width ?: existing?.targetW ?: 200f
        val rootX = 640f + (input.offsetX ?: existing?.let { it.rootX - 640f } ?: 0f)
        val rootY = 360f - (input.offsetY ?: existing?.let { 360f - it.rootY } ?: 0f)
        val povX = input.povX ?: existing?.povX ?: 0f
        val povY = input.povY ?: existing?.povY ?: 0f
        val zoom = input.zoom ?: existing?.zoom ?: 1f

        if (existing == null) {
            val initialW = if (isHorizontalExpand) 0f else maskW
            val initialH = if (isVerticalExpand) 0f else maskH
            val initialAlpha = if (fadeStyle == "fade") 0f else 1f

            val slotState = CharacterCutinSlotState(
                widgetId = widgetId,
                characterKey = input.characterKey,
                expression = input.expression,
                alpha = initialAlpha,
                rootX = rootX,
                rootY = rootY,
                maskW = initialW,
                maskH = initialH,
                targetW = maskW,
                targetH = maskH,
                povX = povX,
                povY = povY,
                zoom = zoom,
                charSlotX = input.charOffsetX ?: 0f,
                charSlotY = 360f - (input.charOffsetY ?: 0f),
                fadeStyle = fadeStyle
            )
            state.cutinSlots[widgetId] = slotState

            if (input.fadeMs > 0) {
                val job = coroutineScope.launch {
                    tween(input.fadeMs) { progress ->
                        state.cutinSlots[widgetId] = slotState.copy(
                            alpha = if (fadeStyle == "fade") progress else 1f,
                            maskW = if (isHorizontalExpand) maskW * progress else maskW,
                            maskH = if (isVerticalExpand) maskH * progress else maskH
                        )
                    }
                }
                if (input.block) job.join()
            } else {
                state.cutinSlots[widgetId] = slotState.copy(
                    alpha = 1f,
                    maskW = maskW,
                    maskH = maskH
                )
            }
        } else {
            val fromRootX = existing.rootX
            val fromRootY = existing.rootY
            val fromMaskW = existing.maskW
            val fromMaskH = existing.maskH
            val fromPovX = existing.povX
            val fromPovY = existing.povY
            val fromZoom = existing.zoom
            val fromCharSlotX = existing.charSlotX
            val fromCharSlotY = existing.charSlotY

            val targetCharSlotX = input.charOffsetX ?: fromCharSlotX
            val targetCharSlotY = 360f - (input.charOffsetY ?: (360f - fromCharSlotY))

            val isSameChar = existing.characterKey == input.characterKey && existing.expression == input.expression
            val oldKey = if (!isSameChar) existing.characterKey else null
            val oldExpr = if (!isSameChar) existing.expression else null

            val targetSlotState = existing.copy(
                characterKey = input.characterKey,
                expression = input.expression,
                targetW = maskW,
                targetH = maskH,
                fadeStyle = fadeStyle,
                oldCharacterKey = oldKey,
                oldExpression = oldExpr,
                oldCharAlpha = if (!isSameChar) 1f else 0f
            )

            if (input.fadeMs > 0) {
                val job = coroutineScope.launch {
                    tween(input.fadeMs) { progress ->
                        state.cutinSlots[widgetId] = targetSlotState.copy(
                            rootX = fromRootX + (rootX - fromRootX) * progress,
                            rootY = fromRootY + (rootY - fromRootY) * progress,
                            maskW = fromMaskW + (maskW - fromMaskW) * progress,
                            maskH = fromMaskH + (maskH - fromMaskH) * progress,
                            povX = fromPovX + (povX - fromPovX) * progress,
                            povY = fromPovY + (povY - fromPovY) * progress,
                            zoom = fromZoom + (zoom - fromZoom) * progress,
                            charSlotX = fromCharSlotX + (targetCharSlotX - fromCharSlotX) * progress,
                            charSlotY = fromCharSlotY + (targetCharSlotY - fromCharSlotY) * progress,
                            oldCharAlpha = 1f - progress
                        )
                    }
                    state.cutinSlots[widgetId] = targetSlotState.copy(
                        rootX = rootX,
                        rootY = rootY,
                        maskW = maskW,
                        maskH = maskH,
                        povX = povX,
                        povY = povY,
                        zoom = zoom,
                        charSlotX = targetCharSlotX,
                        charSlotY = targetCharSlotY,
                        oldCharacterKey = null,
                        oldExpression = null,
                        oldCharAlpha = 0f
                    )
                }
                if (input.block) job.join()
            } else {
                state.cutinSlots[widgetId] = targetSlotState.copy(
                    rootX = rootX,
                    rootY = rootY,
                    maskW = maskW,
                    maskH = maskH,
                    povX = povX,
                    povY = povY,
                    zoom = zoom,
                    charSlotX = targetCharSlotX,
                    charSlotY = targetCharSlotY,
                    oldCharacterKey = null,
                    oldExpression = null,
                    oldCharAlpha = 0f
                )
            }
        }
    }

    override suspend fun setCurtain(input: CurtainInput) {
        val dir = input.direction
        val target = CurtainVisualState(
            direction = dir,
            fill = input.fillTo,
            alpha = input.alphaTo ?: 1f,
            grad = input.grad
        )
        if (input.fadeMs > 0) {
            val fromFill = input.fillFrom ?: 0f
            val job = coroutineScope.launch {
                delay(input.delayMs)
                tween(input.fadeMs) { progress ->
                    state.curtains[dir] = target.copy(fill = fromFill + (input.fillTo - fromFill) * progress)
                }
            }
            if (input.block) job.join()
        } else {
            state.curtains[dir] = target
        }
    }

    override suspend fun setGridBackground(input: GridBackgroundInput) {
        val target = GridBackgroundVisualState(
            imageKeys = input.imageKeys,
            assetKind = input.assetKind ?: "background",
            layout = input.layout ?: "large",
            alpha = if (input.fadeMs > 0) 0f else 1f,
            scaleX = input.scaleX,
            scaleY = input.scaleY,
            x = input.x,
            y = input.y,
            solidWidths = input.solidWidths,
            solidHeights = input.solidHeights,
            initPositionMode = input.initPositionMode ?: "default"
        )
        state.gridBackground = target
        if (input.fadeMs > 0) {
            val job = coroutineScope.launch {
                tween(input.fadeMs) { progress ->
                    state.gridBackground = target.copy(alpha = progress)
                }
            }
            if (input.block) job.join()
        }
    }

    override suspend fun setLargeImage(input: GridBackgroundInput) {
        setGridBackground(input)
    }

    override suspend fun runCharacterAction(input: CharacterActionInput) {
        val slot = input.slot
        val current = state.characters[slot] ?: return
        cancelCharSlotJobs(slot)

        when (input.type) {
            "move" -> {
                val fromX = current.actionX
                val fromY = current.actionY
                val toX = fromX + input.xOffset
                val toY = fromY + input.yOffset
                val job = coroutineScope.launch {
                    tween(input.durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            actionX = fromX + (toX - fromX) * progress,
                            actionY = fromY + (toY - fromY) * progress
                        )
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(actionX = toX, actionY = toY)
                    }
                }
                addCharSlotJob(slot, job)
                if (input.block && input.durationMs > 0) job.join()
            }
            "jump" -> {
                val fromX = current.actionX
                val fromY = current.actionY
                val toX = fromX + input.xOffset
                val toY = fromY + input.yOffset
                val power = max(0f, input.power)
                val times = max(1, input.times)
                val singleJumpMs = max(16L, input.durationMs / times)

                val job = coroutineScope.launch {
                    launch {
                        repeat(times) {
                            tween(singleJumpMs) { progress ->
                                val cycle = if (progress <= 0.5f) progress / 0.5f else (1f - progress) / 0.5f
                                val cur = state.characters[slot] ?: return@tween
                                state.characters[slot] = cur.copy(jumpOffsetY = power * cycle)
                            }
                        }
                        val cur = state.characters[slot]
                        if (cur != null) {
                            state.characters[slot] = cur.copy(jumpOffsetY = 0f)
                        }
                    }
                    tween(input.durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            actionX = fromX + (toX - fromX) * progress,
                            actionY = fromY + (toY - fromY) * progress
                        )
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(actionX = toX, actionY = toY)
                    }
                }
                addCharSlotJob(slot, job)
                if (input.block && input.durationMs > 0) job.join()
            }
            "shake" -> {
                if (input.stop || input.power <= 0f) {
                    state.characters[slot] = current.copy(shakeOffsetX = 0f, shakeOffsetY = 0f)
                    return
                }
                val power = input.power
                val randomness = input.randomness.toInt().coerceIn(0, 100)
                val times = max(1, input.times)
                val interval = max(16L, input.durationMs / times)

                val job = coroutineScope.launch {
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < input.durationMs) {
                        val roll = Random.nextInt(1, 101)
                        val factor = if (roll < randomness) Random.nextFloat() * 2f - 1f else 0f
                        val cur = state.characters[slot] ?: break
                        state.characters[slot] = cur.copy(
                            shakeOffsetX = factor * power,
                            shakeOffsetY = factor * power
                        )
                        delay(interval)
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(shakeOffsetX = 0f, shakeOffsetY = 0f)
                    }
                }
                addCharSlotJob(slot, job)
                if (input.block && input.durationMs > 0) job.join()
            }
            "zoom" -> {
                val fromScaleX = current.scaleX
                val fromScaleY = current.scaleY
                val toScaleX = input.scaleX ?: fromScaleX
                val toScaleY = input.scaleY ?: fromScaleY
                val fromX = current.actionX
                val fromY = current.actionY
                val toX = fromX + input.xOffset
                val toY = fromY + input.yOffset

                val job = coroutineScope.launch {
                    tween(input.durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            scaleX = fromScaleX + (toScaleX - fromScaleX) * progress,
                            scaleY = fromScaleY + (toScaleY - fromScaleY) * progress,
                            actionX = fromX + (toX - fromX) * progress,
                            actionY = fromY + (toY - fromY) * progress
                        )
                    }
                    val cur = state.characters[slot]
                    if (cur != null) {
                        state.characters[slot] = cur.copy(
                            scaleX = toScaleX,
                            scaleY = toScaleY,
                            actionX = toX,
                            actionY = toY
                        )
                    }
                }
                addCharSlotJob(slot, job)
                if (input.block && input.durationMs > 0) job.join()
            }
            "exit" -> {
                val toX = if (input.direction == "left") -1280f else 1280f
                val fromX = current.actionX
                val fromY = current.actionY
                val toY = fromY + input.yOffset

                val job = coroutineScope.launch {
                    tween(input.durationMs) { progress ->
                        val cur = state.characters[slot] ?: return@tween
                        state.characters[slot] = cur.copy(
                            actionX = fromX + (toX - fromX) * progress,
                            actionY = fromY + (toY - fromY) * progress
                        )
                    }
                    state.characters.remove(slot)
                }
                addCharSlotJob(slot, job)
                if (input.block && input.durationMs > 0) job.join()
            }
        }
    }

    override fun setDialogue(
    speaker: String,
    text: String,
    tagStyles: RichTagStyles?,
    layout: DialogueLayout?
) {
    state.dialogue = DialogueVisualState(
        visible = true,
        speaker = speaker,
        text = text,
        tagStyles = tagStyles ?: state.dialogue.tagStyles,
        layout = layout ?: state.dialogue.layout,
        isExpanded = state.dialogue.isExpanded
    )
}

    override fun hideDialogue() {
        state.dialogue = state.dialogue.copy(visible = false)
    }

    override fun finishDialogueCommand() {
        state.dialogue = state.dialogue.copy(isExpanded = false)
    }

    override suspend fun setImage(key: String, input: BackgroundInput?) {
        val inp = input ?: BackgroundInput()
        val target = BackgroundVisualState(
            textureKey = key,
            alpha = if (inp.fadeMs > 0) 0f else 1f,
            scaleX = inp.scaleX,
            scaleY = inp.scaleY,
            x = inp.x,
            y = inp.y,
            widthMultiplier = inp.width ?: 1f,
            heightMultiplier = inp.height ?: 1f,
            tiled = inp.tiled,
            screenAdapt = inp.screenAdapt
        )
        state.foregroundImage = target
        if (inp.fadeMs > 0) {
            val job = coroutineScope.launch {
                tween(inp.fadeMs) { progress ->
                    state.foregroundImage = target.copy(alpha = progress)
                }
            }
            if (inp.block) job.join()
        }
    }

    override suspend fun setImageRotate(input: ImageRotateInput) {
        val fromAngle = state.imageRotationDeg
        val toAngle = fromAngle + input.angleDeg + (input.circles * 360f) * (if (input.inverse) -1f else 1f)
        val job = coroutineScope.launch {
            tween(input.durationMs) { progress ->
                state.imageRotationDeg = fromAngle + (toAngle - fromAngle) * progress
            }
        }
        if (input.block && input.durationMs > 0) job.join()
    }

    override suspend fun setImageTween(input: ImageTweenInput) {
    val current = state.foregroundImage ?: return
    val sessionId = ++imageTweenSessionId
    val fromX = input.xFrom ?: current.x
    val toX = input.xTo ?: current.x
    val fromY = input.yFrom ?: current.y
    val toY = input.yTo ?: current.y
    val fromScaleX = input.xScaleFrom ?: current.scaleX
    val toScaleX = input.xScaleTo ?: current.scaleX
    val fromScaleY = input.yScaleFrom ?: current.scaleY
    val toScaleY = input.yScaleTo ?: current.scaleY

    val job = coroutineScope.launch {
        tween(input.durationMs, input.ease) { progress ->
            if (imageTweenSessionId != sessionId) return@tween
            state.foregroundImage = current.copy(
                x = fromX + (toX - fromX) * progress,
                y = fromY + (toY - fromY) * progress,
                scaleX = fromScaleX + (toScaleX - fromScaleX) * progress,
                scaleY = fromScaleY + (toScaleY - fromScaleY) * progress
            )
        }
    }
    if (input.block && input.durationMs > 0 && !input.loop) job.join()
}

    override suspend fun setInterlude(input: InterludeInput) {
        val channelId = input.channel
        val jobs = mutableListOf<Job>()

        if (input.clear) {
            val targetChannels = if (channelId < 0) {
                state.interludes.keys.toList()
            } else {
                listOfNotNull(state.interludes[channelId]?.channel)
            }

            for (ch in targetChannels) {
                cancelInterludeJobs(ch)
                val chState = state.interludes[ch] ?: continue
                if (input.templateSizeDurationMs > 0) {
                    val from = input.templateSizeFrom
                    val to = input.templateSizeTo
                    val baseW = chState.baseSizeX
                    val baseH = chState.baseSizeY
                    val clearJob = coroutineScope.launch {
                        tween(input.templateSizeDurationMs) { progress ->
                            val cur = state.interludes[ch] ?: return@tween
                            val factorX = from.x + (to.x - from.x) * progress
                            val factorY = from.y + (to.y - from.y) * progress
                            state.interludes[ch] = cur.copy(
                                maskWidth = baseW * factorX,
                                maskHeight = baseH * factorY
                            )
                        }
                        state.interludes.remove(ch)
                    }
                    jobs.add(clearJob)
                    addInterludeJob(ch, clearJob)
                } else {
                    state.interludes.remove(ch)
                }
            }
            if (input.block && jobs.isNotEmpty()) {
                jobs.joinAll()
            }
            return
        }

        if (channelId < 0) {
            onWarning?.invoke("interlude channel is invalid: $channelId")
            return
        }

        cancelInterludeJobs(channelId)
        val existing = state.interludes[channelId]
        val baseW = if (input.size.x > 0f) input.size.x else (existing?.baseSizeX ?: 200f)
        val baseH = if (input.size.y > 0f) input.size.y else (existing?.baseSizeY ?: 200f)

        var channelState = existing?.copy(
            visible = input.switchOn,
            offsetX = input.offset.x,
            offsetY = input.offset.y,
            baseSizeX = baseW,
            baseSizeY = baseH,
            charName = input.charName.ifEmpty { existing.charName }
        ) ?: InterludeChannelVisualState(
            channel = channelId,
            visible = input.switchOn,
            offsetX = input.offset.x,
            offsetY = input.offset.y,
            baseSizeX = baseW,
            baseSizeY = baseH,
            maskWidth = baseW,
            maskHeight = baseH,
            charName = input.charName
        )

        if (input.templateSizeDurationMs > 0) {
            val from = input.templateSizeFrom
            val to = input.templateSizeTo
            val maskJob = coroutineScope.launch {
                tween(input.templateSizeDurationMs) { progress ->
                    val cur = state.interludes[channelId] ?: return@tween
                    val factorX = from.x + (to.x - from.x) * progress
                    val factorY = from.y + (to.y - from.y) * progress
                    state.interludes[channelId] = cur.copy(
                        maskWidth = baseW * factorX,
                        maskHeight = baseH * factorY
                    )
                }
            }
            jobs.add(maskJob)
            addInterludeJob(channelId, maskJob)
        } else {
            channelState = channelState.copy(maskWidth = baseW, maskHeight = baseH)
        }

        if (input.type != 0) {
            val slot = input.slot.ifEmpty { "m" }
            val existingElem = channelState.elements[slot]
            val initX = input.positionFrom?.x ?: existingElem?.x ?: 0f
            val initY = input.positionFrom?.let { -it.y } ?: existingElem?.y ?: 0f
            val initScaleX = input.scaleFrom?.x ?: existingElem?.scaleX ?: 1f
            val initScaleY = input.scaleFrom?.y ?: existingElem?.scaleY ?: 1f
            val initAlpha = if (input.alphaFrom >= 0f) input.alphaFrom else (existingElem?.alpha ?: 1f)

            var elem = existingElem?.copy(
                name = input.name.ifEmpty { existingElem.name },
                type = input.type,
                avatarCharacterKey = input.avatarCharacterKey ?: existingElem.avatarCharacterKey,
                avatarExpression = input.avatarExpression ?: existingElem.avatarExpression,
                visible = input.switchOn,
                x = initX,
                y = initY,
                scaleX = initScaleX,
                scaleY = initScaleY,
                alpha = initAlpha
            ) ?: InterludeElementVisualState(
                slot = slot,
                name = input.name,
                type = input.type,
                avatarCharacterKey = input.avatarCharacterKey,
                avatarExpression = input.avatarExpression,
                visible = input.switchOn,
                x = initX,
                y = initY,
                scaleX = initScaleX,
                scaleY = initScaleY,
                alpha = initAlpha
            )

            val elemMap = channelState.elements.toMutableMap()
            elemMap[slot] = elem
            channelState = channelState.copy(elements = elemMap)
            state.interludes[channelId] = channelState

            val duration = maxOf(input.durationMs, input.scaleDurationMs, input.alphaDurationMs)
            if (duration > 0) {
                val fromPos = input.positionFrom
                val toPos = input.positionTo
                val fromScale = input.scaleFrom
                val toScale = input.scaleTo
                val startAlpha = if (input.alphaFrom >= 0f) input.alphaFrom else elem.alpha
                val endAlpha = if (input.alphaTo >= 0f) input.alphaTo else 1f

                val elemJob = coroutineScope.launch {
                    tween(duration) { progress ->
                        val curChannel = state.interludes[channelId] ?: return@tween
                        val curElem = curChannel.elements[slot] ?: return@tween

                        val curX = if (fromPos != null && toPos != null) {
                            fromPos.x + (toPos.x - fromPos.x) * progress
                        } else curElem.x

                        val curY = if (fromPos != null && toPos != null) {
                            -(fromPos.y + (toPos.y - fromPos.y) * progress)
                        } else curElem.y

                        val curScaleX = if (fromScale != null && toScale != null) {
                            fromScale.x + (toScale.x - fromScale.x) * progress
                        } else curElem.scaleX

                        val curScaleY = if (fromScale != null && toScale != null) {
                            fromScale.y + (toScale.y - fromScale.y) * progress
                        } else curElem.scaleY

                        val curAlpha = startAlpha + (endAlpha - startAlpha) * progress

                        val updatedElements = curChannel.elements.toMutableMap()
                        updatedElements[slot] = curElem.copy(
                            x = curX,
                            y = curY,
                            scaleX = curScaleX,
                            scaleY = curScaleY,
                            alpha = curAlpha
                        )
                        state.interludes[channelId] = curChannel.copy(elements = updatedElements)
                    }
                }
                jobs.add(elemJob)
                addInterludeJob(channelId, elemJob)
            }
        } else {
            state.interludes[channelId] = channelState
        }

        if (input.block && jobs.isNotEmpty()) {
            jobs.joinAll()
        }
    }

    override suspend fun setLargeBackgroundTween(input: LargeBackgroundTweenInput) {
        val current = state.gridBackground ?: return
        val fromX = input.xFrom ?: current.x
        val toX = input.xTo ?: current.x
        val fromY = input.yFrom ?: current.y
        val toY = input.yTo ?: current.y
        val fromScaleX = input.xScaleFrom ?: current.scaleX
        val toScaleX = input.xScaleTo ?: current.scaleX
        val fromScaleY = input.yScaleFrom ?: current.scaleY
        val toScaleY = input.yScaleTo ?: current.scaleY

        val job = coroutineScope.launch {
            tween(input.durationMs, input.ease ?: "Linear") { progress ->
                state.gridBackground = current.copy(
                    x = fromX + (toX - fromX) * progress,
                    y = fromY + (toY - fromY) * progress,
                    scaleX = fromScaleX + (toScaleX - fromScaleX) * progress,
                    scaleY = fromScaleY + (toScaleY - fromScaleY) * progress
                )
            }
        }
        if (input.block && input.durationMs > 0 && !input.loop) job.join()
    }

    override suspend fun setLargeImageTween(input: LargeBackgroundTweenInput) {
        setLargeBackgroundTween(input)
    }

    override suspend fun showItem(input: ShowItemInput) {
        val target = ShowItemVisualState(
            visible = true,
            key = input.key,
            blackAlpha = input.blackAlpha,
            alpha = if (input.fadeMs > 0) 0f else 1f,
            offsetX = input.offsetX,
            offsetY = input.offsetY
        )
        state.showItem = target
        if (input.fadeMs > 0) {
            val job = coroutineScope.launch {
                tween(input.fadeMs) { progress ->
                    state.showItem = target.copy(alpha = progress)
                }
            }
            if (input.block) job.join()
        }
    }

    override suspend fun showCgItem(input: CgItemInput) {
        val key = input.key
        cgItemJobs[key]?.forEach { it.cancel() }
        val jobs = mutableListOf<Job>()
        cgItemJobs[key] = jobs

        val initialX = input.positionFrom?.x ?: input.positionTo?.x ?: 0f
        val initialY = input.positionFrom?.y ?: input.positionTo?.y ?: 0f
        val initialScale = input.scaleFrom
        val initialAlpha = input.colorFrom?.a ?: input.alphaFrom
        val initialTint = input.colorFrom

        val targetSlot = CgItemVisualSlot(
            key = key,
            assetKey = input.assetKey,
            x = initialX,
            y = initialY,
            scale = initialScale,
            alpha = initialAlpha,
            tintRgb = initialTint,
            rotationDeg = if (input.rotationFrom <= 0f) input.rotationFrom else 0f,
            width = input.width,
            height = input.height
        )
        state.cgItems[key] = targetSlot

        if (input.positionFrom != null && input.positionTo != null) {
            val fromP = input.positionFrom
            val toP = input.positionTo
            val pJob = coroutineScope.launch {
                if (input.positionDelayMs > 0) delay(input.positionDelayMs)
                tween(input.positionDurationMs, input.ease) { progress ->
                    val cur = state.cgItems[key] ?: return@tween
                    state.cgItems[key] = cur.copy(
                        x = fromP.x + (toP.x - fromP.x) * progress,
                        y = fromP.y + (toP.y - fromP.y) * progress
                    )
                }
            }
            jobs.add(pJob)
        }

        if (input.scaleFrom != input.scaleTo) {
            val fromS = input.scaleFrom
            val toS = input.scaleTo
            val sJob = coroutineScope.launch {
                if (input.scaleDelayMs > 0) delay(input.scaleDelayMs)
                tween(input.scaleDurationMs, input.ease) { progress ->
                    val cur = state.cgItems[key] ?: return@tween
                    state.cgItems[key] = cur.copy(scale = fromS + (toS - fromS) * progress)
                }
            }
            jobs.add(sJob)
        }

        if (input.colorFrom != null && input.colorTo != null) {
            val fromC = input.colorFrom
            val toC = input.colorTo
            val cJob = coroutineScope.launch {
                if (input.alphaDelayMs > 0) delay(input.alphaDelayMs)
                tween(input.alphaDurationMs, input.ease) { progress ->
                    val cur = state.cgItems[key] ?: return@tween
                    val r = fromC.r + (toC.r - fromC.r) * progress
                    val g = fromC.g + (toC.g - fromC.g) * progress
                    val b = fromC.b + (toC.b - fromC.b) * progress
                    val a = fromC.a + (toC.a - fromC.a) * progress
                    state.cgItems[key] = cur.copy(
                        alpha = a,
                        tintRgb = ColorRgba(r, g, b, a)
                    )
                }
            }
            jobs.add(cJob)
        } else if (input.alphaFrom != input.alphaTo) {
            val fromA = input.alphaFrom
            val toA = input.alphaTo
            val aJob = coroutineScope.launch {
                if (input.alphaDelayMs > 0) delay(input.alphaDelayMs)
                tween(input.alphaDurationMs, input.ease) { progress ->
                    val cur = state.cgItems[key] ?: return@tween
                    state.cgItems[key] = cur.copy(alpha = fromA + (toA - fromA) * progress)
                }
            }
            jobs.add(aJob)
        }

        if (input.rotationFrom <= 0f && input.rotationDurationMs > 0) {
            val fromR = input.rotationFrom
            val toR = input.rotationTo
            val rJob = coroutineScope.launch {
                tween(input.rotationDurationMs, input.ease) { progress ->
                    val cur = state.cgItems[key] ?: return@tween
                    state.cgItems[key] = cur.copy(rotationDeg = fromR + (toR - fromR) * progress)
                }
            }
            jobs.add(rJob)
        } else {
            val cur = state.cgItems[key]
            if (cur != null) {
                state.cgItems[key] = cur.copy(rotationDeg = input.rotationTo)
            }
        }

        if (input.block) {
            jobs.joinAll()
        }
    }

    override suspend fun setSticker(input: StickerInput) {
        val target = StickerVisualState(
            id = input.id,
            visible = true,
            text = input.text,
            x = input.x,
            y = input.y,
            sizePx = input.sizePx,
            widthPx = input.widthPx,
            alignment = input.alignment,
            alpha = 1f
        )
        state.stickers[input.id] = target
        input.onTypingComplete?.invoke()
    }

    override suspend fun stickerTween(input: StickerTweenInput) {
        val id = input.id
        val queue = stickerTweenQueues.getOrPut(id) { mutableListOf() }
        if (input.position != null) {
            queue.add(StickerTweenStep.Position(input.position.durationMs, input.join, input.position.from, input.position.to))
        }
        if (input.rotation != null) {
            queue.add(StickerTweenStep.Rotation(input.rotation.durationMs, input.join, input.rotation.from, input.rotation.to))
        }
        if (input.alpha != null) {
            queue.add(StickerTweenStep.Alpha(input.alpha.durationMs, input.join, input.alpha.from, input.alpha.to))
        }

        if (!input.isend) return

        stickerTweenQueues.remove(id)
        val sticker = state.stickers[id] ?: return
        if (queue.isEmpty()) return

        cancelStickerJobs(id)
        val sessionId = (stickerTweenSessionIds[id] ?: 0) + 1
        stickerTweenSessionIds[id] = sessionId

        val windows = mutableListOf<TimedStickerStep>()
        var segmentStart = 0L
        var segmentLongest = 0L
        var open = false

        for (step in queue) {
            if (!open || !step.join) {
                segmentStart += segmentLongest
                segmentLongest = 0L
                open = true
            }
            windows.add(TimedStickerStep(startMs = segmentStart, step = step))
            segmentLongest = max(segmentLongest, step.durationMs)
        }
        val totalMs = segmentStart + segmentLongest

        fun applyStep(step: StickerTweenStep, progress: Float) {
            val cur = state.stickers[id] ?: return
            when (step) {
                is StickerTweenStep.Position -> {
                    state.stickers[id] = cur.copy(
                        x = step.from.x + (step.to.x - step.from.x) * progress,
                        y = step.from.y + (step.to.y - step.from.y) * progress
                    )
                }
                is StickerTweenStep.Alpha -> {
                    state.stickers[id] = cur.copy(
                        alpha = step.from + (step.to - step.from) * progress
                    )
                }
                is StickerTweenStep.Rotation -> {
                    state.stickers[id] = cur.copy(
                        rotationDeg = step.from + (step.to - step.from) * progress
                    )
                }
            }
        }

        for (w in windows) {
            applyStep(w.step, 0f)
        }

        val tweenJob = coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                if (stickerTweenSessionIds[id] != sessionId) return@launch
                val elapsed = System.currentTimeMillis() - startTime
                for (w in windows) {
                    val local = if (w.step.durationMs <= 0L) 1f else {
                        ((elapsed - w.startMs).toFloat() / w.step.durationMs).coerceIn(0f, 1f)
                    }
                    applyStep(w.step, local)
                }
                if (elapsed >= totalMs) break
                delay(16)
            }
            if (stickerTweenSessionIds[id] == sessionId) {
                for (w in windows) {
                    applyStep(w.step, 1f)
                }
            }
        }
        addStickerJob(id, tweenJob)
    }

    override suspend fun setSubtitle(input: SubtitleInput) {
        state.subtitle = SubtitleVisualState(
            visible = true,
            text = input.text,
            x = input.x,
            y = input.y,
            sizePx = input.sizePx,
            widthPx = input.widthPx,
            alignment = input.alignment,
            alpha = 1f
        )
        input.onTypingComplete?.invoke()
    }

    override suspend fun setTimerSticker(input: TimerStickerInput) {
        timerCountJob?.cancel()
        timerFadeJob?.cancel()

        val capSeconds = input.limitSeconds?.takeIf { it > 0 }
        val startFormatted = formatTimer(0L)

        val target = TimerVisualState(
            visible = true,
            timeFormatted = startFormatted,
            alpha = input.fromAlpha,
            x = input.x,
            y = input.y,
            sizePx = input.sizePx,
            widthPx = input.widthPx
        )
        state.timerSticker = target

        if (capSeconds != null) {
            val startMs = System.currentTimeMillis()
            timerCountJob = coroutineScope.launch {
                while (isActive) {
                    val elapsedSeconds = minOf(
                        capSeconds.toLong(),
                        maxOf(0L, (System.currentTimeMillis() - startMs) / 1000L)
                    )
                    state.timerSticker = state.timerSticker.copy(
                        timeFormatted = formatTimer(elapsedSeconds)
                    )
                    if (elapsedSeconds >= capSeconds.toLong()) {
                        break
                    }
                    delay(200)
                }
            }
        }

        val durationMs = if (input.durationMs > 0L) input.durationMs else 130L
        timerFadeJob = coroutineScope.launch {
            tween(durationMs) { progress ->
                state.timerSticker = state.timerSticker.copy(
                    alpha = input.fromAlpha + (input.toAlpha - input.fromAlpha) * progress
                )
            }
            state.timerSticker = state.timerSticker.copy(alpha = input.toAlpha)
        }
    }

    override suspend fun shakeCamera(input: CameraShakeInput) {
        activeShakeJob?.cancel()
        if (input.stop || input.durationMs <= 0) {
            state.camera = state.camera.copy(shakeX = 0f, shakeY = 0f)
            return
        }
        val path = buildShakePath(input.durationMs, input)
        activeShakeJob = coroutineScope.launch {
            tween(input.durationMs) { progress ->
                val offset = sampleShakePath(path, progress)
                state.camera = state.camera.copy(shakeX = offset.x, shakeY = offset.y)
            }
            state.camera = state.camera.copy(shakeX = 0f, shakeY = 0f)
        }
        if (input.block) activeShakeJob?.join()
    }

    override suspend fun showDecision(options: List<String>, values: List<Int>): DecisionSelection {
        val deferred = CompletableDeferred<DecisionSelection>()
        pendingDecisionDeferred = deferred

        state.decision = DecisionVisualState(
            visible = true,
            options = options,
            values = values,
            onSelect = { index ->
                state.decision = state.decision.copy(visible = false)
                val value = values.getOrNull(index) ?: (index + 1)
                deferred.complete(DecisionSelection(index, value))
            }
        )

        return deferred.await().also {
            pendingDecisionDeferred = null
        }
    }

    override fun stopVideo() {
        activeVideoDeferred?.complete(Unit)
        state.video = VideoVisualState(visible = false, url = "")
    }
}