package me.voltual.asp.engine.rendering

import kotlinx.coroutines.*
import me.voltual.asp.Context
import me.voltual.asp.engine.*
import me.voltual.asp.engine.rendering.core.buildShakePath
import me.voltual.asp.engine.rendering.core.dotweenEaseCurve
import me.voltual.asp.engine.rendering.core.sampleShakePath
import kotlin.math.max

class ComposeStoryRenderer(
    private val context: Context,
    val state: StoryRendererState = StoryRendererState(),
    private val coroutineScope: CoroutineScope,
    private val onWarning: ((String) -> Unit)? = null
) : StoryRenderer {

    private var activeShakeJob: Job? = null
    private var pendingDecisionDeferred: CompletableDeferred<DecisionSelection>? = null
    private var isTextTypingComplete = true
    private val cgItemJobs = mutableMapOf<String, MutableList<Job>>()

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

    override suspend fun clearCharacterCutin(widgetId: String?) {
        if (widgetId == null) {
            state.cutinSlots.clear()
        } else {
            state.cutinSlots.remove(widgetId)
        }
    }

    override suspend fun clearInterludes() {}

    override fun destroy() {
        activeShakeJob?.cancel()
        pendingDecisionDeferred?.cancel()
        cgItemJobs.values.flatten().forEach { it.cancel() }
        cgItemJobs.clear()
        state.dialogue = DialogueVisualState(visible = false)
        state.decision = DecisionVisualState(visible = false)
        state.video = VideoVisualState(visible = false)
        state.characters.clear()
        state.curtains.clear()
        state.cutinSlots.clear()
        state.cgItems.clear()
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

        // Position track
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

        // Scale track
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

        // Color/Alpha track
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

        // Rotation track
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