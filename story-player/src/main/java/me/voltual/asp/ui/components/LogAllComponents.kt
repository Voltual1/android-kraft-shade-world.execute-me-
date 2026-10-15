package me.voltual.asp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.asp.engine.RuntimeChoiceSelection
import me.voltual.asp.engine.log.*

@Composable
fun LogAllPanel(
    document: LogDocument,
    activeLineIndex: Int?,
    selections: List<RuntimeChoiceSelection>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .width(520.dp),
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOG ALL (剧情文本)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose) {
                    Text("关闭")
                }
            }
            if (document.degraded) {
                Text(
                    text = "分支组合过多，部分内容未按选择路线区分",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LogAllList(
                document = document,
                activeLineIndex = activeLineIndex,
                selections = selections,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun LogAllList(
    document: LogDocument,
    activeLineIndex: Int?,
    selections: List<RuntimeChoiceSelection>,
    modifier: Modifier = Modifier
) {
    val assignment = remember(selections) {
        selections.associate { it.decisionId to it.optionIndex }
    }
    val listState = rememberLazyListState()

    // Find the item index corresponding to active line index
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex != null) {
            val flatBlocks = document.blocks
            val targetIdx = flatBlocks.indexOfFirst { block ->
                when (block) {
                    is LogBlock.Lines -> block.entries.any { it.lineIndex == activeLineIndex }
                    is LogBlock.Choice -> block.lineIndex == activeLineIndex
                    is LogBlock.Conditional -> false
                }
            }
            if (targetIdx != -1) {
                listState.animateScrollToItem(targetIdx)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(document.blocks) { block ->
            RenderLogBlock(block, document, activeLineIndex, assignment)
        }
    }
}

@Composable
private fun RenderLogBlock(
    block: LogBlock,
    document: LogDocument,
    activeLineIndex: Int?,
    assignment: Map<Int, Int>
) {
    val audience = block.audience
    val isOnPath = remember(audience, assignment) {
        if (audience == TRUE_CONDITION) true
        else document.conditions.evaluatePartial(audience, assignment)
    }
    val alpha = if (isOnPath == false) 0.4f else 1f

    Box(modifier = Modifier.fillMaxWidth().alpha(alpha)) {
        when (block) {
            is LogBlock.Lines -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.entries.forEach { entry ->
                        val isActive = activeLineIndex != null && entry.lineIndex == activeLineIndex && isOnPath == true
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isActive) "▶" else "",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp),
                                textAlign = TextAlign.Center
                            )
                            if (entry.speaker.isNotEmpty()) {
                                Text(
                                    text = entry.speaker,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(80.dp).padding(end = 8.dp),
                                    fontSize = 13.sp
                                )
                            } else {
                                Spacer(modifier = Modifier.width(80.dp))
                            }
                            val annotated = buildAnnotatedString {
                                entry.spans.forEach { span ->
                                    val color = span.color?.let {
                                        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
                                    } ?: MaterialTheme.colorScheme.onSurface
                                    pushStyle(
                                        SpanStyle(
                                            color = color,
                                            fontWeight = if (span.bold) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (span.italic) FontStyle.Italic else FontStyle.Normal
                                        )
                                    )
                                    append(span.text)
                                    pop()
                                }
                            }
                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            is LogBlock.Choice -> {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "剧情选择",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(80.dp).padding(end = 8.dp),
                        fontSize = 13.sp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        block.options.forEach { option ->
                            val isSelected = assignment[block.decisionId] == option.optionIndex
                            Text(
                                text = "• ${option.label}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            is LogBlock.Conditional -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    val label = formatConditionLabel(
                        document.conditions.describe(block.audience),
                        document.decisions
                    )
                    Text(
                        text = "分支条件: $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    block.blocks.forEach { subBlock ->
                        RenderLogBlock(subBlock, document, activeLineIndex, assignment)
                    }
                }
            }
        }
    }
}