package me.voltual.asp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.DialogueState
import me.voltual.asp.engine.PlaybackMode

@Composable
fun StoryDialogueBox(
    dialogue: DialogueState,
    playbackMode: PlaybackMode,
    onModeChange: (PlaybackMode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dialogue.fullText.isEmpty() && dialogue.speaker.isNullOrEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 模式切换面板 (AUTO / SKIP / MANUAL)
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaybackModeChip(
                text = "AUTO",
                isActive = playbackMode == PlaybackMode.AUTO,
                onClick = {
                    val next = if (playbackMode == PlaybackMode.AUTO) PlaybackMode.MANUAL else PlaybackMode.AUTO
                    onModeChange(next)
                }
            )
            PlaybackModeChip(
                text = "SKIP",
                isActive = playbackMode == PlaybackMode.SKIP,
                onClick = {
                    val next = if (playbackMode == PlaybackMode.SKIP) PlaybackMode.MANUAL else PlaybackMode.SKIP
                    onModeChange(next)
                }
            )
        }

        // 说话者标签
        if (!dialogue.speaker.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC1E1E1E), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = dialogue.speaker,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 对话文本框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDC000000), shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topEnd = 8.dp))
                .padding(16.dp)
                .defaultMinSize(minHeight = 80.dp)
        ) {
            Text(
                text = dialogue.text,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun PlaybackModeChip(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isActive) Color(0xFF00ADB5) else Color(0x66000000),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}