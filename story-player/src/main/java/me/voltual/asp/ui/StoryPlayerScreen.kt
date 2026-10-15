package me.voltual.asp.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        // 背景渲染层
        state.background.imageKey?.let { imageKey ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            ) {
                // 图片加载实现（可接入 Coil 或外部 Provider 渲染）
            }
        }

        // 遮罩效果层 (Blocker)
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

        // 对话框层
        StoryDialogueBox(
            dialogue = state.dialogue,
            playbackMode = state.playbackMode,
            onModeChange = { runtime.setPlaybackMode(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 分支选项浮层
        StoryDecisionOverlay(
            decisionState = state.decision,
            onSelectOption = { runtime.selectDecision(it) }
        )
    }
}