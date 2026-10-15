package me.voltual.asp.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StoryRuntime(
    val context: StoryContext,
    private val scope: CoroutineScope
) {
    private val parsedLines: List<ParsedLine> = StoryParser.parseScript(context.scriptText)

    private val _state = MutableStateFlow(StoryDisplayState(totalLines = parsedLines.size))
    val state: StateFlow<StoryDisplayState> = _state.asStateFlow()

    private var currentIndex = 0
    private var isWaitingForUserClick = false
    private var delayJob: Job? = null
    private var typingJob: Job? = null
    private var shakeJob: Job? = null
    private var cameraEffectJob: Job? = null
    private var animTextJob: Job? = null
    private var selectedPredicateValue: String? = null

    var onPlayMusicListener: ((key: String, volume: Float) -> Unit)? = null
    var onStopMusicListener: ((fadeTime: Float) -> Unit)? = null
    var onPlaySoundListener: ((key: String) -> Unit)? = null

    fun start() {
        currentIndex = 0
        scope.launch {
            runLoop()
        }
    }

    private suspend fun runLoop() {
        while (currentIndex < parsedLines.size && scope.isActive) {
            val line = parsedLines[currentIndex]
            _state.update { it.copy(currentLineIndex = currentIndex) }

            executeLine(line)

            if (isWaitingForUserClick) {
                if (_state.value.playbackMode == PlaybackMode.SKIP) {
                    isWaitingForUserClick = false
                    delay(50)
                } else if (_state.value.playbackMode == PlaybackMode.AUTO) {
                    val autoDelay = (line as? ParsedDialogueLine)?.text?.length?.times(80L)?.coerceAtLeast(1200L) ?: 1500L
                    delay(autoDelay)
                    isWaitingForUserClick = false
                } else {
                    while (isWaitingForUserClick && _state.value.playbackMode == PlaybackMode.MANUAL && scope.isActive) {
                        delay(50)
                    }
                }
            }
            currentIndex++
        }

        _state.update { it.copy(isEnded = true) }
    }

    private suspend fun executeLine(line: ParsedLine) {
        when (line) {
            is ParsedNarrationLine -> {
                showDialogue(speaker = null, text = line.text)
                waitForClick()
            }
            is ParsedDialogueLine -> {
                showDialogue(speaker = line.speaker, text = line.text)
                waitForClick()
            }
            is ParsedCommandLine -> {
                StoryCommandRegistry.execute(line, this)
            }
        }
    }

    fun onUserClick() {
        if (_state.value.dialogue.isTyping) {
            typingJob?.cancel()
            _state.update {
                it.copy(
                    dialogue = it.dialogue.copy(
                        text = it.dialogue.fullText,
                        isTyping = false
                    )
                )
            }
            return
        }

        if (_state.value.decision.isVisible) return

        if (isWaitingForUserClick) {
            isWaitingForUserClick = false
        }
    }

    fun selectDecision(option: DecisionOption) {
        selectedPredicateValue = option.value ?: option.text
        _state.update { it.copy(decision = DecisionState(isVisible = false)) }
        isWaitingForUserClick = false
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _state.update { it.copy(playbackMode = mode) }
        if (mode != PlaybackMode.MANUAL) {
            isWaitingForUserClick = false
        }
    }

    internal fun showDialogue(speaker: String?, text: String) {
        typingJob?.cancel()
        _state.update {
            it.copy(
                dialogue = DialogueState(
                    speaker = speaker,
                    text = "",
                    isTyping = true,
                    fullText = text
                )
            )
        }

        typingJob = scope.launch {
            if (_state.value.playbackMode == PlaybackMode.SKIP) {
                _state.update { it.copy(dialogue = it.dialogue.copy(text = text, isTyping = false)) }
                return@launch
            }

            for (i in 1..text.length) {
                val currentSub = text.substring(0, i)
                _state.update { it.copy(dialogue = it.dialogue.copy(text = currentSub)) }
                delay(30)
            }
            _state.update { it.copy(dialogue = it.dialogue.copy(isTyping = false)) }
        }
    }

    internal fun setBackground(key: String, duration: Float, x: Float = 0f, y: Float = 0f, scale: Float = 1f) {
        _state.update {
            it.copy(
                background = BackgroundState(
                    imageKey = key,
                    x = x,
                    y = y,
                    scale = scale,
                    duration = duration
                )
            )
        }
    }

    internal fun updateCharacters(slot1: String?, slot2: String?, focusSlot: Int) {
        _state.update {
            it.copy(
                characterSlot1 = CharacterSlotState(characterRef = slot1, isFocused = focusSlot == 1),
                characterSlot2 = CharacterSlotState(characterRef = slot2, isFocused = focusSlot == 2)
            )
        }
    }

    internal fun setBlocker(
        r: Float, g: Float, b: Float, a: Float,
        duration: Float, isBlocking: Boolean,
        style: String = "default",
        inverse: Boolean = false
    ) {
        _state.update {
            it.copy(
                blocker = BlockerState(
                    r = r, g = g, b = b, alpha = a,
                    duration = duration, style = style, inverse = inverse
                )
            )
        }
        if (isBlocking && duration > 0f && _state.value.playbackMode != PlaybackMode.SKIP) {
            scope.launch {
                delay((duration * 1000).toLong())
            }
        }
    }

    internal suspend fun setCameraEffect(
        effect: String,
        amount: Float,
        duration: Float,
        isBlocking: Boolean,
        keep: Boolean,
        initAmount: Float?
    ) {
        cameraEffectJob?.cancel()
        val durationMs = (duration * 1000).toLong()

        if (effect.equals("Colorinverse", ignoreCase = true)) {
            _state.update { it.copy(cameraEffect = it.cameraEffect.copy(inverseAmount = amount)) }
            return
        }

        if (effect.equals("Grayscale", ignoreCase = true)) {
            val fromAmount = initAmount ?: _state.value.cameraEffect.grayAmount
            if (durationMs <= 0) {
                _state.update { it.copy(cameraEffect = it.cameraEffect.copy(grayAmount = if (keep) amount else 0f)) }
                return
            }

            val job = scope.launch {
                val startTime = System.currentTimeMillis()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    val current = fromAmount + (amount - fromAmount) * progress
                    _state.update { it.copy(cameraEffect = it.cameraEffect.copy(grayAmount = current)) }
                    if (progress >= 1f) break
                    delay(16)
                }
                _state.update { it.copy(cameraEffect = it.cameraEffect.copy(grayAmount = if (keep) amount else 0f)) }
            }
            cameraEffectJob = job
            if (isBlocking && _state.value.playbackMode != PlaybackMode.SKIP) {
                job.join()
            }
        }
    }

    internal fun triggerCameraShake(
        duration: Float,
        xStrength: Float,
        yStrength: Float,
        vibrato: Int = 10,
        randomness: Float = 90f,
        fadeOut: Boolean = true,
        stop: Boolean = false
    ) {
        shakeJob?.cancel()
        if (stop || duration <= 0f) {
            _state.update { it.copy(cameraEffect = it.cameraEffect.copy(shakeOffsetX = 0f, shakeOffsetY = 0f)) }
            return
        }

        val durationMs = (duration * 1000).toLong()
        val path = StoryShakePath.buildShakePath(
            durationMs = durationMs,
            input = ShakePathInput(
                fadeOut = fadeOut,
                randomness = randomness,
                vibrato = vibrato,
                xStrength = xStrength,
                yStrength = yStrength
            )
        )

        shakeJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val (offsetX, offsetY) = StoryShakePath.sampleShakePath(path, progress)
                _state.update {
                    it.copy(cameraEffect = it.cameraEffect.copy(shakeOffsetX = offsetX, shakeOffsetY = offsetY))
                }
                if (progress >= 1f) break
                delay(16)
            }
            _state.update { it.copy(cameraEffect = it.cameraEffect.copy(shakeOffsetX = 0f, shakeOffsetY = 0f)) }
        }
    }

    internal suspend fun showAnimText(
        templateName: String,
        content: String,
        posX: Float,
        posY: Float,
        isBlocking: Boolean
    ) {
        animTextJob?.cancel()
        val (main, sub) = StorySplitContent.parse(content)
        val animDuration = 5000L

        _state.update {
            it.copy(
                animText = AnimTextState(
                    isVisible = true,
                    mainText = main,
                    subText = sub,
                    positionX = posX,
                    positionY = posY,
                    durationMs = animDuration
                )
            )
        }

        val job = scope.launch {
            delay(animDuration)
            _state.update { it.copy(animText = it.animText.copy(isVisible = false)) }
        }
        animTextJob = job

        if (isBlocking && _state.value.playbackMode != PlaybackMode.SKIP) {
            job.join()
        }
    }

    internal suspend fun setSpellSticker(
        id: String,
        action: String,
        style: String,
        content: String,
        x: Float,
        y: Float,
        scaleX: Float,
        scaleY: Float,
        angle: Float,
        alpha: Float,
        isBlocking: Boolean
    ) {
        if (action.equals("hide", ignoreCase = true)) {
            _state.update {
                val stickers = it.spellStickers.toMutableMap()
                stickers.remove(id)
                it.copy(spellStickers = stickers)
            }
            return
        }

        val defaultTexts = if (style.equals("fire", ignoreCase = true)) {
            Pair("夜雪无痕，噤声。", "Ночной снег без следа, молчи.")
        } else {
            Pair("不是灾异，而是目光。", "er eigi ógæfa, heldr augna máttur.")
        }

        val parsed = StorySplitContent.parse(content)
        val main = parsed.first.ifEmpty { defaultTexts.first }
        val sub = parsed.second.ifEmpty { defaultTexts.second }

        val item = SpellStickerItem(
            id = id,
            style = style,
            mainText = main,
            subText = sub,
            x = x,
            y = y,
            scaleX = scaleX,
            scaleY = scaleY,
            angle = angle,
            alpha = alpha,
            isVisible = true
        )

        _state.update {
            val stickers = it.spellStickers.toMutableMap()
            stickers[id] = item
            it.copy(spellStickers = stickers)
        }

        if (isBlocking) {
            waitForClick()
        }
    }

    internal fun clearSpellStickers(isBlocking: Boolean) {
        _state.update { it.copy(spellStickers = emptyMap()) }
        if (isBlocking) {
            waitForClick()
        }
    }

    internal fun showDecision(options: List<String>, values: List<String>) {
        val decisionOptions = options.mapIndexed { index, opt ->
            DecisionOption(text = opt, value = values.getOrNull(index))
        }
        _state.update {
            it.copy(decision = DecisionState(isVisible = true, options = decisionOptions))
        }
        waitForClick()
    }

    internal fun applyPredicate(references: List<String>) {
        if (selectedPredicateValue != null && references.isNotEmpty()) {
            val matched = references.contains(selectedPredicateValue)
            if (!matched) {
                // 跳过分支代码段处理
            }
        }
    }

    internal fun delay(seconds: Float) {
        if (_state.value.playbackMode == PlaybackMode.SKIP) return
        delayJob = scope.launch {
            delay((seconds * 1000).toLong())
        }
    }

    internal fun playMusic(key: String, volume: Float) {
        onPlayMusicListener?.invoke(key, volume)
    }

    internal fun stopMusic(fadeTime: Float) {
        onStopMusicListener?.invoke(fadeTime)
    }

    internal fun playSound(key: String) {
        onPlaySoundListener?.invoke(key)
    }

    internal fun onUnhandledCommand(line: ParsedCommandLine) {}

    private fun waitForClick() {
        isWaitingForUserClick = true
    }
}