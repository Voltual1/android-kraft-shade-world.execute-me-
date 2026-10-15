package me.voltual.asp.engine

import kotlinx.serialization.Serializable

const val STORY_WIDTH: Int = 1280
const val STORY_HEIGHT: Int = 720

typealias StoryCommandValue = Any
typealias StoryCommandArgs = Map<String, StoryCommandValue>

@Serializable
data class StoryFaceRect(
    val h: Int,
    val w: Int,
    val x: Int,
    val y: Int
)

sealed interface StoryCharacterGroup {
    val mode: String

    @Serializable
    data class FaceOverlay(
        val base: String,
        val faceRect: StoryFaceRect,
        override val mode: String = "face_overlay"
    ) : StoryCharacterGroup

    @Serializable
    data class Single(
        override val mode: String = "single"
    ) : StoryCharacterGroup
}

@Serializable
data class StoryCharacterEntry(
    val alias: String = "",
    val face: String? = null,
    val group: Int = -1,
    val image: String? = null,
    val name: String
)

@Serializable
data class Vec2(val x: Float, val y: Float)

@Serializable
data class StoryLinkNode(
    val array: List<StoryCharacterEntry> = emptyList(),
    val groups: List<StoryCharacterGroup> = emptyList(),
    val pos: Vec2 = Vec2(0f, 0f),
    val size: Vec2 = Vec2(0f, 0f)
)

sealed class ParsedLine {
    abstract val lineNumber: Int
    abstract val raw: String

    data class Command(
        override val lineNumber: Int,
        override val raw: String,
        val command: String,
        val args: StoryCommandArgs,
        val content: String,
        val paramPresent: Boolean,
        val trailingText: String
    ) : ParsedLine()

    data class Dialogue(
        override val lineNumber: Int,
        override val raw: String,
        val speaker: String,
        val text: String
    ) : ParsedLine()

    data class Narration(
        override val lineNumber: Int,
        override val raw: String,
        val text: String
    ) : ParsedLine()
}

data class StoryMetadata(
    val args: StoryCommandArgs = emptyMap(),
    val characterSortType: String = "DEFAULT",
    val denyAutoSwitchScene: Boolean = false,
    val dontClearGameObjectPoolOnStart: Boolean = false,
    val fitMode: String = "DEFAULT",
    val id: String = "",
    val isAutoable: Boolean = true,
    val isSkippable: Boolean = true,
    val isTutorial: Boolean = false,
    val isVideoOnly: Boolean = false,
    val title: String = ""
)

enum class PlayerState {
    ERROR,
    FINISHED,
    IDLE,
    RUNNING,
    WAITING_DECISION,
    WAITING_INPUT,
    WAITING_TIMER,
    WAITING_VIDEO
}

enum class AutoPlayMode {
    BUTTON_AUTO,
    DEFAULT,
    QUICK_PLAY
}

data class AutoPlayState(
    val buttonSpeedLevel: Int,
    val mode: AutoPlayMode,
    val quickSpeedLevel: Int
)

data class RuntimeChoiceSelection(
    val decisionId: Int,
    val optionIndex: Int,
    val value: Int
)

data class DecisionSelection(
    val optionIndex: Int,
    val value: Int
)

typealias DecisionPolicy = (decisionId: Int, options: List<String>, values: List<Int>) -> Int?

enum class LineSeekPhase {
    SEEKING,
    REACHED,
    MISSED,
    ABORTED
}

data class LineSeekUpdate(
    val phase: LineSeekPhase,
    val reason: String? = null,
    val target: Int
)

data class RuntimeLogPosition(
    val lineIndex: Int?,
    val selections: List<RuntimeChoiceSelection>
)

data class RuntimeWarning(
    val command: String? = null,
    val cursor: Int,
    val detail: String? = null,
    val lineNumber: Int? = null,
    val type: String
)

data class ColorRgba(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float
)

data class CharacterSlotInput(
    val action: String? = null,
    val angle: Float? = null,
    val alphaFrom: Float? = null,
    val alphaTo: Float? = null,
    val blackEnd: Float? = null,
    val blackStart: Float? = null,
    val block: Boolean = false,
    val characterKey: String? = null,
    val circles: Int? = null,
    val dimmed: Boolean = false,
    val durationMs: Long? = null,
    val enterFrom: String? = null,
    val enterPosition: Vec2? = null,
    val expression: String? = null,
    val fadeIdentity: String? = null,
    val focus: Int? = null,
    val focusMode: String? = null,
    val focusSlots: List<String>? = null,
    val inverse: Boolean? = null,
    val nativeKey: String? = null,
    val positionFrom: Vec2? = null,
    val positionTo: Vec2? = null,
    val posZoom: Vec2? = null,
    val power: Float? = null,
    val randomness: Int? = null,
    val replaceFadeMs: Long? = null,
    val scaleX: Float? = null,
    val scaleY: Float? = null,
    val slot: String,
    val slotSequence: Boolean = false,
    val stop: Boolean = false,
    val times: Int? = null,
    val transType: Int? = null
)

data class CharacterActionInput(
    val block: Boolean,
    val direction: String? = null,
    val durationMs: Long,
    val power: Float,
    val randomness: Float,
    val rotationFromDeg: Float,
    val rotationLeftDeg: Float,
    val rotationRightDeg: Float,
    val scaleX: Float? = null,
    val scaleY: Float? = null,
    val slot: String,
    val stop: Boolean,
    val times: Int,
    val type: String,
    val xOffset: Float,
    val yOffset: Float
)

data class BlockerInput(
    val block: Boolean,
    val fadeMs: Long,
    val from: ColorRgba,
    val image: String? = null,
    val inverse: Boolean,
    val style: String,
    val to: ColorRgba
)

data class CurtainInput(
    val alphaFrom: Float? = null,
    val alphaTo: Float? = null,
    val block: Boolean,
    val delayMs: Long,
    val direction: Int,
    val fadeMs: Long,
    val fillFrom: Float? = null,
    val fillTo: Float,
    val grad: Boolean
)

data class CameraShakeInput(
    val block: Boolean,
    val durationMs: Long,
    val fadeOut: Boolean,
    val infinite: Boolean,
    val randomness: Float,
    val stop: Boolean,
    val vibrato: Int,
    val xStrength: Float,
    val yStrength: Float
)

enum class FocusColorMode {
    COLORINVERSE,
    GRAYSCALE,
    NONE
}

data class FocusOutInput(
    val block: Boolean,
    val durationMs: Long,
    val from: Float? = null,
    val id: String,
    val to: Float,
    val type: String
)

data class FocusParamInput(
    val blur: Boolean,
    val color: FocusColorMode
)

data class BackgroundInput(
    val block: Boolean = false,
    val fadeMs: Long = 0,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val screenAdapt: String? = null,
    val tiled: Boolean = false,
    val width: Float? = null,
    val height: Float? = null,
    val x: Float = 0f,
    val y: Float = 0f
)

data class BackgroundTweenInput(
    val block: Boolean,
    val durationMs: Long,
    val ease: String? = null,
    val loop: Boolean = false,
    val xFrom: Float? = null,
    val xScaleFrom: Float? = null,
    val xScaleTo: Float? = null,
    val xTo: Float? = null,
    val yFrom: Float? = null,
    val yScaleFrom: Float? = null,
    val yScaleTo: Float? = null,
    val yTo: Float? = null
)

data class ImageTweenInput(
    val block: Boolean,
    val durationMs: Long,
    val ease: String,
    val loop: Boolean = false,
    val xFrom: Float? = null,
    val xScaleFrom: Float? = null,
    val xScaleTo: Float? = null,
    val xTo: Float? = null,
    val yFrom: Float? = null,
    val yScaleFrom: Float? = null,
    val yScaleTo: Float? = null,
    val yTo: Float? = null
)

data class ImageRotateInput(
    val angleDeg: Float,
    val block: Boolean,
    val circles: Int,
    val durationMs: Long,
    val inverse: Boolean
)

data class LargeBackgroundTweenInput(
    val block: Boolean,
    val durationMs: Long,
    val ease: String? = null,
    val loop: Boolean = false,
    val xFrom: Float? = null,
    val xScaleFrom: Float? = null,
    val xScaleTo: Float? = null,
    val xTo: Float? = null,
    val yFrom: Float? = null,
    val yScaleFrom: Float? = null,
    val yScaleTo: Float? = null,
    val yTo: Float? = null
)

data class GridBackgroundInput(
    val assetKind: String? = null,
    val block: Boolean,
    val fadeMs: Long,
    val imageKeys: List<String>,
    val initPositionMode: String? = null,
    val layout: String? = null,
    val scaleX: Float,
    val scaleY: Float,
    val solidHeights: List<Float>,
    val solidWidths: List<Float>,
    val x: Float,
    val y: Float
)

data class ShowItemInput(
    val blackAlpha: Float,
    val block: Boolean,
    val fadeMs: Long,
    val key: String,
    val offsetX: Float,
    val offsetY: Float
)

data class CgItemInput(
    val assetKey: String,
    val alphaDelayMs: Long,
    val alphaDurationMs: Long,
    val alphaFrom: Float,
    val alphaTo: Float,
    val block: Boolean,
    val colorFrom: ColorRgba? = null,
    val colorTo: ColorRgba? = null,
    val ease: String,
    val height: Int,
    val key: String,
    val positionDelayMs: Long,
    val positionDurationMs: Long,
    val positionFrom: Vec2? = null,
    val positionTo: Vec2? = null,
    val rotationDurationMs: Long,
    val rotationFrom: Float,
    val rotationTo: Float,
    val scaleDelayMs: Long,
    val scaleDurationMs: Long,
    val scaleFrom: Float,
    val scaleTo: Float,
    val width: Int
)

data class DialogueLayout(
    val executor: String,
    val text: String
)

data class SubtitleInput(
    val alignment: String,
    val delayMs: Long,
    val onTypingComplete: (() -> Unit)? = null,
    val sizePx: Float,
    val text: String,
    val widthPx: Float,
    val x: Float,
    val y: Float
)

data class StickerInput(
    val alignment: String,
    val delayMs: Long,
    val onTypingComplete: (() -> Unit)? = null,
    val sizePx: Float,
    val text: String,
    val widthPx: Float,
    val x: Float,
    val y: Float,
    val append: Boolean,
    val fadeMs: Long,
    val id: String
)

data class TweenTrackFloat(val durationMs: Long, val from: Float, val to: Float)
data class TweenTrackVec2(val durationMs: Long, val from: Vec2, val to: Vec2)

data class StickerTweenInput(
    val alpha: TweenTrackFloat? = null,
    val id: String,
    val isend: Boolean,
    val join: Boolean,
    val position: TweenTrackVec2? = null,
    val rotation: TweenTrackFloat? = null
)

data class SpellStickerInput(
    val alpha: Float? = null,
    val angle: Float? = null,
    val content: String,
    val id: String,
    val style: String,
    val x: Float? = null,
    val xScale: Float? = null,
    val y: Float? = null,
    val yScale: Float? = null
)

data class AnimTextInput(
    val block: Boolean,
    val content: String,
    val id: String,
    val name: String,
    val position: Vec2,
    val style: String
)

data class AvgDisplayInput(
    val alphaFrom: Float? = null,
    val alphaTo: Float? = null,
    val block: Boolean,
    val durationMs: Long,
    val entryFrom: Float? = null,
    val entryIndex: Int,
    val entryTo: Float? = null,
    val id: String,
    val layer: Int,
    val name: String,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val scaleX: Float? = null,
    val scaleXFrom: Float? = null,
    val scaleXTo: Float? = null,
    val scaleY: Float? = null,
    val scaleYFrom: Float? = null,
    val scaleYTo: Float? = null,
    val slot: String,
    val style: String,
    val x: Float? = null,
    val xFrom: Float? = null,
    val xTo: Float? = null,
    val y: Float? = null,
    val yFrom: Float? = null,
    val yTo: Float? = null
)

data class TimerStickerInput(
    val durationMs: Long,
    val fromAlpha: Float,
    val limitSeconds: Int? = null,
    val sizePx: Float,
    val toAlpha: Float,
    val widthPx: Float,
    val x: Float,
    val y: Float
)

data class TimerClearInput(
    val durationMs: Long
)

data class CharacterCutinInput(
    val block: Boolean,
    val characterKey: String? = null,
    val characterMissing: Boolean = false,
    val charOffsetX: Float? = null,
    val charOffsetY: Float? = null,
    val expression: String? = null,
    val fadeMs: Long,
    val fadeStyle: String? = null,
    val offsetX: Float? = null,
    val offsetY: Float? = null,
    val povX: Float? = null,
    val povY: Float? = null,
    val widgetId: String,
    val width: Float? = null,
    val zoom: Float? = null
)

data class InterludeInput(
    val alphaDurationMs: Long,
    val alphaFrom: Float,
    val alphaTo: Float,
    val avatarCharacterKey: String? = null,
    val avatarExpression: String? = null,
    val block: Boolean,
    val channel: Int,
    val charName: String,
    val clear: Boolean,
    val direction: String,
    val durationMs: Long,
    val maskId: String,
    val name: String,
    val offset: Vec2,
    val positionFrom: Vec2? = null,
    val positionTo: Vec2? = null,
    val scaleDurationMs: Long,
    val scaleFrom: Vec2? = null,
    val scaleTo: Vec2? = null,
    val size: Vec2,
    val slot: String,
    val style: Int,
    val switchOn: Boolean,
    val templateSizeDurationMs: Long,
    val templateSizeFrom: Vec2,
    val templateSizeTo: Vec2,
    val type: Int
)

data class PlayMusicInput(
    val crossfadeMs: Long,
    val delayMs: Long,
    val intro: String? = null,
    val key: String,
    val volume: Float
)

data class PlaySoundInput(
    val channel: String,
    val delayMs: Long,
    val key: String,
    val loop: Boolean,
    val volume: Float
)

interface StoryAudio {
    fun destroy()
    suspend fun playMusic(input: PlayMusicInput)
    suspend fun playSound(input: PlaySoundInput)
    suspend fun setMusicVolume(volume: Float, fadeMs: Long)
    suspend fun setSoundVolume(channel: String, volume: Float, fadeMs: Long)
    suspend fun stopMusic(fadeMs: Long)
    suspend fun stopSound(channel: String, fadeMs: Long)
}

interface StoryRenderer {
    suspend fun clearCgItems(key: String? = null, fadeMs: Long = 130, ease: String = "Linear", block: Boolean = false)
    suspend fun clearAvgDisplays()
    suspend fun clearAnimTexts()
    suspend fun setCameraEffect(effect: String, amount: Float, durationMs: Long, block: Boolean, keep: Boolean, initialAmount: Float? = null)
    suspend fun setFocusOut(input: FocusOutInput)
    fun setFocusParam(input: FocusParamInput)
    suspend fun clearBackground(fadeMs: Long = 0, block: Boolean = false)
    suspend fun clearCharacters(slot: String? = null, fadeMs: Long = 0)
    suspend fun clearCurtains(fadeMs: Long = 0, block: Boolean = false)
    suspend fun clearGridBackground(fadeMs: Long = 0, block: Boolean = false)
    suspend fun clearImage(fadeMs: Long = 0, block: Boolean = false)
    suspend fun clearLargeImage(fadeMs: Long = 0, block: Boolean = false)
    suspend fun clearItems(fadeMs: Long = 0, block: Boolean = false)
    suspend fun clearSticker(id: String? = null, fadeMs: Long = 0)
    suspend fun clearStickers(fadeMs: Long = 0)
    suspend fun clearSpellStickers()
    suspend fun clearSubtitle(fadeMs: Long = 0)
    suspend fun clearTimerSticker(input: TimerClearInput? = null)
    suspend fun clearCharacterCutin(widgetId: String? = null)
    suspend fun clearInterludes()
    fun destroy()
    fun finishTextTyping(): Boolean
    suspend fun playVideo(url: String)
    suspend fun setAnimText(input: AnimTextInput)
    suspend fun setAvgDisplay(input: AvgDisplayInput)
    suspend fun setBackground(key: String, input: BackgroundInput? = null)
    suspend fun setBackgroundTween(input: BackgroundTweenInput)
    suspend fun setBlocker(input: BlockerInput)
    suspend fun setCharacter(input: CharacterSlotInput)
    suspend fun setCharacterCutin(input: CharacterCutinInput)
    suspend fun setCurtain(input: CurtainInput)
    suspend fun setGridBackground(input: GridBackgroundInput)
    suspend fun setLargeImage(input: GridBackgroundInput)
    suspend fun runCharacterAction(input: CharacterActionInput)
    fun setDialogue(speaker: String, text: String, tagStyles: Map<String, RichTagStyle>? = null, layout: DialogueLayout? = null)
    fun hideDialogue()
    fun finishDialogueCommand()
    suspend fun setImage(key: String, input: BackgroundInput? = null)
    suspend fun setImageRotate(input: ImageRotateInput)
    suspend fun setImageTween(input: ImageTweenInput)
    suspend fun setInterlude(input: InterludeInput)
    suspend fun setLargeBackgroundTween(input: LargeBackgroundTweenInput)
    suspend fun setLargeImageTween(input: LargeBackgroundTweenInput)
    suspend fun showItem(input: ShowItemInput)
    suspend fun showCgItem(input: CgItemInput)
    suspend fun setSticker(input: StickerInput)
    suspend fun stickerTween(input: StickerTweenInput)
    suspend fun setSpellSticker(input: SpellStickerInput)
    suspend fun hideSpellSticker(id: String)
    suspend fun setSubtitle(input: SubtitleInput)
    suspend fun setTimerSticker(input: TimerStickerInput)
    suspend fun shakeCamera(input: CameraShakeInput)
    suspend fun showDecision(options: List<String>, values: List<Int>): DecisionSelection
    fun stopVideo()
}

interface StoryPlayer {
    suspend fun advance()
    fun canSkipNode(): Boolean
    fun destroy()
    fun getAutoPlayState(): AutoPlayState
    fun getDecisionSelectValue(): Int
    fun getDisplayedLineIndex(): Int?
    fun getLogPosition(): RuntimeLogPosition
    fun getState(): PlayerState
    fun onDisplayedLineChange(listener: (Int?) -> Unit): () -> Unit
    fun setAutoPlayMode(mode: AutoPlayMode)
    fun setAutoPlaySpeedLevel(level: Int)
    fun setDecisionPolicy(policy: DecisionPolicy?)
    fun seekToLine(target: Int, choices: Map<Int, Int>, onUpdate: (LineSeekUpdate) -> Unit): () -> Unit
    suspend fun skipNode()
    suspend fun start()
}

data class RuntimeOptions(
    val animateRatio: Float = 1f,
    val firstRead: Boolean = true,
    val onWarning: ((RuntimeWarning) -> Unit)? = null,
    val typingIntervalMs: Long = 40
)