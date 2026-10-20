package me.voltual.asp.engine.rendering

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.voltual.asp.engine.ColorRgba
import me.voltual.asp.engine.DialogueLayout
import me.voltual.asp.engine.FocusColorMode
import me.voltual.asp.engine.RichTagStyles
import me.voltual.asp.engine.Vec2

data class BackgroundVisualState(
    val textureKey: String? = null,
    val alpha: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val widthMultiplier: Float = 1f,
    val heightMultiplier: Float = 1f,
    val tiled: Boolean = false,
    val screenAdapt: String? = null
)

data class GridBackgroundVisualState(
    val imageKeys: List<String> = emptyList(),
    val assetKind: String = "background",
    val layout: String = "large",
    val alpha: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val solidWidths: List<Float> = emptyList(),
    val solidHeights: List<Float> = emptyList(),
    val initPositionMode: String = "default"
)

data class CharacterVisualSlot(
    val slot: String,
    val characterKey: String? = null,
    val expression: String? = null,
    val alpha: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val actionX: Float = 0f,
    val actionY: Float = 0f,
    val zoomShiftX: Float = 0f,
    val zoomShiftY: Float = 0f,
    val shakeOffsetX: Float = 0f,
    val shakeOffsetY: Float = 0f,
    val jumpOffsetY: Float = 0f,
    val rotationDeg: Float = 0f,
    val dimmed: Boolean = false,
    val zIndex: Float = 0f,
    val oldCharacterKey: String? = null,
    val oldExpression: String? = null,
    val oldCharAlpha: Float = 0f
) {
    val totalOffsetX: Float get() = actionX + zoomShiftX + shakeOffsetX
    val totalOffsetY: Float get() = shakeOffsetY - actionY - zoomShiftY - jumpOffsetY
}

data class BlockerVisualState(
    val visible: Boolean = false,
    val color: ColorRgba = ColorRgba(0f, 0f, 0f, 0f),
    val style: String = "default",
    val image: String? = null,
    val inverse: Boolean = false,
    val alpha: Float = 0f,
    val flipX: Boolean = false,
    val flipY: Boolean = false
)

data class CurtainVisualState(
    val direction: Int,
    val fill: Float,
    val alpha: Float,
    val grad: Boolean
)

data class DialogueVisualState(
    val visible: Boolean = false,
    val speaker: String = "",
    val text: String = "",
    val tagStyles: RichTagStyles? = null,
    val layout: DialogueLayout? = null,
    val isExpanded: Boolean = false
)

data class SubtitleVisualState(
    val visible: Boolean = false,
    val text: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val sizePx: Float = 24f,
    val widthPx: Float = 1280f,
    val alignment: String = "left",
    val alpha: Float = 1f
)

data class StickerVisualState(
    val id: String,
    val visible: Boolean = false,
    val text: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val sizePx: Float = 24f,
    val widthPx: Float = 1280f,
    val alignment: String = "left",
    val alpha: Float = 1f,
    val rotationDeg: Float = 0f
)

data class DecisionVisualState(
    val visible: Boolean = false,
    val options: List<String> = emptyList(),
    val values: List<Int> = emptyList(),
    val onSelect: (Int) -> Unit = {}
)

data class VideoVisualState(
    val visible: Boolean = false,
    val url: String = "",
    val onEnded: () -> Unit = {}
)

data class CameraVisualState(
    val shakeX: Float = 0f,
    val shakeY: Float = 0f,
    val grayscale: Float = 0f,
    val colorInverse: Float = 0f
)

data class FocusVisualState(
    val blur: Boolean = true,
    val colorMode: FocusColorMode = FocusColorMode.NONE,
    val amounts: Map<String, Float> = emptyMap()
) {
    fun getAmount(target: String): Float = amounts[target] ?: 0f
}

data class CharacterCutinSlotState(
    val widgetId: String,
    val characterKey: String? = null,
    val expression: String? = null,
    val alpha: Float = 1f,
    val rootX: Float = 640f,
    val rootY: Float = 360f,
    val maskW: Float = 200f,
    val maskH: Float = 720f,
    val targetW: Float = 200f,
    val targetH: Float = 720f,
    val povX: Float = 0f,
    val povY: Float = 0f,
    val zoom: Float = 1f,
    val charSlotX: Float = 0f,
    val charSlotY: Float = 360f,
    val fadeStyle: String = "fade",
    val oldCharacterKey: String? = null,
    val oldExpression: String? = null,
    val oldCharAlpha: Float = 0f
)

data class CgItemVisualSlot(
    val key: String,
    val assetKey: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val tintRgb: ColorRgba? = null,
    val rotationDeg: Float = 0f,
    val width: Int = 0,
    val height: Int = 0
)

data class AnimTextVisualItem(
    val id: String,
    val sessionId: Int,
    val mainText: String,
    val subText: String,
    val style: String,
    val positionX: Float,
    val positionY: Float,
    val progress: Float = 0f
)

data class SpellStickerVisualState(
    val id: String,
    val style: String = "sami",
    val mainText: String = "",
    val subText: String = "",
    val visible: Boolean = true,
    val alpha: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val angle: Float = 0f
)

data class ShowItemVisualState(
    val visible: Boolean = false,
    val key: String = "",
    val blackAlpha: Float = 0.4f,
    val alpha: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class TimerVisualState(
    val visible: Boolean = false,
    val timeFormatted: String = "",
    val alpha: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val sizePx: Float = 24f,
    val widthPx: Float = 1280f
)

data class InterludeElementVisualState(
    val slot: String,
    val name: String,
    val type: Int,
    val avatarCharacterKey: String? = null,
    val avatarExpression: String? = null,
    val visible: Boolean = true,
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val alpha: Float = 1f
)

data class InterludeChannelVisualState(
    val channel: Int,
    val visible: Boolean = true,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val baseSizeX: Float = 0f,
    val baseSizeY: Float = 0f,
    val maskWidth: Float = 0f,
    val maskHeight: Float = 0f,
    val charName: String = "",
    val elements: Map<String, InterludeElementVisualState> = emptyMap()
)

data class AvgDisplayVisualItem(
    val id: String,
    val name: String,
    val style: String,
    val slot: String,
    val layer: Int = 1,
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationZ: Float = 0f,
    val alpha: Float = 1f,
    val visible: Boolean = true
)

class StoryRendererState {
    var background by mutableStateOf<BackgroundVisualState?>(null)
    var previousBackground by mutableStateOf<BackgroundVisualState?>(null)

    var foregroundImage by mutableStateOf<BackgroundVisualState?>(null)
    var previousForegroundImage by mutableStateOf<BackgroundVisualState?>(null)

    var imageRotationDeg by mutableStateOf(0f)
    var gridBackground by mutableStateOf<GridBackgroundVisualState?>(null)
    val characters = mutableStateMapOf<String, CharacterVisualSlot>()
    var blocker by mutableStateOf(BlockerVisualState())
    val curtains = mutableStateMapOf<Int, CurtainVisualState>()
    val cutinSlots = mutableStateMapOf<String, CharacterCutinSlotState>()
    val interludes = mutableStateMapOf<Int, InterludeChannelVisualState>()
    val cgItems = mutableStateMapOf<String, CgItemVisualSlot>()
    val animTexts = mutableStateListOf<AnimTextVisualItem>()
    val spellStickers = mutableStateMapOf<String, SpellStickerVisualState>()
    val avgDisplays = mutableStateMapOf<String, AvgDisplayVisualItem>()
    var dialogue by mutableStateOf(DialogueVisualState())
    var subtitle by mutableStateOf(SubtitleVisualState())
    val stickers = mutableStateMapOf<String, StickerVisualState>()
    var decision by mutableStateOf(DecisionVisualState())
    var video by mutableStateOf(VideoVisualState())
    var camera by mutableStateOf(CameraVisualState())
    var focus by mutableStateOf(FocusVisualState())
    var showItem by mutableStateOf<ShowItemVisualState?>(null)
    var timerSticker by mutableStateOf(TimerVisualState())
}