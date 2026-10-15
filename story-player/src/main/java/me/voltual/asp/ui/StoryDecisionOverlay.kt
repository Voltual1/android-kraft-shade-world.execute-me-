package me.voltual.asp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.DecisionOption
import me.voltual.asp.engine.DecisionState

@Composable
fun StoryDecisionOverlay(
    decisionState: DecisionState,
    onSelectOption: (DecisionOption) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = decisionState.isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                decisionState.options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xF0222831), shape = RoundedCornerShape(8.dp))
                            .clickable { onSelectOption(option) }
                            .padding(vertical = 14.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.text,
                            color = Color(0xFFEEEEEE),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}