package me.voltual.asp.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import com.cardinalblue.kraftshade.model.GlVec4
import com.cardinalblue.kraftshade.shader.buffer.LoadedTexture
import me.voltual.asp.R
import me.voltual.asp.engine.StoryAsset
import me.voltual.asp.engine.StoryRuntime
import me.voltual.asp.shader.StorySlideMaskKraftShader

@Composable
fun StoryPlayerScreen(
    runtime: StoryRuntime,
    modifier: Modifier = Modifier
) {
    val state by runtime.state.collectAsState()
    val context = LocalContext.current

    val shakeOffsetX = state.cameraEffect.shakeOffsetX
    val shakeOffsetY = state.cameraEffect.shakeOffsetY

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(shakeOffsetX.toInt(), shakeOffsetY.toInt()) }
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                runtime.onUserClick()
            }
    ) {
        // 1. 背景层
        val bgKey = state.background.imageKey
        if (!bgKey.isNullOrBlank()) {
            val bgUrl = StoryAsset.resolveStoryAssetByKey(bgKey, StoryAsset.StoryAssetFamily.BACKGROUND)
            AsyncImage(
                model = bgUrl,
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
        }

        // 2. 角色立绘层 (Slot 1 & Slot 2)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            state.characterSlot1.characterRef?.let { ref ->
                val charUrl = StoryAsset.resolveStoryCharacterAssetByKey(ref)
                AsyncImage(
                    model = charUrl,
                    contentDescription = "Character Slot 1",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth(0.45f)
                )
            }

            state.characterSlot2.characterRef?.let { ref ->
                val charUrl = StoryAsset.resolveStoryCharacterAssetByKey(ref)
                AsyncImage(
                    model = charUrl,
                    contentDescription = "Character Slot 2",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth(0.45f)
                )
            }
        }

        // 3. 符文印章层 (SpellSticker)
        StorySpellStickerView(stickers = state.spellStickers)

        // 4. 地区开场印章动效层 (Location Stamp / AnimText)
        StoryLocationStamp(animTextState = state.animText)

        // 5. 遮罩层 (Blocker / SlideMask using KraftShade)
        val blocker = state.blocker
        if (blocker.alpha > 0f) {
            if (blocker.style == "slider" || blocker.style == "verticalslider") {
                val effectState = rememberKraftShadeEffectState()
                val maskBitmap = remember {
                    BitmapFactory.decodeResource(context.resources, R.drawable.slide_mask)
                }

                LaunchedEffect(blocker.alpha, blocker.style, blocker.inverse) {
                    effectState.setEffect { targetBuffer ->
                        pipeline(targetBuffer) {
                            val maskTexture = LoadedTexture(maskBitmap)
                            val shader = StorySlideMaskKraftShader().apply {
                                uAlpha = blocker.alpha
                                uVertical = if (blocker.style == "verticalslider") 1f else 0f
                                uFlipX = if (blocker.inverse && blocker.style == "slider") 1f else 0f
                                uFlipY = if (blocker.inverse && blocker.style == "verticalslider") 1f else 0f
                                uBlockerColor = GlVec4(blocker.r, blocker.g, blocker.b, 1f)
                                setSecondInputTexture(maskTexture)
                            }
                            step(shader, targetBuffer)
                        }
                    }
                }

                KraftShadeEffectView(
                    state = effectState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(
                                red = blocker.r,
                                green = blocker.g,
                                blue = blocker.b,
                                alpha = blocker.alpha
                            )
                        )
                )
            }
        }

        // 6. 对话框层
        StoryDialogueBox(
            dialogue = state.dialogue,
            playbackMode = state.playbackMode,
            onModeChange = { runtime.setPlaybackMode(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 7. 分支选项浮层
        StoryDecisionOverlay(
            decisionState = state.decision,
            onSelectOption = { runtime.selectDecision(it) }
        )
    }
}