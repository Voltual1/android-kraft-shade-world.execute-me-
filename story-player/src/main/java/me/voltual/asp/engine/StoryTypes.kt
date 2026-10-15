package me.voltual.asp.engine

const val STORY_WIDTH = 1280
const val STORY_HEIGHT = 720

data class StoryFaceRect(
    val h: Int,
    val w: Int,
    val x: Int,
    val y: Int
)

sealed interface StoryCharacterGroup
data class StoryCharacterGroupFaceOverlay(val base: String, val faceRect: StoryFaceRect) : StoryCharacterGroup
data object StoryCharacterGroupSingle : StoryCharacterGroup

data class StoryCharacterEntry(
    val alias: String,
    val face: String?,
    val group: Int,
    val image: String?,
    val name: String
)

data class StoryLinkNode(
    val array: List<StoryCharacterEntry>,
    val groups: List<StoryCharacterGroup>,
    val pos: Pair<Float, Float>,
    val size: Pair<Float, Float>
)

sealed class ParsedLine {
    abstract val lineNumber: Int
    abstract val raw: String
}

data class ParsedCommandLine(
    override val lineNumber: Int,
    override val raw: String,
    val command: String,
    val args: Map<String, Any>,
    val content: String,
    val paramPresent: Boolean,
    val trailingText: String
) : ParsedLine()

data class ParsedDialogueLine(
    override val lineNumber: Int,
    override val raw: String,
    val speaker: String,
    val text: String
) : ParsedLine()

data class ParsedNarrationLine(
    override val lineNumber: Int,
    override val raw: String,
    val text: String
) : ParsedLine()

data class StoryMetadata(
    val args: Map<String, Any>,
    val characterSortType: String,
    val denyAutoSwitchScene: Boolean,
    val dontClearGameObjectPoolOnStart: Boolean,
    val fitMode: String,
    val id: String,
    val isAutoable: Boolean,
    val isSkippable: Boolean,
    val isTutorial: Boolean,
    val isVideoOnly: Boolean,
    val title: String
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

data class RuntimeLogPosition(
    val lineIndex: Int?,
    val selections: List<RuntimeChoiceSelection>
)

data class RuntimeWarning(
    val type: WarningType,
    val cursor: Int,
    val command: String? = null,
    val detail: String? = null,
    val lineNumber: Int? = null
) {
    enum class WarningType {
        ERROR,
        INVALID_PARAMETER,
        MISSING_ASSET,
        PARSE,
        UNSUPPORTED_COMMAND,
        UNSUPPORTED_VISUAL,
        UNKNOWN_COMMAND
    }
}

data class PlayMusicInput(
    val key: String,
    val volume: Float = 1f,
    val crossfadeMs: Long = 0L,
    val delayMs: Long = 0L,
    val intro: String? = null
)

data class PlaySoundInput(
    val key: String,
    val volume: Float = 1f,
    val channel: String = "",
    val loop: Boolean = false,
    val delayMs: Long = 0L
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