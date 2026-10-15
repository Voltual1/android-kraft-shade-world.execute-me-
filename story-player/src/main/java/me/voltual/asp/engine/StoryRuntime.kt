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
    private var selectedPredicateValue: String? = null

    var onPlayMusicListener: ((key: String, volume: Float) -> Unit)? = null
    var onStopMusicListener: ((fadeTime: Float) -> Unit)? = null
    var onPlaySoundListener: ((key: String) -> Unit)? = null
    var onCameraShakeListener: ((duration: Float, x: Float, y: Float) -> Unit)? = null

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

    internal fun setBlocker(r: Float, g: Float, b: Float, a: Float, duration: Float, isBlocking: Boolean) {
        _state.update {
            it.copy(
                blocker = BlockerState(r = r, g = g, b = b, alpha = a, duration = duration)
            )
        }
        if (isBlocking && duration > 0f && _state.value.playbackMode != PlaybackMode.SKIP) {
            scope.launch {
                delay((duration * 1000).toLong())
            }
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
                // 跳转或跳过逻辑
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

    internal fun triggerCameraShake(duration: Float, x: Float, y: Float) {
        onCameraShakeListener?.invoke(duration, x, y)
    }

    internal fun onUnhandledCommand(line: ParsedCommandLine) {}

    private fun waitForClick() {
        isWaitingForUserClick = true
    }
}