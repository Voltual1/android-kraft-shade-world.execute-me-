package me.voltual.asp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.voltual.asp.engine.StoryAsset
import me.voltual.asp.engine.StoryRuntime

@Composable
fun StoryPlayerScreen(
    runtime: StoryRuntime,
    modifier: Modifier = Modifier
) {
    val state by runtime.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                runtime.onUserClick()
            }
    ) {
        // 1. 背景渲染层
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
            // 左侧/Slot 1 立绘
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

            // 右侧/Slot 2 立绘
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

        // 3. 画面遮罩效果层 (Blocker)
        if (state.blocker.alpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(
                            red = state.blocker.r,
                            green = state.blocker.g,
                            blue = state.blocker.b,
                            alpha = state.blocker.alpha
                        )
                    )
            )
        }

        // 4. 对话框层
        StoryDialogueBox(
            dialogue = state.dialogue,
            playbackMode = state.playbackMode,
            onModeChange = { runtime.setPlaybackMode(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 5. 分支选项浮层
        StoryDecisionOverlay(
            decisionState = state.decision,
            onSelectOption = { runtime.selectDecision(it) }
        )
    }
}