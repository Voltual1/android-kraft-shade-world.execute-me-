package me.voltual.asp.engine

data class BackgroundState(
    val imageKey: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val duration: Float = 0f
)

data class CharacterSlotState(
    val characterRef: String? = null,
    val isFocused: Boolean = false
)

data class BlockerState(
    val r: Float = 0f,
    val g: Float = 0f,
    val b: Float = 0f,
    val alpha: Float = 0f,
    val duration: Float = 0f,
    val style: String = "default",
    val inverse: Boolean = false
)

data class CameraEffectState(
    val grayAmount: Float = 0f,
    val inverseAmount: Float = 0f,
    val shakeOffsetX: Float = 0f,
    val shakeOffsetY: Float = 0f
)

data class AnimTextState(
    val isVisible: Boolean = false,
    val mainText: String = "",
    val subText: String = "",
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val durationMs: Long = 5000L
)

data class SpellStickerItem(
    val id: String,
    val style: String = "sami",
    val mainText: String = "",
    val subText: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val angle: Float = 0f,
    val alpha: Float = 1f,
    val isVisible: Boolean = true
)

data class DecisionOption(
    val text: String,
    val value: String?
)

data class DecisionState(
    val isVisible: Boolean = false,
    val options: List<DecisionOption> = emptyList()
)

data class DialogueState(
    val speaker: String? = null,
    val text: String = "",
    val isTyping: Boolean = false,
    val fullText: String = ""
)

enum class PlaybackMode {
    MANUAL,
    AUTO,
    SKIP
}

data class StoryDisplayState(
    val currentLineIndex: Int = 0,
    val totalLines: Int = 0,
    val background: BackgroundState = BackgroundState(),
    val characterSlot1: CharacterSlotState = CharacterSlotState(),
    val characterSlot2: CharacterSlotState = CharacterSlotState(),
    val blocker: BlockerState = BlockerState(),
    val cameraEffect: CameraEffectState = CameraEffectState(),
    val animText: AnimTextState = AnimTextState(),
    val spellStickers: Map<String, SpellStickerItem> = emptyMap(),
    val dialogue: DialogueState = DialogueState(),
    val decision: DecisionState = DecisionState(),
    val playbackMode: PlaybackMode = PlaybackMode.MANUAL,
    val isEnded: Boolean = false
)