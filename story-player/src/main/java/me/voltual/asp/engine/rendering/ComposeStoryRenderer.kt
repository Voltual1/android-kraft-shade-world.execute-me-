package me.voltual.asp.engine.rendering

import kotlinx.coroutines.*
import me.voltual.asp.Context
import me.voltual.asp.engine.*
import me.voltual.asp.engine.rendering.core.buildShakePath
import me.voltual.asp.engine.rendering.core.dotweenEaseCurve
import me.voltual.asp.engine.rendering.core.sampleShakePath

class ComposeStoryRenderer(
    private val context: Context,
    val state: StoryRendererState = StoryRendererState(),
    private val coroutineScope: CoroutineScope,
    private val onWarning: ((String) -> Unit)? = null
) : StoryRenderer {

    private var activeShakeJob: Job? = null
    private var pendingDecisionDeferred: CompletableDeferred<DecisionSelection>? = null
    private var isTextTypingComplete = true

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

    override suspend fun clearCgItems(key: String?, fadeMs: Long, ease: String, block: Boolean) {}
    override suspend fun clearAvgDisplays() {}
    override suspend fun clearAnimTexts() {}

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
        val fromAmount = input.from ?: state.focus.amount
        val toAmount = input.to
        val durationMs = input.durationMs

        state.focus = state.focus.copy(
            amount = fromAmount,
            targetType = input.type,
            targetId = input.id
        )

        val job = coroutineScope.launch {
            tween(durationMs) { progress ->
                state.focus = state.focus.copy(amount = fromAmount + (toAmount - fromAmount) * progress)
            }
            state.focus = state.focus.copy(amount = toAmount)
        }
        if (input.block && durationMs > 0) job.join()
    }

    override fun setFocusParam(input: FocusParamInput) {
        state.focus = state.focus.copy(
            blur = input.blur,
            colorMode = input.color
        )
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
            val current = state.stickers[id] ?: return
            if (fadeMs <= 0) {
                state.stickers.remove(id)
                return
            }
            coroutineScope.launch {
                val startAlpha = current.alpha
                tween(fadeMs) { progress ->
                    state.stickers[id] = current.copy(alpha = startAlpha * (1f - progress))
                }
                state.stickers.remove(id)
            }
        } else {
            clearStickers(fadeMs)
        }
    }

    override suspend fun clearStickers(fadeMs: Long) {
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

    override suspend fun clearSpellStickers() {}

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
        state.timerSticker = state.timerSticker.copy(visible = false)
    }

    override suspend fun clearCharacterCutin(widgetId: String?) {}
    override suspend fun clearInterludes() {}

    override fun destroy() {
        activeShakeJob?.cancel()
        pendingDecisionDeferred?.cancel()
        state.dialogue = DialogueVisualState(visible = false)
        state.decision = DecisionVisualState(visible = false)
        state.video = VideoVisualState(visible = false)
        state.characters.clear()
        state.curtains.clear()
        state.stickers.clear()
    }

    override fun finishTextTyping(): Boolean {
        if (!isTextTypingComplete) {
            isTextTypingComplete = true
            return true
        }
        return false
    }

    override suspend fun playVideo(url: String) {
        state.video = VideoVisualState(visible = true, url = url)
    }

    override suspend fun setAnimText(input: AnimTextInput) {}
    override suspend fun setAvgDisplay(input: AvgDisplayInput) {}

    override suspend fun setBackground(key: String, input: BackgroundInput?) {
        val inp = input ?: BackgroundInput()
        val target = BackgroundVisualState(
            textureKey = key,
            alpha = if (inp.fadeMs > 0) 0f else 1f,
            scaleX = inp.scaleX,
            scaleY = inp.scaleY,
            x = inp.x,
            y = inp.y,
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
        val target = BlockerVisualState(
            visible = input.to.a > 0f,
            color = input.to,
            style = input.style,
            image = input.image,
            inverse = input.inverse,
            alpha = if (input.fadeMs > 0) input.from.a else input.to.a,
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
                        visible = curAlpha > 0f
                    )
                }
            }
            if (input.block) job.join()
        }
    }

    override suspend fun setCharacter(input: CharacterSlotInput) {
        val slot = input.slot
        val current = state.characters[slot] ?: CharacterVisualSlot(slot = slot)
        val target = current.copy(
            characterKey = input.characterKey ?: current.characterKey,
            expression = input.expression ?: current.expression,
            alpha = input.alphaTo ?: (if ((input.durationMs ?: 0L) > 0) 0f else 1f),
            scaleX = input.scaleX ?: current.scaleX,
            scaleY = input.scaleY ?: current.scaleY,
            x = input.positionTo?.x ?: current.x,
            y = input.positionTo?.y ?: current.y,
            dimmed = input.dimmed
        )
        state.characters[slot] = target
        val durationMs = input.durationMs ?: 0L
        if (durationMs > 0 && (input.alphaFrom != null || input.alphaTo != null)) {
            val fromA = input.alphaFrom ?: current.alpha
            val toA = input.alphaTo ?: 1f
            val job = coroutineScope.launch {
                tween(durationMs) { progress ->
                    state.characters[slot] = target.copy(alpha = fromA + (toA - fromA) * progress)
                }
            }
            if (input.block) job.join()
        }
    }

    override suspend fun setCharacterCutin(input: CharacterCutinInput) {}

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
        val fromX = current.x
        val fromY = current.y
        val toX = fromX + input.xOffset
        val toY = fromY + input.yOffset

        val job = coroutineScope.launch {
            tween(input.durationMs) { progress ->
                state.characters[slot] = current.copy(
                    x = fromX + (toX - fromX) * progress,
                    y = fromY + (toY - fromY) * progress
                )
            }
        }
        if (input.block && input.durationMs > 0) job.join()
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
            tagStyles = tagStyles,
            layout = layout,
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

    override suspend fun setInterlude(input: InterludeInput) {}

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

    override suspend fun showCgItem(input: CgItemInput) {}

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

    override suspend fun stickerTween(input: StickerTweenInput) {}
    override suspend fun setSpellSticker(input: SpellStickerInput) {}
    override suspend fun hideSpellSticker(id: String) {}

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
        state.timerSticker = TimerVisualState(
            visible = true,
            timeFormatted = "00:00:00",
            alpha = input.toAlpha,
            x = input.x,
            y = input.y,
            sizePx = input.sizePx,
            widthPx = input.widthPx
        )
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
        state.video = VideoVisualState(visible = false, url = "")
    }
}