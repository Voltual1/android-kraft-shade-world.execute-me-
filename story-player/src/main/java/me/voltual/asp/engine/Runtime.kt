package me.voltual.asp.engine

import me.voltual.asp.engine.log.RuntimeDecisionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.voltual.asp.Context
import me.voltual.asp.engine.log.parseDecision
import me.voltual.asp.engine.log.parsePredicateReferences
import me.voltual.asp.engine.log.passesGate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

private enum class CommandResult {
    CONTINUE,
    WAIT_INPUT
}

data class AutoSpeed(
    val animateRatio: Float,
    val autoWaitBaseTime: Float,
    val autoWaitTimePerText: Float,
    val typeWriterDelayMs: Long
)

val BUTTON_AUTO_SPEEDS = listOf(
    AutoSpeed(animateRatio = 1f, autoWaitBaseTime = 1.5f, autoWaitTimePerText = 0.03f, typeWriterDelayMs = 40L),
    AutoSpeed(animateRatio = 1f, autoWaitBaseTime = 0.5f, autoWaitTimePerText = 0.015f, typeWriterDelayMs = 10L),
    AutoSpeed(animateRatio = 1f, autoWaitBaseTime = 0.25f, autoWaitTimePerText = 0.01f, typeWriterDelayMs = 10L)
)

val QUICK_AUTO_SPEEDS = listOf(
    AutoSpeed(animateRatio = 0f, autoWaitBaseTime = 0.2f, autoWaitTimePerText = 0f, typeWriterDelayMs = 10L),
    AutoSpeed(animateRatio = 0f, autoWaitBaseTime = 0.1f, autoWaitTimePerText = 0f, typeWriterDelayMs = 5L),
    AutoSpeed(animateRatio = 0f, autoWaitBaseTime = 0.05f, autoWaitTimePerText = 0f, typeWriterDelayMs = 2L),
    AutoSpeed(animateRatio = 0f, autoWaitBaseTime = 0.025f, autoWaitTimePerText = 0f, typeWriterDelayMs = 1L)
)

const val TYPE_WRITER_ORIGIN_DELAY_MS: Long = 40L

private val BUILTIN_COMMAND_NAMES = listOf(
    "endtip", "dialog", "aside", "delay", "video", "skipnode", "skiptothis",
    "background", "backgroundtween", "gridbg", "verticalbg", "largebg",
    "largeimg", "image", "showitem", "hideitem", "imagetween", "imagerotate",
    "largebgtween", "largeimgtween", "blocker", "curtain", "charslot",
    "character", "characteraction", "charactercutin", "interlude", "animtext",
    "avgdisplay", "playmusic", "stopmusic", "musicvolume", "playsound",
    "stopsound", "soundvolume", "cameraeffect", "camerashake", "focusout",
    "focusparam", "multiline", "subtitle", "sticker", "stickertween",
    "timersticker", "timerclear", "stickerclear", "spellsticker",
    "spellstickerclear", "theater", "decision", "predicate", "bgeffect",
    "cgitem", "effect", "hidecgitem"
)

private val UNSUPPORTED_AD8_COMMANDS = setOf("bgeffect", "effect")

private fun clamp(value: Float, min: Float, max: Float): Float = min(max, max(min, value))

private fun toOptionalNumber(value: Any?): Float? {
    if (value is Number) return value.toFloat()
    if (value is Boolean) return if (value) 1f else 0f
    if (value is String) {
        val trimmed = value.trim()
        if (Regex("""^[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?$""").matches(trimmed)) {
            return trimmed.toFloatOrNull()
        }
    }
    return null
}

private fun toNumber(value: Any?, fallback: Float = 0f): Float = toOptionalNumber(value) ?: fallback

private fun toBoolean(value: Any?, fallback: Boolean = false): Boolean {
    if (value is Boolean) return value
    if (value is String) {
        if (value.equals("true", ignoreCase = true)) return true
        if (value.equals("false", ignoreCase = true)) return false
    }
    return fallback
}

/** Banker's rounding / round half to even, exactly matching System.Convert.ToInt32(double) in C# */
fun roundHalfToEven(value: Double): Int = Math.rint(value).toInt()

private fun toNativeIntParam(value: Any?, fallback: Int): Int {
    if (value is Boolean) return if (value) 1 else 0
    if (value == "true") return 1
    if (value == "false") return 0
    return roundHalfToEven(toNumber(value, fallback.toFloat()).toDouble())
}

private fun toStringParam(value: Any?, fallback: String = ""): String {
    if (value is String) return value
    if (value is Number || value is Boolean) return value.toString()
    return fallback
}

private fun parseNumberSequence(value: Any?): List<Float> {
    if (value is Number) return listOf(value.toFloat())
    if (value !is String) return emptyList()
    return value.split("/")
        .mapNotNull { it.trim().replace(",", "").toFloatOrNull() }
}

private fun parseVector2(value: Any?): Vec2? {
    if (value !is String || value.isBlank()) return null
    val parts = value.split(",")
    if (parts.size < 2) return null
    val x = parts[0].trim().toFloatOrNull() ?: return null
    val y = parts[1].trim().toFloatOrNull() ?: return null
    return Vec2(x, y)
}

private fun parseCgVector2(value: Any?): Vec2? {
    if (value !is String) return null
    val parts = value.split(",")
    if (parts.size != 2) return null
    val x = parts[0].trim().toFloatOrNull() ?: 0f
    val y = parts[1].trim().toFloatOrNull() ?: 0f
    return Vec2(x, y)
}

private fun parseCgColor(value: Any?): ColorRgba? {
    if (value !is String) return null
    val parts = value.split(",")
    if (parts.size != 4) return null
    val r = parts[0].trim().toFloatOrNull() ?: 1f
    val g = parts[1].trim().toFloatOrNull() ?: 1f
    val b = parts[2].trim().toFloatOrNull() ?: 1f
    val a = parts[3].trim().toFloatOrNull() ?: 1f
    return ColorRgba(r, g, b, a)
}

private fun parseInterludeType(value: Any?): Int {
    val raw = toStringParam(value).trim()
    if (raw == "uichar") return 1
    if (raw == "bg") return 2
    if (raw == "char") return 3
    val num = toNumber(value, 0f).toInt()
    return if (num in 1..3) num else 0
}

data class GroupedAssetSelection(
    val assetKind: String,
    val groupValue: String
)

private fun resolveGroupedAssetSelection(
    imageGroup: String,
    cgGroup: String,
    imageGroupKind: String = "background"
): GroupedAssetSelection? {
    if (imageGroup.isNotEmpty()) return GroupedAssetSelection(imageGroupKind, imageGroup)
    if (cgGroup.isNotEmpty()) return GroupedAssetSelection("image", cgGroup)
    return null
}

data class SkipNodeLabel(
    val lineIndex: Int,
    val mode: String
)

private fun preprocessSkipNodes(lines: List<ParsedLine>): List<SkipNodeLabel> {
    val labels = mutableListOf<SkipNodeLabel>()
    for ((index, line) in lines.withIndex()) {
        if (line is ParsedLine.Command && line.command == "skipnode") {
            val modeStr = toStringParam(line.args["mode"], "skip")
            val mode = if (modeStr == "nofirstskip") "nofirstskip" else "skip"
            labels.add(SkipNodeLabel(index, mode))
        }
    }
    return labels
}

private fun preprocessSkipToIndex(lines: List<ParsedLine>): Int {
    return lines.indexOfFirst { it is ParsedLine.Command && it.command == "skiptothis" }
}

private data class StickerSlotInfo(
    var charCount: Int,
    var rawLength: Int,
    val layout: StickerInput
)

private data class TypingSession(
    val id: Int,
    val speaker: String,
    val richChars: List<RichChar>,
    val tagStyles: RichTagStyles
)

class StoryRuntime(
    private val context: Context,
    private val renderer: StoryRenderer,
    private val audio: StoryAudio,
    options: RuntimeOptions = RuntimeOptions()
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val commandRegistry = CommandRegistry<CommandResult>()
    private val defaultSpeed = AutoSpeed(
        animateRatio = max(0f, options.animateRatio),
        autoWaitBaseTime = 1.5f,
        autoWaitTimePerText = 0.03f,
        typeWriterDelayMs = max(0L, options.typingIntervalMs)
    )
    private val firstRead = options.firstRead
    val onWarning = options.onWarning

    private var autoPlayMode = AutoPlayMode.DEFAULT
    private var buttonSpeedLevel = 0
    private var quickSpeedLevel = 0
    private var autoClickGeneration = 0

    private val lines: List<ParsedLine> = parseScript(context.scriptText ?: context.script.joinToString("\n"))
    private val skipNodeLabels = preprocessSkipNodes(lines)
    private val skipToIndex = preprocessSkipToIndex(lines)

    private var cursor = 0
    private var state = PlayerState.IDLE
    private var destroyed = false

    private val _displayedLineEvents = MutableSharedFlow<Int?>(replay = 1)
    val displayedLineEvents: SharedFlow<Int?> = _displayedLineEvents.asSharedFlow()

    private var displayedLineIndexValue: Int? = null
    var displayedLineIndex: Int?
        get() = displayedLineIndexValue
        private set(value) {
            if (displayedLineIndexValue == value) return
            displayedLineIndexValue = value
            _displayedLineEvents.tryEmit(value)

            val seek = lineSeek
            if (seek != null && value == seek.target) {
                val onUpdate = seek.onUpdate
                val target = seek.target
                closeLineSeek()
                applyAutoPlayMode(AutoPlayMode.DEFAULT)
                onUpdate(LineSeekUpdate(LineSeekPhase.REACHED, null, target))
            }
        }

    private var decisionSelectValue = 0
    private var decisionReferences: List<Int>? = null
    private var decisionPolicy: DecisionPolicy? = null

    private data class ActiveLineSeek(
        val choices: Map<Int, Int>,
        val onUpdate: (LineSeekUpdate) -> Unit,
        var previousPolicy: DecisionPolicy?,
        val target: Int
    )

    private var lineSeek: ActiveLineSeek? = null
    private val choiceHistory = mutableListOf<RuntimeChoiceSelection>()

    private var processing = false
    private var processingCompletion: CompletableDeferred<Unit>? = null

    private var currentSkipMode = "skip"
    private var skipNodeQueueIndex = 0
    private var itemSlotInUse = false
    private var theaterMode = false
    private var theaterAutoPlayCache: AutoPlayState? = null

    private var currentMessageLength = 0
    private var currentRawMessageLength = 0
    private var currentTypingComplete = false

    private var multilineText = ""
    private var multilineEnd = false
    private var multilineShownChars = 0

    private val stickerSlots = mutableMapOf<String, StickerSlotInfo>()
    private val stickerPoolFadeMs = mutableListOf<Long>()
    private var pendingInputEffect: (suspend () -> Unit)? = null

    private var typingSession: TypingSession? = null
    private var typingSessionId = 0
    private var typingJob: Job? = null
    private var autoClickJob: Job? = null

    init {
        for (command in BUILTIN_COMMAND_NAMES) {
            commandRegistry.register(command) { line -> executeBuiltinCommand(line) }
        }
    }

    fun getState(): PlayerState = state
    fun getDisplayedLineIndex(): Int? = displayedLineIndex
    fun getDecisionSelectValue(): Int = decisionSelectValue
    fun getLogPosition(): RuntimeLogPosition = RuntimeLogPosition(
        lineIndex = displayedLineIndex,
        selections = choiceHistory.toList()
    )
    fun getAutoPlayState(): AutoPlayState = AutoPlayState(
        buttonSpeedLevel = buttonSpeedLevel,
        mode = autoPlayMode,
        quickSpeedLevel = quickSpeedLevel
    )

    fun onDisplayedLineChange(listener: (Int?) -> Unit): () -> Unit {
        val job = coroutineScope.launch(Dispatchers.Main.immediate) {
            displayedLineEvents.collect {
                try {
                    listener(it)
                } catch (e: Throwable) {
                    warn("error", e.message ?: "story listener error")
                }
            }
        }
        listener(displayedLineIndex)
        return { job.cancel() }
    }

    fun setDecisionPolicy(policy: DecisionPolicy?) {
        val seek = lineSeek
        if (seek != null) {
            seek.previousPolicy = policy
        } else {
            decisionPolicy = policy
        }
    }

    fun seekToLine(
        target: Int,
        choices: Map<Int, Int>,
        onUpdate: (LineSeekUpdate) -> Unit
    ): () -> Unit {
        if (destroyed || state == PlayerState.FINISHED || state == PlayerState.ERROR) {
            onUpdate(LineSeekUpdate(LineSeekPhase.MISSED, if (state == PlayerState.ERROR) "error" else "finished", target))
            return {}
        }

        closeLineSeek()
        lineSeek = ActiveLineSeek(
            choices = choices,
            onUpdate = { update ->
                try {
                    onUpdate(update)
                } catch (e: Throwable) {
                    warn("error", e.message ?: "line seek listener error")
                }
            },
            previousPolicy = decisionPolicy,
            target = target
        )
        decisionPolicy = { decisionId, _, _ -> choices[decisionId] ?: 0 }
        enterQuickSeekMode()
        lineSeek?.onUpdate?.invoke(LineSeekUpdate(LineSeekPhase.SEEKING, null, target))
        return { closeLineSeek() }
    }

    private fun closeLineSeek(update: LineSeekUpdate? = null) {
        val seek = lineSeek ?: return
        lineSeek = null
        decisionPolicy = seek.previousPolicy
        if (update != null) seek.onUpdate(update)
    }

    private fun enterQuickSeekMode() {
        applyAutoPlayMode(AutoPlayMode.QUICK_PLAY)
        setAutoPlaySpeedLevel(QUICK_AUTO_SPEEDS.size - 1)
    }

    private fun finishLineSeekMissed(reason: String) {
        val seek = lineSeek ?: return
        closeLineSeek(LineSeekUpdate(LineSeekPhase.MISSED, reason, seek.target))
    }

    fun setAutoPlayMode(mode: AutoPlayMode) {
        if (destroyed || state == PlayerState.FINISHED || state == PlayerState.ERROR) return
        if (autoPlayMode == mode) return

        if (mode != AutoPlayMode.QUICK_PLAY && lineSeek != null) {
            closeLineSeek(LineSeekUpdate(LineSeekPhase.ABORTED, null, lineSeek!!.target))
        }

        applyAutoPlayMode(mode)
    }

    private fun applyAutoPlayMode(mode: AutoPlayMode) {
        if (destroyed || state == PlayerState.FINISHED || state == PlayerState.ERROR) return
        if (autoPlayMode == mode) return

        autoPlayMode = mode
        cancelAutoClick()

        if (mode == AutoPlayMode.QUICK_PLAY && state == PlayerState.WAITING_INPUT) {
            coroutineScope.launch { advanceFromClick() }
            return
        }
        if (mode != AutoPlayMode.DEFAULT && currentTypingComplete) {
            scheduleAutoClick(currentRawMessageLength)
        }
    }

    private fun applyScriptAutoPlayMode(mode: AutoPlayMode) {
        applyAutoPlayMode(mode)
        if (lineSeek != null && autoPlayMode != AutoPlayMode.QUICK_PLAY) {
            enterQuickSeekMode()
        }
    }

    fun setAutoPlaySpeedLevel(level: Int) {
        val normalized = max(0, level)
        if (autoPlayMode == AutoPlayMode.QUICK_PLAY) {
            quickSpeedLevel = min(normalized, QUICK_AUTO_SPEEDS.size - 1)
        } else {
            buttonSpeedLevel = min(normalized, BUTTON_AUTO_SPEEDS.size - 1)
        }

        cancelAutoClick()
        if (autoPlayMode != AutoPlayMode.DEFAULT && currentTypingComplete) {
            scheduleAutoClick(currentRawMessageLength)
        }
    }

    fun canSkipNode(): Boolean {
        val running = state != PlayerState.IDLE && state != PlayerState.ERROR && state != PlayerState.FINISHED
        val hasSkipAnchor = skipToIndex >= 0 || skipNodeLabels.isNotEmpty()
        val skipToPending = skipToIndex < 0 || cursor <= skipToIndex
        return running && hasSkipAnchor && skipToPending
    }

    suspend fun start() {
        if (destroyed || state != PlayerState.IDLE) return
        if (context.storyMetadata?.isVideoOnly == true) {
            state = PlayerState.FINISHED
            finishLineSeekMissed("finished")
            return
        }
        state = PlayerState.RUNNING
        processLoop()
    }

    suspend fun advance() {
        if (destroyed) return
        if (state == PlayerState.IDLE) {
            start()
            return
        }
        if (theaterMode) return

        setAutoPlayMode(AutoPlayMode.DEFAULT)
        if (state != PlayerState.WAITING_INPUT) return
        advanceFromClick()
    }

    private suspend fun advanceFromClick() {
        if (state != PlayerState.WAITING_INPUT) return

        if (finishTypingNow()) {
            onTypingComplete()
            if (multilineEnd) resetMultiline()
            return
        }
        if (renderer.finishTextTyping()) {
            onTypingComplete()
            return
        }

        cancelAutoClick()
        if (multilineEnd) resetMultiline()
        renderer.finishDialogueCommand()

        val inputEffect = pendingInputEffect
        pendingInputEffect = null
        inputEffect?.invoke()

        state = PlayerState.RUNNING
        processLoop()
    }

    suspend fun skipNode() {
        if (destroyed || !canSkipNode()) return

        val hadActiveProcessLoop = processing
        val activeProcessCompletion = processingCompletion
        var shouldResume = false

        if (skipToIndex >= 0 && cursor <= skipToIndex) {
            cursor = skipToIndex + 1
            shouldResume = true
        } else if (skipToIndex >= 0) {
            return
        } else if (skipToIndex < 0) {
            val nextLabel = skipNodeLabels.getOrNull(skipNodeQueueIndex)
            if (nextLabel != null) {
                skipNodeQueueIndex += 1
                currentSkipMode = nextLabel.mode
            }

            val tutorial = context.storyMetadata?.isTutorial ?: false
            val isSkippable = !tutorial && (currentSkipMode != "nofirstskip" || !firstRead)
            if (nextLabel != null && !isSkippable) {
                cursor = nextLabel.lineIndex + 1
                shouldResume = true
            } else {
                cursor = lines.size
            }
        }

        pendingInputEffect = null
        if (shouldResume) {
            finishTypingNow()
            renderer.finishDialogueCommand()
            renderer.clearInterludes()
            renderer.clearCharacterCutin()
            renderer.clearSpellStickers()
            renderer.clearSubtitle(0)
            renderer.clearTimerSticker(TimerClearInput(0))

            repeat(stickerSlots.size) { stickerPoolFadeMs.add(150L) }
            stickerSlots.clear()
            renderer.clearStickers(150)
        }

        cancelTyping()
        if (shouldResume) {
            state = PlayerState.RUNNING
        } else if (!hadActiveProcessLoop) {
            state = PlayerState.FINISHED
            finishLineSeekMissed("finished")
        }

        if (hadActiveProcessLoop) {
            activeProcessCompletion?.await()
        } else if (shouldResume) {
            processLoop()
        }
    }

    private fun getCurrentSpeed(): AutoSpeed {
        return when (autoPlayMode) {
            AutoPlayMode.BUTTON_AUTO -> BUTTON_AUTO_SPEEDS.getOrElse(buttonSpeedLevel) { BUTTON_AUTO_SPEEDS[0] }
            AutoPlayMode.QUICK_PLAY -> QUICK_AUTO_SPEEDS.getOrElse(quickSpeedLevel) { defaultSpeed }
            else -> defaultSpeed
        }
    }

    private fun calculateFadeMs(seconds: Any?, fallback: Float = 0f): Long {
        val s = toNumber(seconds, fallback)
        return max(0L, (s * getCurrentSpeed().animateRatio * 1000f).roundToLong())
    }

    private fun stickerTypeDelayMs(value: Any?): Long {
        val originDelaySeconds = TYPE_WRITER_ORIGIN_DELAY_MS / 1000f
        val scale = toNumber(value, 1f) / originDelaySeconds
        return max(0L, (getCurrentSpeed().typeWriterDelayMs * scale).roundToLong())
    }

    private fun multilineTypeDelayScale(value: Any?): Float {
        val originDelaySeconds = TYPE_WRITER_ORIGIN_DELAY_MS / 1000f
        val currentDelaySeconds = getCurrentSpeed().typeWriterDelayMs / 1000f
        val delaySeconds = if (value == null) currentDelaySeconds else toNumber(value, currentDelaySeconds)
        return delaySeconds / originDelaySeconds
    }

    private fun cancelAutoClick() {
        autoClickGeneration += 1
        autoClickJob?.cancel()
        autoClickJob = null
    }

    private fun scheduleAutoClick(messageLength: Int) {
        cancelAutoClick()
        if (autoPlayMode == AutoPlayMode.DEFAULT) return
        val generation = autoClickGeneration
        val speed = getCurrentSpeed()
        val delayMs = ((speed.autoWaitBaseTime + messageLength * speed.autoWaitTimePerText) * 1000f).roundToLong()

        autoClickJob = coroutineScope.launch {
            delay(delayMs)
            if (destroyed || generation != autoClickGeneration || autoPlayMode == AutoPlayMode.DEFAULT || state != PlayerState.WAITING_INPUT) {
                return@launch
            }
            advanceFromClick()
        }
    }

    fun destroy() {
        destroyed = true
        lineSeek = null
        cancelTyping()
        cancelAutoClick()
        pendingInputEffect = null
        coroutineScope.cancel()
    }

    private suspend fun processLoop() {
        if (processing) return
        processing = true
        val completion = CompletableDeferred<Unit>()
        processingCompletion = completion

        try {
            while (!destroyed) {
                if (cursor >= lines.size) {
                    renderer.clearAnimTexts()
                    renderer.clearAvgDisplays()
                    state = PlayerState.FINISHED
                    finishLineSeekMissed("finished")
                    return
                }

                val line = lines[cursor]
                cursor += 1

                if (!passesGate(RuntimeDecisionState(decisionSelectValue, decisionReferences), line)) {
                    continue
                }

                when (line) {
                    is ParsedLine.Dialogue -> {
                        if (line.text.isEmpty()) {
                            cancelTyping()
                            renderer.hideDialogue()
                            continue
                        }
                        resetMultiline()
                        displayedLineIndex = line.lineNumber
                        waitForDialogueInput(line.speaker, line.text)
                        return
                    }

                    is ParsedLine.Narration -> {
                        if (line.text.isEmpty()) continue
                        resetMultiline()
                        displayedLineIndex = line.lineNumber
                        waitForDialogueInput("", line.text)
                        return
                    }

                    is ParsedLine.Command -> {
                        val result = executeCommand(line)
                        if (result == CommandResult.WAIT_INPUT) {
                            state = PlayerState.WAITING_INPUT
                            return
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            state = PlayerState.ERROR
            finishLineSeekMissed("error")
            warn("error", e.message ?: "unknown runtime error")
        } finally {
            processing = false
            completion.complete(Unit)
            processingCompletion = null
        }
    }

    private fun exactArg(args: StoryCommandArgs, key: String): Any? = args[key]

    private fun arg(args: StoryCommandArgs, key: String): Any? {
        if (args.containsKey(key)) return args[key]
        return args.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    private fun warn(type: String, detail: String? = null, lineNumber: Int? = null, command: String? = null) {
        onWarning?.invoke(
            RuntimeWarning(
                command = command,
                cursor = cursor,
                detail = detail,
                lineNumber = lineNumber,
                type = type
            )
        )
    }

    private fun resolveImageKey(key: String): String? {
        val normalized = key.trim().lowercase()
        return if (normalized.isEmpty()) null else normalized
    }

    private fun hasAudioKey(key: String): Boolean {
        return resolveStoryAudioByKey(key, context.audioVariables) != null
    }

    private fun resolveCharacterName(ref: String): CharacterRefSelection? {
        return resolveCharacterSelection(context.linkMap, ref)
    }

    private suspend fun executeCommand(line: ParsedLine.Command): CommandResult {
        if (line.command == "header") return CommandResult.CONTINUE
        val executors = commandRegistry.get(line.command)
        if (executors == null) {
            warn("unknown_command", line.command)
            return CommandResult.CONTINUE
        }
        if (executors.size == 1) {
            return executors[0](line)
        }
        var wait = false
        for (executor in executors) {
            if (executor(line) == CommandResult.WAIT_INPUT) {
                wait = true
            }
        }
        return if (wait) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
    }

    private suspend fun executeBuiltinCommand(line: ParsedLine.Command): CommandResult {
        val args = line.args

        when (line.command) {
            "endtip" -> {
                val shouldProcessEndtip = (autoPlayMode == AutoPlayMode.BUTTON_AUTO || autoPlayMode == AutoPlayMode.QUICK_PLAY) &&
                        !(context.storyMetadata?.isVideoOnly ?: false)
                if (!shouldProcessEndtip) return CommandResult.CONTINUE

                finishLineSeekMissed("finished")
                applyAutoPlayMode(AutoPlayMode.DEFAULT)
                resetMultiline()
                startTypingDialogue("", line.content, executor = "endtip")
                return CommandResult.WAIT_INPUT
            }

            "dialog" -> {
                if (line.content.isNotEmpty()) {
                    resetMultiline()
                    displayedLineIndex = line.lineNumber
                    waitForDialogueInput(toStringParam(exactArg(args, "name")), line.content)
                    return CommandResult.WAIT_INPUT
                }
                cancelTyping()
                renderer.hideDialogue()
                return CommandResult.CONTINUE
            }

            "aside" -> {
                if (line.content.isNotEmpty()) {
                    resetMultiline()
                    displayedLineIndex = line.lineNumber
                    startTypingDialogue("", line.content, executor = "aside")
                    return CommandResult.WAIT_INPUT
                }
                cancelTyping()
                renderer.hideDialogue()
                return CommandResult.CONTINUE
            }

            "delay" -> {
                val durationMs = max(
                    0L,
                    (toNumber(exactArg(args, "time"), 0f) * getCurrentSpeed().animateRatio * 1000f).roundToLong()
                )
                state = PlayerState.WAITING_TIMER
                delay(durationMs)
                state = PlayerState.RUNNING
                return CommandResult.CONTINUE
            }

            "video" -> {
                val url = toStringParam(exactArg(args, "url"))
                val res = toStringParam(exactArg(args, "res"))
                if (url.isEmpty() && res.isEmpty()) {
                    warn("invalid_parameter", "video: no url or res")
                    return CommandResult.CONTINUE
                }

                val resolved = if (url.isNotEmpty()) resolveStoryVideoByKey(url) else resolveStoryVideoByKey(res)
                if (resolved == null) {
                    warn("missing_asset", "video: ${if (url.isNotEmpty()) url else res}")
                    return CommandResult.CONTINUE
                }

                state = PlayerState.WAITING_VIDEO
                renderer.playVideo(resolved)
                state = PlayerState.RUNNING
                return CommandResult.CONTINUE
            }

            "skipnode" -> {
                val label = skipNodeLabels.getOrNull(skipNodeQueueIndex)
                skipNodeQueueIndex += 1
                currentSkipMode = label?.mode ?: "skip"
                return CommandResult.CONTINUE
            }

            "skiptothis" -> return CommandResult.CONTINUE

            "background" -> {
                val image = toStringParam(exactArg(args, "image"))
                val fadeMs = calculateFadeMs(exactArg(args, "fadetime"))
                val block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false)
                if (image.isEmpty()) {
                    renderer.clearBackground(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                val resolved = resolveImageKey(image)
                if (resolved == null) {
                    warn("missing_asset", "background: $image")
                    renderer.clearBackground(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                renderer.setBackground(
                    resolved,
                    BackgroundInput(
                        block = block,
                        fadeMs = fadeMs,
                        height = toNumber(exactArg(args, "height"), 1f),
                        scaleX = toNumber(exactArg(args, "xScale"), 1f),
                        scaleY = toNumber(exactArg(args, "yScale"), 1f),
                        screenAdapt = toStringParam(exactArg(args, "screenadapt")).takeIf { it.isNotEmpty() },
                        tiled = toBoolean(exactArg(args, "tiled"), false),
                        width = toNumber(exactArg(args, "width"), 1f),
                        x = toNumber(exactArg(args, "x"), 0f),
                        y = toNumber(exactArg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "backgroundtween" -> {
                val duration = exactArg(args, "duration")
                val block = toNumber(duration, 0f) > 0 && toBoolean(exactArg(args, "block"), false)
                val loop = toBoolean(exactArg(args, "loop"), false)
                if (block && loop) {
                    warn("invalid_parameter", "Loop and block both true when tween background! Will cause intinity lop!", line.lineNumber, line.command)
                }

                renderer.setBackgroundTween(
                    BackgroundTweenInput(
                        block = block && !loop,
                        durationMs = max(0L, (toNumber(duration, 0f) * 1000f).roundToLong()),
                        ease = toStringParam(exactArg(args, "ease"), "Linear"),
                        loop = loop,
                        xFrom = toOptionalNumber(exactArg(args, "xFrom")),
                        xScaleFrom = toOptionalNumber(exactArg(args, "xScaleFrom")),
                        xScaleTo = toOptionalNumber(exactArg(args, "xScaleTo")),
                        xTo = toOptionalNumber(exactArg(args, "xTo")),
                        yFrom = toOptionalNumber(exactArg(args, "yFrom")),
                        yScaleFrom = toOptionalNumber(exactArg(args, "yScaleFrom")),
                        yScaleTo = toOptionalNumber(exactArg(args, "yScaleTo")),
                        yTo = toOptionalNumber(exactArg(args, "yTo"))
                    )
                )
                return CommandResult.CONTINUE
            }

            "gridbg" -> {
                val imageGroup = toStringParam(exactArg(args, "imagegroup"))
                val cgGroup = toStringParam(exactArg(args, "cggroup"))
                val groupSelection = resolveGroupedAssetSelection(imageGroup, cgGroup)
                val initPositionModeArg = toStringParam(exactArg(args, "initposmode"), "default")
                val initPositionMode = if (initPositionModeArg in setOf("default", "upperleft", "lowercenter", "center")) initPositionModeArg else "center"
                val fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong())
                val block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false)

                if (groupSelection == null) {
                    renderer.clearGridBackground(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                val imageKeys = groupSelection.groupValue.split("/").mapNotNull { resolveImageKey(it) }
                val solidWidths = parseNumberSequence(exactArg(args, "solidwidth")).take(2)
                val solidHeights = parseNumberSequence(exactArg(args, "solidheight")).take(2)

                if (imageKeys.isEmpty() || imageKeys.size != 4 || solidWidths.size != 2 || solidHeights.size != 2) {
                    warn("parse", "gridbg parameter validation failed")
                    renderer.clearGridBackground(0)
                    return CommandResult.CONTINUE
                }

                renderer.setGridBackground(
                    GridBackgroundInput(
                        assetKind = groupSelection.assetKind,
                        block = block,
                        fadeMs = fadeMs,
                        imageKeys = imageKeys,
                        initPositionMode = initPositionMode,
                        layout = "grid",
                        scaleX = toNumber(exactArg(args, "xScale"), 1f),
                        scaleY = toNumber(exactArg(args, "yScale"), 1f),
                        solidHeights = solidHeights,
                        solidWidths = solidWidths,
                        x = toNumber(exactArg(args, "x"), 0f),
                        y = toNumber(exactArg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "verticalbg" -> {
                val imageGroup = toStringParam(exactArg(args, "imagegroup"))
                val cgGroup = toStringParam(exactArg(args, "cggroup"))
                val groupSelection = resolveGroupedAssetSelection(imageGroup, cgGroup)
                val initPositionModeArg = toStringParam(exactArg(args, "initposmode"), "default")
                val initPositionMode = if (initPositionModeArg in setOf("default", "upperleft", "lowercenter", "center")) initPositionModeArg else "center"
                val fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong())
                val block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false)

                if (groupSelection == null) {
                    renderer.clearGridBackground(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                val imageKeys = groupSelection.groupValue.split("/").mapNotNull { resolveImageKey(it) }
                val solidWidth = toNumber(exactArg(args, "solidwidth"), 0f)
                val solidWidths = if (solidWidth > 0) listOf(solidWidth) else emptyList()
                val solidHeights = parseNumberSequence(exactArg(args, "solidheight")).take(imageKeys.size)

                if (imageKeys.isEmpty() || imageKeys.size > 4 || solidWidths.size != 1 || solidHeights.size != imageKeys.size) {
                    warn("parse", "verticalbg validation failed", line.lineNumber, line.command)
                    renderer.clearGridBackground(0, false)
                    return CommandResult.CONTINUE
                }

                renderer.setGridBackground(
                    GridBackgroundInput(
                        assetKind = groupSelection.assetKind,
                        block = block,
                        fadeMs = fadeMs,
                        imageKeys = imageKeys,
                        initPositionMode = initPositionMode,
                        layout = "vertical",
                        scaleX = toNumber(exactArg(args, "xScale"), 1f),
                        scaleY = toNumber(exactArg(args, "yScale"), 1f),
                        solidHeights = solidHeights,
                        solidWidths = solidWidths,
                        x = toNumber(exactArg(args, "x"), 0f),
                        y = toNumber(exactArg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "largebg" -> {
                val imageGroup = toStringParam(exactArg(args, "imagegroup"))
                val cgGroup = toStringParam(exactArg(args, "cggroup"))
                val groupSelection = resolveGroupedAssetSelection(imageGroup, cgGroup)
                val initPositionModeArg = toStringParam(exactArg(args, "initposmode"), "default")
                val initPositionMode = if (initPositionModeArg in setOf("default", "lowercenter", "upperleft")) initPositionModeArg else "center"
                val fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong())
                val block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false)

                if (groupSelection == null) {
                    renderer.clearGridBackground(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                val imageKeys = groupSelection.groupValue.split("/").mapNotNull { resolveImageKey(it) }
                val solidWidths = parseNumberSequence(exactArg(args, "solidwidth")).take(2)
                val solidHeight = toNumber(exactArg(args, "solidheight"), 0f)

                if (imageKeys.size != 2 || solidWidths.size != 2) {
                    warn("parse", "largebg expects width_count * height_count == image_count")
                    renderer.clearGridBackground(0)
                    return CommandResult.CONTINUE
                }

                renderer.setGridBackground(
                    GridBackgroundInput(
                        assetKind = groupSelection.assetKind,
                        block = block,
                        fadeMs = fadeMs,
                        imageKeys = imageKeys,
                        initPositionMode = initPositionMode,
                        layout = "large",
                        scaleX = toNumber(exactArg(args, "xScale"), 1f),
                        scaleY = toNumber(exactArg(args, "yScale"), 1f),
                        solidHeights = listOf(solidHeight),
                        solidWidths = solidWidths,
                        x = toNumber(exactArg(args, "x"), 0f),
                        y = toNumber(exactArg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "largeimg" -> {
                val imageGroup = toStringParam(arg(args, "imagegroup"))
                val cgGroup = toStringParam(arg(args, "cggroup"))
                val groupSelection = resolveGroupedAssetSelection(imageGroup, cgGroup, "image")
                val groupValue = groupSelection?.groupValue ?: ""
                val fadeMs = max(0L, (toNumber(arg(args, "fadetime"), if (groupValue.isNotEmpty()) 0.15f else 0f) * 1000f).roundToLong())
                val block = toBoolean(arg(args, "blok"), toBoolean(arg(args, "isblock"), toBoolean(arg(args, "block"), false)))

                if (groupSelection == null) {
                    renderer.clearLargeImage(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                val imageKeys = groupSelection.groupValue.split("/").mapNotNull { resolveImageKey(it) }
                val solidWidths = parseNumberSequence(arg(args, "solidwidth"))
                val solidHeights = parseNumberSequence(arg(args, "solidheight"))

                if (imageKeys.isEmpty() || solidWidths.isEmpty() || solidHeights.isEmpty() || solidWidths.size * solidHeights.size != imageKeys.size) {
                    warn("parse", "largeimg tile validation failed")
                    return CommandResult.CONTINUE
                }

                renderer.setLargeImage(
                    GridBackgroundInput(
                        assetKind = groupSelection.assetKind,
                        block = block,
                        fadeMs = fadeMs,
                        imageKeys = imageKeys,
                        layout = "large",
                        scaleX = max(0f, toNumber(arg(args, "xscale"), 1f)),
                        scaleY = max(0f, toNumber(arg(args, "yscale"), 1f)),
                        solidHeights = solidHeights,
                        solidWidths = solidWidths,
                        x = toNumber(arg(args, "x"), 0f),
                        y = toNumber(arg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "image" -> {
                val image = toStringParam(exactArg(args, "image"))
                val fadeMs = calculateFadeMs(exactArg(args, "fadetime"))
                val block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false)
                if (image.isEmpty()) {
                    renderer.clearImage(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                val resolved = resolveImageKey(image)
                if (resolved == null) {
                    warn("missing_asset", "image: $image")
                    renderer.clearImage(fadeMs, block)
                    return CommandResult.CONTINUE
                }

                renderer.setImage(
                    resolved,
                    BackgroundInput(
                        block = block,
                        fadeMs = fadeMs,
                        height = toNumber(exactArg(args, "height"), 1f),
                        scaleX = toNumber(exactArg(args, "xScale"), 1f),
                        scaleY = toNumber(exactArg(args, "yScale"), 1f),
                        screenAdapt = toStringParam(exactArg(args, "screenadapt")).takeIf { it.isNotEmpty() },
                        tiled = toBoolean(exactArg(args, "tiled"), false),
                        width = toNumber(exactArg(args, "width"), 1f),
                        x = toNumber(exactArg(args, "x"), 0f),
                        y = toNumber(exactArg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "showitem" -> {
                val style = toStringParam(exactArg(args, "style"), "photo")
                if (style != "photo" && style != "cutin") {
                    warn("invalid_parameter", "showitem style is not registered: $style", line.lineNumber, line.command)
                    return CommandResult.CONTINUE
                }
                if (style == "cutin") {
                    warn("unsupported_visual", "showitem style=cutin uses cutin expand", line.lineNumber, line.command)
                }

                val image = toStringParam(exactArg(args, "image"))
                if (image.isEmpty()) return CommandResult.CONTINUE
                val resolved = resolveImageKey(image)
                if (resolved == null) {
                    warn("missing_asset", "showitem: $image")
                    return CommandResult.CONTINUE
                }

                renderer.showItem(
                    ShowItemInput(
                        blackAlpha = clamp(toNumber(exactArg(args, "black"), 0.4f), 0f, 1f),
                        block = true,
                        fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0.5f) * 1000f).roundToLong()),
                        key = resolved,
                        offsetX = toNumber(exactArg(args, "offsetx"), 0f),
                        offsetY = toNumber(exactArg(args, "offsety"), 0f)
                    )
                )
                itemSlotInUse = true
                return CommandResult.CONTINUE
            }

            "hideitem" -> {
                val hadItem = itemSlotInUse
                itemSlotInUse = false
                renderer.clearItems(
                    max(0L, (toNumber(exactArg(args, "fadetime"), 0.5f) * 1000f).roundToLong()),
                    hadItem
                )
                return CommandResult.CONTINUE
            }

            "cgitem" -> {
                val image = toStringParam(exactArg(args, "image"))
                val id = toStringParam(exactArg(args, "id"))
                val key = if (id.isNotEmpty()) "${image}_$id" else image
                val style = toStringParam(exactArg(args, "style"), "cg")
                if (style == "blocker" || style != "cg" || image.isEmpty()) {
                    warn("invalid_parameter", "cgitem style unsupported or empty image")
                    return CommandResult.CONTINUE
                }
                val resolved = resolveImageKey(image)
                if (resolved == null) {
                    warn("missing_asset", "cgitem: $image")
                    return CommandResult.CONTINUE
                }

                fun s2ms(name: String): Long = max(0L, (toNumber(exactArg(args, name), 0f) * 1000f).roundToLong())
                val cFrom = parseCgColor(exactArg(args, "cfrom"))
                val cTo = parseCgColor(exactArg(args, "cto"))

                renderer.showCgItem(
                    CgItemInput(
                        assetKey = resolved,
                        alphaDelayMs = s2ms("adelay"),
                        alphaDurationMs = s2ms("aduration"),
                        alphaFrom = toNumber(exactArg(args, "afrom"), 1f),
                        alphaTo = toNumber(exactArg(args, "ato"), 1f),
                        block = toBoolean(exactArg(args, "block"), false),
                        colorFrom = if (cFrom != null && cTo != null) cFrom else null,
                        colorTo = if (cFrom != null && cTo != null) cTo else null,
                        ease = toStringParam(exactArg(args, "ease"), "Linear"),
                        height = toNumber(exactArg(args, "height"), 0f).toInt(),
                        key = key,
                        positionDelayMs = s2ms("pdelay"),
                        positionDurationMs = s2ms("pduration"),
                        positionFrom = parseCgVector2(exactArg(args, "pfrom")),
                        positionTo = parseCgVector2(exactArg(args, "pto")),
                        rotationDurationMs = s2ms("rduration"),
                        rotationFrom = toNumber(exactArg(args, "rfrom"), -1f),
                        rotationTo = toNumber(exactArg(args, "rto"), 0f),
                        scaleDelayMs = s2ms("sdelay"),
                        scaleDurationMs = s2ms("sduration"),
                        scaleFrom = toNumber(exactArg(args, "sfrom"), 1f),
                        scaleTo = toNumber(exactArg(args, "sto"), 1f),
                        width = toNumber(exactArg(args, "width"), 0f).toInt()
                    )
                )
                return CommandResult.CONTINUE
            }

            "hidecgitem" -> {
                val image = toStringParam(exactArg(args, "image"))
                val id = toStringParam(exactArg(args, "id"))
                val key = if (image.isNotEmpty() || id.isNotEmpty()) (if (id.isNotEmpty()) "${image}_$id" else image) else null
                renderer.clearCgItems(
                    key = key,
                    fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0.13f) * 1000f).roundToLong()),
                    ease = toStringParam(exactArg(args, "ease"), "Linear"),
                    block = toBoolean(exactArg(args, "block"), false)
                )
                return CommandResult.CONTINUE
            }

            "imagetween" -> {
                val durationMs = max(0L, (toNumber(exactArg(args, "duration"), 0f) * 1000f).roundToLong())
                val block = durationMs > 0 && toBoolean(exactArg(args, "block"), false)
                val loop = toBoolean(exactArg(args, "loop"), false)
                if (block && loop) {
                    warn("invalid_parameter", "Loop and block both true when tween background! Will cause intinity lop!", line.lineNumber, line.command)
                }
                renderer.setImageTween(
                    ImageTweenInput(
                        block = block && !loop,
                        durationMs = durationMs,
                        ease = toStringParam(exactArg(args, "ease"), "Linear"),
                        loop = loop,
                        xFrom = toOptionalNumber(exactArg(args, "xFrom")),
                        xScaleFrom = toOptionalNumber(exactArg(args, "xScaleFrom")),
                        xScaleTo = toOptionalNumber(exactArg(args, "xScaleTo")),
                        xTo = toOptionalNumber(exactArg(args, "xTo")),
                        yFrom = toOptionalNumber(exactArg(args, "yFrom")),
                        yScaleFrom = toOptionalNumber(exactArg(args, "yScaleFrom")),
                        yScaleTo = toOptionalNumber(exactArg(args, "yScaleTo")),
                        yTo = toOptionalNumber(exactArg(args, "yTo"))
                    )
                )
                return CommandResult.CONTINUE
            }

            "imagerotate" -> {
                renderer.setImageRotate(
                    ImageRotateInput(
                        angleDeg = toNumber(exactArg(args, "angle"), 0f),
                        block = toBoolean(exactArg(args, "block"), false),
                        circles = toNumber(exactArg(args, "circles"), 0f).toInt(),
                        durationMs = calculateFadeMs(exactArg(args, "fadetime")),
                        inverse = toBoolean(exactArg(args, "inverse"), false)
                    )
                )
                return CommandResult.CONTINUE
            }

            "largebgtween" -> {
                val durationMs = max(0L, (toNumber(exactArg(args, "duration"), 0f) * 1000f).roundToLong())
                val block = durationMs > 0 && toBoolean(exactArg(args, "block"), false)
                val loop = toBoolean(exactArg(args, "loop"), false)
                if (block && loop) {
                    warn("invalid_parameter", "Loop and block both true when tween background! Will cause intinity lop!")
                }
                renderer.setLargeBackgroundTween(
                    LargeBackgroundTweenInput(
                        block = block && !loop,
                        durationMs = durationMs,
                        ease = toStringParam(exactArg(args, "ease"), "Linear"),
                        loop = loop,
                        xFrom = toOptionalNumber(exactArg(args, "xFrom")),
                        xScaleFrom = toOptionalNumber(exactArg(args, "xScaleFrom")),
                        xScaleTo = toOptionalNumber(exactArg(args, "xScaleTo")),
                        xTo = toOptionalNumber(exactArg(args, "xTo")),
                        yFrom = toOptionalNumber(exactArg(args, "yFrom")),
                        yScaleFrom = toOptionalNumber(exactArg(args, "yScaleFrom")),
                        yScaleTo = toOptionalNumber(exactArg(args, "yScaleTo")),
                        yTo = toOptionalNumber(exactArg(args, "yTo"))
                    )
                )
                return CommandResult.CONTINUE
            }

            "largeimgtween" -> {
                val durationMs = max(0L, (toNumber(arg(args, "duration"), 0f) * 1000f).roundToLong())
                val block = durationMs > 0 && toBoolean(arg(args, "block"), false)
                val loop = toBoolean(arg(args, "loop"), false)
                val xScale = toOptionalNumber(arg(args, "xscale"))
                val yScale = toOptionalNumber(arg(args, "yscale"))
                val x = toOptionalNumber(arg(args, "x"))
                val y = toOptionalNumber(arg(args, "y"))

                renderer.setLargeImageTween(
                    LargeBackgroundTweenInput(
                        block = block && !loop,
                        durationMs = durationMs,
                        ease = toStringParam(arg(args, "ease"), "Linear"),
                        loop = loop,
                        xFrom = toOptionalNumber(arg(args, "xfrom")) ?: x,
                        xScaleFrom = toOptionalNumber(arg(args, "xscalefrom")) ?: xScale,
                        xScaleTo = toOptionalNumber(arg(args, "xscaleto")) ?: xScale,
                        xTo = toOptionalNumber(arg(args, "xto")) ?: x,
                        yFrom = toOptionalNumber(arg(args, "yfrom")) ?: y,
                        yScaleFrom = toOptionalNumber(arg(args, "yscalefrom")) ?: yScale,
                        yScaleTo = toOptionalNumber(arg(args, "yscaleto")) ?: yScale,
                        yTo = toOptionalNumber(arg(args, "yto")) ?: y
                    )
                )
                return CommandResult.CONTINUE
            }

            "blocker" -> {
                val styleArg = toStringParam(exactArg(args, "style"), "defualt")
                val style = if (styleArg == "slider" || styleArg == "verticalslider") styleArg else "default"
                val fadeMs = calculateFadeMs(exactArg(args, "fadetime"), 0.4f)

                renderer.setBlocker(
                    BlockerInput(
                        block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false),
                        fadeMs = fadeMs,
                        from = ColorRgba(
                            r = toNumber(exactArg(args, "rfrom"), Float.NaN),
                            g = toNumber(exactArg(args, "gfrom"), Float.NaN),
                            b = toNumber(exactArg(args, "bfrom"), Float.NaN),
                            a = toNumber(exactArg(args, "afrom"), Float.NaN)
                        ),
                        image = toStringParam(exactArg(args, "image")).takeIf { it.isNotEmpty() },
                        inverse = toBoolean(exactArg(args, "inverse"), false),
                        style = style,
                        to = ColorRgba(
                            r = toNumber(exactArg(args, "r"), 0f),
                            g = toNumber(exactArg(args, "g"), 0f),
                            b = toNumber(exactArg(args, "b"), 0f),
                            a = toNumber(exactArg(args, "a"), 1f)
                        )
                    )
                )
                return CommandResult.CONTINUE
            }

            "curtain" -> {
                val direction = toNumber(exactArg(args, "direction"), -1f).toInt()
                val fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0.4f) * 1000f).roundToLong())
                val block = fadeMs > 0 && toBoolean(exactArg(args, "block"), false)

                if (direction < 0) {
                    renderer.clearCurtains(fadeMs, false)
                    return CommandResult.CONTINUE
                }
                if (direction > 7) {
                    warn("invalid_parameter", "curtain direction: $direction")
                    return CommandResult.CONTINUE
                }
                val alpha = toNumber(exactArg(args, "a"), -1f)

                renderer.setCurtain(
                    CurtainInput(
                        alphaFrom = toOptionalNumber(exactArg(args, "afrom")),
                        alphaTo = if (alpha >= 0f) alpha else null,
                        block = block,
                        delayMs = max(0L, (toNumber(exactArg(args, "delay"), 0f) * 1000f).roundToLong()),
                        direction = direction,
                        fadeMs = fadeMs,
                        fillFrom = toNumber(exactArg(args, "fillfrom"), 1f),
                        fillTo = toNumber(exactArg(args, "fillto"), 0f),
                        grad = toBoolean(exactArg(args, "grad"), false)
                    )
                )
                return CommandResult.CONTINUE
            }

            "charslot" -> {
                val rawSlot = toStringParam(args["slot"]).trim()
                val slot = parseCharacterSlot(rawSlot)
                val nameRef = toStringParam(args["name"])
                val durationMs = calculateFadeMs(args["duration"], 0f)
                val isEnd = !toStringParam(args["end"]).trim().equals("false", ignoreCase = true)
                val rawBlock = toBoolean(args["isblock"], false)
                val block = rawBlock && isEnd

                if (rawSlot.isEmpty()) {
                    renderer.clearCharacters(null, durationMs)
                    return CommandResult.CONTINUE
                }
                if (slot == null) {
                    warn("unsupported_visual", "charslot slot is not a built-in slot: $rawSlot", line.lineNumber, line.command)
                    return CommandResult.CONTINUE
                }

                val isEmptyChar = nameRef == "char_empty"
                var resolved: CharacterRefSelection? = null
                if (isEmptyChar) {
                    renderer.clearCharacters(slot, 0)
                } else if (nameRef.isNotEmpty()) {
                    resolved = resolveCharacterName(nameRef)
                    if (resolved == null) {
                        warn("missing_asset", "character: $nameRef")
                        renderer.clearCharacters(slot, durationMs)
                    }
                }

                val rawAlphaFrom = if (args["afrom"] == null) -1f else toNumber(args["afrom"], -1f)
                val rawAlphaTo = if (args["ato"] == null) -1f else toNumber(args["ato"], -1f)
                var alphaFrom: Float? = null
                var alphaTo: Float? = null
                if (resolved != null) {
                    if (rawAlphaFrom < 0f || rawAlphaTo < 0f) {
                        alphaFrom = 0f
                        alphaTo = 1f
                    } else {
                        alphaFrom = clamp(rawAlphaFrom, 0f, 1f)
                        alphaTo = clamp(rawAlphaTo, 0f, 1f)
                    }
                } else if (nameRef.isEmpty() && rawAlphaFrom >= 0f) {
                    alphaFrom = clamp(rawAlphaFrom, 0f, 1f)
                    alphaTo = min(1f, rawAlphaTo)
                }

                renderer.setCharacter(
                    CharacterSlotInput(
                        action = toStringParam(args["action"]).trim().lowercase().takeIf {
                            it in setOf("jump", "move", "rotate", "setpos", "shake", "shakemove", "zoom")
                        },
                        angle = toOptionalNumber(args["angle"]),
                        alphaFrom = alphaFrom,
                        alphaTo = alphaTo,
                        blackEnd = toOptionalNumber(args["bend"]),
                        blackStart = toOptionalNumber(args["bstart"]),
                        block = block,
                        characterKey = resolved?.base,
                        circles = toOptionalNumber(args["circles"])?.toInt(),
                        durationMs = durationMs,
                        expression = resolved?.expression,
                        fadeIdentity = if (nameRef.isNotEmpty()) nativeCharacterFadeIdentity(nameRef) else null,
                        focusMode = "subset",
                        focusSlots = resolveCharacterSlotFocusSlots(args),
                        inverse = if (args["inverse"] != null) toBoolean(args["inverse"]) else null,
                        nativeKey = nameRef.takeIf { it.isNotEmpty() },
                        positionFrom = parseVector2(args["posfrom"]),
                        positionTo = parseVector2(args["posto"]) ?: if (toStringParam(args["posto"]).trim().isNotEmpty()) Vec2(0f, 0f) else null,
                        posZoom = parseVector2(args["poszoom"]),
                        power = max(0f, toNumber(args["power"], 0f)),
                        randomness = clamp(toNativeIntParam(args["random"], 10).toFloat(), 0f, 100f).toInt(),
                        replaceFadeMs = durationMs,
                        scaleX = if (args["xscale"] == null && args["scale"] == null) null else max(0f, toNumber(args["xscale"], toNumber(args["scale"], 1f))),
                        scaleY = if (args["yscale"] == null && args["scale"] == null) null else max(0f, toNumber(args["yscale"], toNumber(args["scale"], 1f))),
                        slot = slot,
                        slotSequence = true,
                        stop = toBoolean(args["stop"], false),
                        times = toNumber(args["times"], 1f).toInt()
                    )
                )
                return CommandResult.CONTINUE
            }

            "character" -> {
                val nameRef = toStringParam(args["name"])
                val name2Ref = toStringParam(args["name2"])
                val focus = toNumber(args["focus"], 0f).toInt()
                val durationMs = calculateFadeMs(args["fadetime"], 0.15f)
                val transType = toNumber(args["transtype"], 0f).toInt()
                val hasRight = name2Ref.isNotEmpty()

                data class SlotUpdateTask(val slot: String, val name: String, val bStart: Any?, val bEnd: Any?, val enter: Any?, val x: Any?, val y: Any?)
                val updates = listOf(
                    SlotUpdateTask("m", if (nameRef.isNotEmpty() && !hasRight) nameRef else "", args["blackstart"], args["blackend"], args["enter"], args["xpos1"], args["ypos1"]),
                    SlotUpdateTask("l", if (hasRight) nameRef else "", args["blackstart"], args["blackend"], args["enter"], args["xpos1"], args["ypos1"]),
                    SlotUpdateTask("r", name2Ref, args["blackstart2"], args["blackend2"], args["enter2"], args["xpos2"], args["ypos2"])
                )

                for (update in updates) {
                    if (update.name.isEmpty()) {
                        renderer.clearCharacters(update.slot, durationMs)
                        continue
                    }
                    val resolved = resolveCharacterName(update.name)
                    if (resolved == null) {
                        warn("missing_asset", "character: ${update.name}")
                        renderer.clearCharacters(update.slot, durationMs)
                        continue
                    }
                    val enterFrom = parseEnterDirection(update.enter)
                    val enterPosition = if (enterFrom != null && update.x != null && update.y != null) {
                        Vec2(toNumber(update.x, 0f).toInt().toFloat(), -toNumber(update.y, 0f).toInt().toFloat())
                    } else null

                    renderer.setCharacter(
                        CharacterSlotInput(
                            blackEnd = toOptionalNumber(update.bEnd),
                            blackStart = toOptionalNumber(update.bStart),
                            block = false,
                            characterKey = resolved.base,
                            dimmed = if (update.slot == "r") (focus !in setOf(0, 2)) else (focus.toLong() and 0xFFFFFFFFL) > 1,
                            durationMs = durationMs,
                            enterFrom = enterFrom,
                            enterPosition = enterPosition,
                            expression = resolved.expression,
                            fadeIdentity = nativeCharacterFadeIdentity(update.name),
                            focus = focus,
                            nativeKey = update.name,
                            slot = update.slot,
                            transType = transType
                        )
                    )
                }
                return CommandResult.CONTINUE
            }

            "characteraction" -> {
                val type = toStringParam(args["type"]).trim().lowercase()
                if (type !in setOf("exit", "jump", "move", "shake", "zoom")) {
                    warn("parse", "characteraction type is invalid: ${args["type"]}")
                    return CommandResult.CONTINUE
                }
                val slot = parseCharacterActionSlot(args["name"], type)
                val scaleFallback = toOptionalNumber(args["scale"])?.coerceAtLeast(0f)
                val xScale = toOptionalNumber(args["xscale"])?.coerceAtLeast(0f) ?: scaleFallback
                val yScale = toOptionalNumber(args["yscale"])?.coerceAtLeast(0f) ?: scaleFallback

                renderer.runCharacterAction(
                    CharacterActionInput(
                        block = toBoolean(args["block"], toBoolean(args["isblock"], false)),
                        direction = toStringParam(args["direction"]).trim().lowercase().takeIf { it == "left" || it == "right" },
                        durationMs = max(0L, (toNumber(args["fadetime"], 0.5f) * 1000f).roundToLong()),
                        power = max(0f, toNumber(args["power"], 0f)),
                        randomness = clamp(toNumber(args["randomness"], 90f), 0f, 100f),
                        rotationFromDeg = toNumber(args["start"], 0f),
                        rotationLeftDeg = if (args["leftend"] == null) -15f else -toNumber(args["leftend"], 15f),
                        rotationRightDeg = if (args["rightend"] == null) 15f else toNumber(args["rightend"], 15f),
                        scaleX = xScale,
                        scaleY = yScale,
                        slot = slot,
                        stop = toBoolean(args["stop"], false),
                        times = toNumber(args["times"], 1f).toInt(),
                        type = type,
                        xOffset = toNumber(args["xpos"], 0f),
                        yOffset = toNumber(args["ypos"], 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "charactercutin" -> {
                val widgetId = toStringParam(args["widgetID"])
                if (widgetId.isEmpty()) {
                    warn("parse", "charactercutin widgetID is empty")
                    return CommandResult.CONTINUE
                }

                val nameRef = toStringParam(args["name"]).trim()
                val resolved = if (nameRef.isNotEmpty()) resolveCharacterName(nameRef) else null
                val fadeMs = calculateFadeMs(args["fadetime"], 0.4f)
                val block = toBoolean(args["block"], false)

                if (nameRef.isNotEmpty() && resolved == null) {
                    warn("missing_asset", "charactercutin: $nameRef")
                }

                val fadeStyleRaw = toStringParam(args["fadestyle"]).trim().lowercase()
                val validStyles = setOf(
                    "fade", "horiz_expand_center", "horiz_expand_left2right", "horiz_expand_right2left",
                    "vert_expand_center", "vert_expand_top2bottom", "vert_expand_bottom2top"
                )

                renderer.setCharacterCutin(
                    CharacterCutinInput(
                        block = block,
                        characterKey = resolved?.base,
                        characterMissing = nameRef.isNotEmpty() && resolved == null,
                        charOffsetX = toOptionalNumber(args["charOffsetX"]),
                        charOffsetY = toOptionalNumber(args["charOffsetY"]),
                        expression = resolved?.expression,
                        fadeMs = fadeMs,
                        fadeStyle = fadeStyleRaw.takeIf { it in validStyles },
                        offsetX = toOptionalNumber(args["offsetx"]),
                        offsetY = toOptionalNumber(args["offsety"]),
                        povX = toOptionalNumber(args["povX"]),
                        povY = toOptionalNumber(args["povY"]),
                        widgetId = widgetId,
                        width = toOptionalNumber(args["width"]),
                        zoom = toOptionalNumber(args["zoom"])
                    )
                )
                return CommandResult.CONTINUE
            }

            "interlude" -> {
                val checkedChannel = toNumber(exactArg(args, "channel"), 0f).toInt()
                if (checkedChannel < 0) {
                    warn("invalid_parameter", "interlude channel is invalid: $checkedChannel")
                    return CommandResult.CONTINUE
                }

                val channel = exactArg(args, "channel")?.let { toNumber(it, -1f).toInt() } ?: -1
                val type = parseInterludeType(exactArg(args, "type"))
                val name = toStringParam(exactArg(args, "name"))
                val resolvedAvatar = if (type == 3 && name.isNotEmpty()) resolveCharacterName(name) else null
                if (type == 3 && name.isNotEmpty() && resolvedAvatar == null) {
                    warn("missing_asset", "interlude avatar: $name")
                }

                fun dur(k: String) = max(0L, (toNumber(exactArg(args, k), 0f) * getCurrentSpeed().animateRatio * 1000f).roundToLong())

                renderer.setInterlude(
                    InterludeInput(
                        alphaDurationMs = dur("aduration"),
                        alphaFrom = toNumber(exactArg(args, "afrom"), -1f),
                        alphaTo = toNumber(exactArg(args, "ato"), -1f),
                        avatarCharacterKey = resolvedAvatar?.base,
                        avatarExpression = resolvedAvatar?.expression,
                        block = toBoolean(exactArg(args, "block"), false),
                        channel = channel,
                        charName = toStringParam(exactArg(args, "char")),
                        clear = toBoolean(exactArg(args, "clear"), false),
                        direction = toStringParam(exactArg(args, "direction")),
                        durationMs = dur("duration"),
                        maskId = toStringParam(exactArg(args, "maskid")),
                        name = name,
                        offset = parseVector2(exactArg(args, "offset")) ?: Vec2(0f, 0f),
                        positionFrom = parseVector2(exactArg(args, "pfrom")),
                        positionTo = parseVector2(exactArg(args, "pto")),
                        scaleDurationMs = dur("sduration"),
                        scaleFrom = parseVector2(exactArg(args, "sfrom")),
                        scaleTo = parseVector2(exactArg(args, "sto")),
                        size = parseVector2(exactArg(args, "size")) ?: Vec2(0f, 0f),
                        slot = toStringParam(exactArg(args, "slot")),
                        style = toNumber(exactArg(args, "style"), 0f).toInt(),
                        switchOn = toBoolean(exactArg(args, "switch"), false),
                        templateSizeDurationMs = dur("tsduration"),
                        templateSizeFrom = parseVector2(exactArg(args, "tsfrom")) ?: Vec2(0f, 0f),
                        templateSizeTo = parseVector2(exactArg(args, "tsto")) ?: Vec2(0f, 0f),
                        type = type
                    )
                )
                return CommandResult.CONTINUE
            }

            "animtext" -> {
                val rawPos = exactArg(args, "pos")
                val parsedPos = parseVector2(rawPos)
                renderer.setAnimText(
                    AnimTextInput(
                        block = toBoolean(exactArg(args, "block"), false),
                        content = translateText(line.trailingText),
                        id = toStringParam(exactArg(args, "id")),
                        name = toStringParam(exactArg(args, "name")),
                        position = parsedPos ?: Vec2(0f, 0f),
                        style = toStringParam(exactArg(args, "style"))
                    )
                )
                return CommandResult.CONTINUE
            }

            "avgdisplay" -> {
                val rawStyle = toStringParam(exactArg(args, "style"))
                val rawSlot = toStringParam(exactArg(args, "slot"))
                val styleMap = mapOf("1" to "animetext", "2" to "spine", "3" to "effect", "4" to "bgeffect", "5" to "bg", "6" to "animekv", "7" to "character")
                val slotMap = mapOf("1" to "bgover", "2" to "charover", "3" to "cgover")
                val style = styleMap[rawStyle] ?: rawStyle
                val slot = slotMap[rawSlot] ?: rawSlot
                val durationMs = calculateFadeMs(exactArg(args, "duration"), if (style == "bg") 0.1f else 0f)

                renderer.setAvgDisplay(
                    AvgDisplayInput(
                        alphaFrom = toOptionalNumber(exactArg(args, "afrom")),
                        alphaTo = toOptionalNumber(exactArg(args, "ato")),
                        block = durationMs > 0 && toBoolean(exactArg(args, "block"), false),
                        durationMs = durationMs,
                        entryFrom = toOptionalNumber(exactArg(args, "entryfrom")),
                        entryIndex = toNumber(exactArg(args, "entryindex"), 0f).toInt(),
                        entryTo = toOptionalNumber(exactArg(args, "entryto")),
                        id = toStringParam(exactArg(args, "id")),
                        layer = toNumber(exactArg(args, "layer"), 1f).toInt(),
                        name = toStringParam(exactArg(args, "name")),
                        rotationX = toNumber(exactArg(args, "rox"), 0f),
                        rotationY = toNumber(exactArg(args, "roy"), 0f),
                        rotationZ = toNumber(exactArg(args, "roz"), 0f),
                        scaleX = toOptionalNumber(exactArg(args, "scalex")),
                        scaleXFrom = toOptionalNumber(exactArg(args, "scalexfrom")),
                        scaleXTo = toOptionalNumber(exactArg(args, "scalexto")),
                        scaleY = toOptionalNumber(exactArg(args, "scaley")),
                        scaleYFrom = toOptionalNumber(exactArg(args, "scaleyfrom")),
                        scaleYTo = toOptionalNumber(exactArg(args, "scaleyto")),
                        slot = slot,
                        style = style,
                        x = toOptionalNumber(exactArg(args, "x")),
                        xFrom = toOptionalNumber(exactArg(args, "xfrom")),
                        xTo = toOptionalNumber(exactArg(args, "xto")),
                        y = toOptionalNumber(exactArg(args, "y")),
                        yFrom = toOptionalNumber(exactArg(args, "yfrom")),
                        yTo = toOptionalNumber(exactArg(args, "yto"))
                    )
                )
                return CommandResult.CONTINUE
            }

            "playmusic" -> {
                val key = toStringParam(exactArg(args, "key"))
                if (key.isEmpty()) {
                    warn("parse", "playmusic key is empty")
                    return CommandResult.CONTINUE
                }
                if (!hasAudioKey(key)) {
                    warn("missing_asset", "music: $key")
                    return CommandResult.CONTINUE
                }
                val intro = toStringParam(exactArg(args, "intro")).takeIf { it.isNotEmpty() }
                audio.playMusic(
                    PlayMusicInput(
                        crossfadeMs = max(0L, (toNumber(exactArg(args, "crossfade"), 0.4f) * 1000f).roundToLong()),
                        delayMs = max(0L, (toNumber(exactArg(args, "delay"), 0f) * 1000f).roundToLong()),
                        intro = intro,
                        key = key,
                        volume = toNumber(exactArg(args, "volume"), 1f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "stopmusic" -> {
                audio.stopMusic(max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong()))
                return CommandResult.CONTINUE
            }

            "musicvolume" -> {
                audio.setMusicVolume(
                    volume = toNumber(exactArg(args, "volume"), 1f),
                    fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong())
                )
                return CommandResult.CONTINUE
            }

            "playsound" -> {
                val key = toStringParam(exactArg(args, "key"))
                if (key.isEmpty()) {
                    warn("parse", "playsound key is empty")
                    return CommandResult.CONTINUE
                }
                if (!hasAudioKey(key)) {
                    warn("missing_asset", "sound: $key")
                    return CommandResult.CONTINUE
                }
                audio.playSound(
                    PlaySoundInput(
                        channel = toStringParam(exactArg(args, "channel"), key),
                        delayMs = max(0L, (toNumber(exactArg(args, "delay"), 0f) * 1000f).roundToLong()),
                        key = key,
                        loop = toBoolean(exactArg(args, "loop"), false),
                        volume = toNumber(exactArg(args, "volume"), 1f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "stopsound" -> {
                val key = toStringParam(exactArg(args, "key"))
                audio.stopSound(
                    channel = toStringParam(exactArg(args, "channel"), key),
                    fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong())
                )
                return CommandResult.CONTINUE
            }

            "soundvolume" -> {
                audio.setSoundVolume(
                    channel = toStringParam(exactArg(args, "channel")),
                    volume = toNumber(exactArg(args, "volume"), 1f),
                    fadeMs = max(0L, (toNumber(exactArg(args, "fadetime"), 0f) * 1000f).roundToLong())
                )
                return CommandResult.CONTINUE
            }

            "cameraeffect" -> {
                val effect = toStringParam(exactArg(args, "effect"))
                val block = toBoolean(exactArg(args, "block"), false)
                val keep = toBoolean(exactArg(args, "keep"), false)
                val fadeMs = max(
                    0L,
                    (toNumber(exactArg(args, "fadetime"), 0.4f) * getCurrentSpeed().animateRatio * 1000f).roundToLong()
                )
                if (effect == "Chaos") {
                    warn("unsupported_visual", "cameraeffect:Chaos")
                    return CommandResult.CONTINUE
                }
                if (effect == "Colorinverse") {
                    renderer.setCameraEffect(effect, if (keep) 1f else 0f, 0L, false, true)
                    return CommandResult.CONTINUE
                }
                if (effect != "Grayscale") return CommandResult.CONTINUE

                renderer.setCameraEffect(
                    effect = effect,
                    amount = toNumber(exactArg(args, "amount"), 1f),
                    durationMs = fadeMs,
                    block = block,
                    keep = keep,
                    initialAmount = toOptionalNumber(exactArg(args, "initamount"))
                )
                return CommandResult.CONTINUE
            }

            "camerashake" -> {
                val durationRaw = toNumber(exactArg(args, "duration"), -1f)
                renderer.shakeCamera(
                    CameraShakeInput(
                        block = toBoolean(exactArg(args, "block"), false),
                        durationMs = if (durationRaw < 0f) 10_000L else max(0L, (durationRaw * 1000f).roundToLong()),
                        fadeOut = toBoolean(exactArg(args, "fadeout"), false),
                        infinite = durationRaw < 0f,
                        randomness = toNumber(exactArg(args, "randomness"), 90f),
                        stop = toBoolean(exactArg(args, "stop"), false),
                        vibrato = toNumber(exactArg(args, "vibrato"), 10f).toInt(),
                        xStrength = toNumber(exactArg(args, "xstrength"), 1f),
                        yStrength = toNumber(exactArg(args, "ystrength"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "focusout" -> {
                val durationMs = max(0L, (toNumber(exactArg(args, "duration"), 0f) * 1000f).roundToLong())
                val from = toOptionalNumber(exactArg(args, "from"))
                renderer.setFocusOut(
                    FocusOutInput(
                        block = durationMs > 0 && toBoolean(exactArg(args, "block"), false),
                        durationMs = durationMs,
                        from = if (from != null && from >= 0f) from else null,
                        id = toStringParam(exactArg(args, "id")),
                        to = toNumber(exactArg(args, "to"), 0f),
                        type = toStringParam(exactArg(args, "type"))
                    )
                )
                return CommandResult.CONTINUE
            }

            "focusparam" -> {
                val effect = toStringParam(exactArg(args, "effect"))
                val colorMode = when (effect) {
                    "Grayscale" -> FocusColorMode.GRAYSCALE
                    "Colorinverse" -> FocusColorMode.COLORINVERSE
                    else -> FocusColorMode.NONE
                }
                renderer.setFocusParam(
                    FocusParamInput(
                        blur = toBoolean(exactArg(args, "blur"), true),
                        color = colorMode
                    )
                )
                return CommandResult.CONTINUE
            }

            "multiline" -> {
                val text = line.trailingText
                if (text.isEmpty()) {
                    renderer.hideDialogue()
                    return CommandResult.CONTINUE
                }
                val shownChars = multilineShownChars
                multilineText += text
                multilineEnd = toBoolean(exactArg(args, "end"), false)
                displayedLineIndex = line.lineNumber
                startTypingDialogue(
                    speaker = toStringParam(exactArg(args, "name")),
                    text = multilineText,
                    delayScale = multilineTypeDelayScale(exactArg(args, "delay")),
                    executor = "multiline",
                    layoutText = text,
                    startIndex = shownChars
                )
                multilineShownChars = currentMessageLength
                return CommandResult.WAIT_INPUT
            }

            "subtitle" -> {
                val text = translateText(toStringParam(exactArg(args, "text")))
                if (text.isEmpty()) {
                    renderer.clearSubtitle(150)
                    return CommandResult.CONTINUE
                }
                val x = toNumber(exactArg(args, "x"), 0f)
                val y = toNumber(exactArg(args, "y"), 0f)
                if (x < 0 || x > 1280 || y < 0 || y > 720) return CommandResult.CONTINUE

                displayedLineIndex = line.lineNumber
                cancelTyping()
                currentMessageLength = parseRichChars(text).size
                currentRawMessageLength = text.length
                renderer.setSubtitle(
                    SubtitleInput(
                        alignment = toStringParam(exactArg(args, "alignment")).lowercase().takeIf { it in setOf("left", "center", "right") } ?: "left",
                        delayMs = getCurrentSpeed().typeWriterDelayMs,
                        onTypingComplete = { onTypingComplete() },
                        sizePx = toNumber(exactArg(args, "size"), 24f),
                        text = text,
                        widthPx = min(toNumber(exactArg(args, "width"), 1280f), 1280f - x),
                        x = x,
                        y = y
                    )
                )
                return CommandResult.WAIT_INPUT
            }

            "sticker" -> {
                val id = toStringParam(exactArg(args, "id"))
                if (id.isEmpty()) {
                    warn("parse", "sticker id is empty")
                    return CommandResult.CONTINUE
                }

                val multi = toBoolean(exactArg(args, "multi"), false)
                val existing = stickerSlots[id]
                val block = toBoolean(exactArg(args, "block"), existing == null)
                val duration = toNumber(exactArg(args, "duration"), -1f)
                val hideFadeMs = ((if (duration > 0f) duration else 0.15f) * 1000f).roundToLong()

                if (existing != null && !multi) {
                    stickerSlots.remove(id)
                    stickerPoolFadeMs.add(hideFadeMs)
                    renderer.clearSticker(id, hideFadeMs)
                    cancelTyping()
                    currentMessageLength = 0
                    currentRawMessageLength = 0
                    onTypingComplete()
                    return if (block) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
                }

                if (existing != null) {
                    val text = translateText(toStringParam(exactArg(args, "text")))
                    existing.charCount += parseRichChars(text).size
                    existing.rawLength += text.length
                    renderer.setSticker(
                        existing.layout.copy(
                            append = true,
                            id = id,
                            onTypingComplete = beginStickerTyping(existing.charCount, existing.rawLength),
                            text = text
                        )
                    )
                    return if (block) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
                }

                val text = translateText(toStringParam(exactArg(args, "text")))
                val x = toNumber(exactArg(args, "x"), 0f)
                val y = toNumber(exactArg(args, "y"), 0f)
                if (x < 0 || x > 1280 || y < 0 || y > 720 || text.isEmpty()) return CommandResult.CONTINUE

                val pooledFadeMs = if (stickerPoolFadeMs.isNotEmpty()) stickerPoolFadeMs.removeAt(0) else null
                val showFadeMs = if (multi && duration < 0f) {
                    pooledFadeMs ?: 150L
                } else {
                    ((if (duration >= 0f) duration else 0.15f) * 1000f).roundToLong()
                }

                val layout = StickerInput(
                    alignment = toStringParam(exactArg(args, "alignment")).lowercase().takeIf { it in setOf("left", "center", "right") } ?: "left",
                    append = false,
                    delayMs = stickerTypeDelayMs(exactArg(args, "delay")),
                    fadeMs = showFadeMs,
                    id = id,
                    sizePx = toNumber(exactArg(args, "size"), 24f),
                    text = text,
                    widthPx = min(toNumber(exactArg(args, "width"), 1280f), 1280f - x),
                    x = x,
                    y = y
                )
                val charCount = parseRichChars(text).size
                stickerSlots[id] = StickerSlotInfo(charCount, text.length, layout)
                renderer.setSticker(layout.copy(onTypingComplete = beginStickerTyping(charCount, text.length)))
                return if (block) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
            }

            "stickertween" -> {
                val id = toStringParam(exactArg(args, "id"))
                if (id.isEmpty() || !stickerSlots.containsKey(id)) return CommandResult.CONTINUE

                val join = toBoolean(exactArg(args, "join"), false)
                val isend = toBoolean(exactArg(args, "isend"), false)
                val block = toBoolean(exactArg(args, "block"), false)

                val pto = parseVector2(exactArg(args, "pto"))
                val rto = toOptionalNumber(exactArg(args, "rto"))
                val ato = exactArg(args, "ato")
                val hasGroup = ato != null || pto != null || rto != null

                val tween = StickerTweenInput(
                    alpha = if (ato != null) TweenTrackFloat(
                        durationMs = calculateFadeMs(exactArg(args, "aduration")),
                        from = toOptionalNumber(exactArg(args, "afrom")) ?: 0f,
                        to = toNumber(ato, 0f)
                    ) else null,
                    id = id,
                    isend = isend,
                    join = join,
                    position = if (pto != null) TweenTrackVec2(
                        durationMs = calculateFadeMs(exactArg(args, "pduration")),
                        from = parseVector2(exactArg(args, "pfrom")) ?: Vec2(0f, 0f),
                        to = pto
                    ) else null,
                    rotation = if (rto != null) TweenTrackFloat(
                        durationMs = calculateFadeMs(exactArg(args, "rduration")),
                        from = toOptionalNumber(exactArg(args, "rfrom")) ?: 0f,
                        to = rto
                    ) else null
                )
                if (hasGroup) renderer.stickerTween(tween)
                return if (block && isend) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
            }

            "timersticker" -> {
                renderer.setTimerSticker(
                    TimerStickerInput(
                        durationMs = max(0L, (toNumber(exactArg(args, "duration"), 1f) * 1000f).roundToLong()).let { if (it == 0L) 130L else it },
                        fromAlpha = toNumber(exactArg(args, "afrom"), 0f),
                        limitSeconds = exactArg(args, "time")?.let { toNumber(it, -1f).toInt() },
                        sizePx = toNumber(exactArg(args, "size"), 24f),
                        toAlpha = toNumber(exactArg(args, "ato"), 1f),
                        widthPx = toNumber(exactArg(args, "width"), 1280f),
                        x = toNumber(exactArg(args, "x"), 0f),
                        y = toNumber(exactArg(args, "y"), 0f)
                    )
                )
                return CommandResult.CONTINUE
            }

            "timerclear" -> {
                renderer.clearTimerSticker(
                    TimerClearInput(
                        durationMs = max(0L, (toNumber(exactArg(args, "duration"), 0.13f) * 1000f).roundToLong())
                    )
                )
                return CommandResult.CONTINUE
            }

            "stickerclear" -> {
                repeat(stickerSlots.size) { stickerPoolFadeMs.add(150L) }
                stickerSlots.clear()
                renderer.clearStickers(150)
                renderer.clearTimerSticker(TimerClearInput(0))
                return if (toBoolean(exactArg(args, "block"), false)) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
            }

            "spellsticker" -> {
                val id = toStringParam(exactArg(args, "id"))
                if (id.isEmpty()) {
                    warn("parse", "spellsticker id is empty")
                    return CommandResult.CONTINUE
                }
                val action = toStringParam(exactArg(args, "action"), "show")
                val block = toBoolean(exactArg(args, "block"), false)
                val alpha = toOptionalNumber(exactArg(args, "alpha"))

                if (action.equals("hide", ignoreCase = true)) {
                    renderer.hideSpellSticker(id)
                } else {
                    renderer.setSpellSticker(
                        SpellStickerInput(
                            alpha = alpha?.let { clamp(it, 0f, 1f) },
                            angle = toOptionalNumber(exactArg(args, "angle")),
                            content = translateText(line.content),
                            id = id,
                            style = toStringParam(exactArg(args, "style"), "sami"),
                            x = toOptionalNumber(exactArg(args, "x")),
                            xScale = toOptionalNumber(exactArg(args, "xScale")),
                            y = toOptionalNumber(exactArg(args, "y")),
                            yScale = toOptionalNumber(exactArg(args, "yScale"))
                        )
                    )
                }

                if (block) {
                    pendingInputEffect = if (action.equals("hide", ignoreCase = true)) null else {
                        { renderer.hideSpellSticker(id) }
                    }
                    return CommandResult.WAIT_INPUT
                }
                return CommandResult.CONTINUE
            }

            "spellstickerclear" -> {
                renderer.clearSpellStickers()
                pendingInputEffect = null
                return if (toBoolean(exactArg(args, "block"), false)) CommandResult.WAIT_INPUT else CommandResult.CONTINUE
            }

            "theater" -> {
                val mode = toBoolean(exactArg(args, "mode"), true)
                if (mode && !theaterMode) {
                    theaterAutoPlayCache = getAutoPlayState()
                    theaterMode = true
                    buttonSpeedLevel = 0
                    applyScriptAutoPlayMode(AutoPlayMode.BUTTON_AUTO)
                } else if (!mode && theaterMode) {
                    theaterMode = false
                    val cached = theaterAutoPlayCache
                    theaterAutoPlayCache = null
                    if (cached != null) {
                        buttonSpeedLevel = cached.buttonSpeedLevel
                        quickSpeedLevel = cached.quickSpeedLevel
                        applyScriptAutoPlayMode(if (cached.mode == AutoPlayMode.QUICK_PLAY) AutoPlayMode.DEFAULT else cached.mode)
                    }
                }
                return CommandResult.CONTINUE
            }

            "decision" -> {
                decisionSelectValue = 0
                decisionReferences = null
                val parsed = parseDecision(line, context.audioVariables)
                if (parsed == null) {
                    applyScriptAutoPlayMode(AutoPlayMode.DEFAULT)
                    warn("invalid_parameter", "decision options parameter is missing")
                    return CommandResult.CONTINUE
                }

                val options = parsed.options.map { it.label }
                val values = parsed.options.map { it.value }

                val policyPick = decisionPolicy?.invoke(line.lineNumber, options, values)
                if (policyPick != null && policyPick in options.indices) {
                    val value = values[policyPick]
                    decisionSelectValue = value
                    choiceHistory.add(RuntimeChoiceSelection(line.lineNumber, policyPick, value))
                    return CommandResult.CONTINUE
                }

                val autoPlayCache = autoPlayMode
                applyAutoPlayMode(AutoPlayMode.DEFAULT)
                state = PlayerState.WAITING_DECISION
                val selection = renderer.showDecision(options, values)
                decisionSelectValue = selection.value
                if (selection.optionIndex >= 0) {
                    choiceHistory.add(RuntimeChoiceSelection(line.lineNumber, selection.optionIndex, selection.value))
                }
                applyAutoPlayMode(autoPlayCache)
                state = PlayerState.RUNNING
                return CommandResult.CONTINUE
            }

            "predicate" -> {
                decisionReferences = parsePredicateReferences(line)
                return CommandResult.CONTINUE
            }

            else -> {
                if (line.command in UNSUPPORTED_AD8_COMMANDS) {
                    warn("unsupported_command", line.command)
                } else {
                    warn("unknown_command", line.command)
                }
                return CommandResult.CONTINUE
            }
        }
    }

    private fun waitForDialogueInput(speaker: String, text: String) {
        startTypingDialogue(speaker, text)
        state = PlayerState.WAITING_INPUT
    }

    private fun startTypingDialogue(
        speaker: String,
        text: String,
        delayScale: Float = 1f,
        executor: String = "dialog",
        layoutText: String = text,
        startIndex: Int = 0
    ) {
        cancelTyping()
        val translatedSpeaker = translateText(speaker)
        val translatedText = translateText(text)
        val richChars = parseRichChars(translatedText)
        val layout = DialogueLayout(
            executor = executor,
            text = parseRichChars(translateText(layoutText)).map { it.char }.joinToString("")
        )
        currentMessageLength = richChars.size
        currentRawMessageLength = translatedText.length
        currentTypingComplete = false

        val initialDelayMs = getTypeWriterDelayMs(delayScale)
        val from = startIndex.coerceIn(0, richChars.size)
        if (from >= richChars.size || initialDelayMs <= 0) {
            renderer.setDialogue(
                speaker = translatedSpeaker,
                text = richCharsToTaggedText(richChars),
                tagStyles = buildTagStyles(richChars),
                layout = layout
            )
            onTypingComplete()
            return
        }

        val sessionId = ++typingSessionId
        val tagStyles = buildTagStyles(richChars)
        typingSession = TypingSession(
            id = sessionId,
            speaker = translatedSpeaker,
            richChars = richChars,
            tagStyles = tagStyles
        )
        renderer.setDialogue(
            speaker = translatedSpeaker,
            text = richCharsToTaggedText(richChars.subList(0, from)),
            tagStyles = tagStyles,
            layout = layout
        )

        typingJob = coroutineScope.launch {
            runTyping(sessionId, translatedSpeaker, richChars, delayScale, from)
        }
    }

    private fun translateText(text: String): String = expandStoryText(text, context.audioVariables)

    private fun getTypeWriterDelayMs(delayScale: Float): Long {
        val delayMs = (getCurrentSpeed().typeWriterDelayMs * delayScale).roundToLong()
        return if (delayMs < 0) TYPE_WRITER_ORIGIN_DELAY_MS else delayMs
    }

    private suspend fun runTyping(
        sessionId: Int,
        speaker: String,
        richChars: List<RichChar>,
        delayScale: Float,
        startIndex: Int
    ) {
        for (index in startIndex until richChars.size) {
            val session = typingSession
            if (session == null || session.id != sessionId || destroyed) return

            val delayMs = getTypeWriterDelayMs(delayScale)
            if (delayMs > 0) delay(delayMs)

            val curSession = typingSession
            if (curSession == null || curSession.id != sessionId || destroyed) return

            val visible = richCharsToTaggedText(richChars.subList(0, index + 1))
            renderer.setDialogue(speaker, visible)
        }

        val finalDelayMs = getTypeWriterDelayMs(delayScale)
        if (finalDelayMs > 0) delay(finalDelayMs)
        if (typingSession?.id == sessionId) {
            typingSession = null
            onTypingComplete()
        }
    }

    private fun onTypingComplete() {
        currentTypingComplete = true
        scheduleAutoClick(currentRawMessageLength)
    }

    private fun beginStickerTyping(charCount: Int, rawLength: Int): () -> Unit {
        cancelTyping()
        currentMessageLength = charCount
        currentRawMessageLength = rawLength
        val sessionId = typingSessionId
        return {
            if (!destroyed && typingSessionId == sessionId) {
                onTypingComplete()
            }
        }
    }

    private fun finishTypingNow(): Boolean {
        val session = typingSession ?: return false
        cancelTyping()
        renderer.setDialogue(session.speaker, richCharsToTaggedText(session.richChars), session.tagStyles)
        return true
    }

    private fun resetMultiline() {
        multilineText = ""
        multilineEnd = false
        multilineShownChars = 0
    }

    private fun cancelTyping() {
        typingSession = null
        typingSessionId += 1
        typingJob?.cancel()
        typingJob = null
        currentTypingComplete = false
        cancelAutoClick()
    }

    private fun parseEnterDirection(value: Any?): String? {
        val raw = toStringParam(value)
        return if (raw in setOf("left", "right", "up", "down")) raw else null
    }

    private fun parseCharacterSlot(value: Any?): String? {
        return when (toStringParam(value).trim().lowercase()) {
            "char_left", "l", "left" -> "l"
            "c", "center", "char_middle", "m", "middle" -> "m"
            "char_right", "r", "right" -> "r"
            else -> null
        }
    }

    private fun resolveCharacterSlotFocusSlots(args: StoryCommandArgs): List<String> {
        val focus = toStringParam(args["focus"]).trim().lowercase()
        if (focus.isEmpty() || focus == "all") return listOf("l", "m", "r")

        val tokenMap = mapOf("l" to "l", "left" to "l", "m" to "m", "middle" to "m", "r" to "r", "right" to "r")
        return focus.split(",")
            .mapNotNull { tokenMap[it.trim()] }
            .distinct()
    }

    private fun parseCharacterActionSlot(value: Any?, type: String): String {
        return when (toStringParam(value).trim().lowercase()) {
            "left" -> "l"
            "right" -> "r"
            "middle" -> "m"
            else -> if (type == "exit") "m" else "l"
        }
    }
}