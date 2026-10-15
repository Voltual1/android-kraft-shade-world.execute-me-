package me.voltual.asp.engine.log

import me.voltual.asp.engine.ParsedLine

private data class MultilineAccumulator(
    val name: String,
    val text: String,
    val source: LogLineSource,
    val lastLineIndex: Int
)

private data class SymbolicState(
    val condition: Int,
    val selectedValue: Int,
    val references: List<Int>?,
    val multiline: MultilineAccumulator?
) {
    fun toDecisionState() = RuntimeDecisionState(selectedValue, references)
}

private data class PendingText(
    val entry: LogLineEntry,
    val audiences: MutableList<Int>
)

private fun stateKey(state: SymbolicState): String {
    val multiline = if (state.multiline == null) "-" else "${state.multiline.source}\u0001${state.multiline.lastLineIndex}\u0001${state.multiline.name}\u0001${state.multiline.text}"
    val references = state.references?.joinToString(",") ?: "-"
    return "${state.selectedValue}\u0002$references\u0002$multiline"
}

private const val MAX_STATES = 10_000

data class AnalyzeOptions(
    val maxStates: Int = MAX_STATES
)

fun analyzeStoryFlow(
    lines: List<ParsedLine>,
    variables: Map<String, Any?> = emptyMap(),
    options: AnalyzeOptions = AnalyzeOptions()
): StoryFlowResult {
    val maxStates = options.maxStates
    val conditions = ConditionStore()
    val decisions = mutableMapOf<Int, DecisionDefinition>()
    val domains = mutableMapOf<Int, List<Int>>()
    val emissions = mutableListOf<LogEmission>()

    var states = listOf(
        SymbolicState(
            condition = conditions.trueCondition(),
            selectedValue = 0,
            references = null,
            multiline = null
        )
    )

    var peakStateCount = 1
    var decisionCount = 0
    var degraded = false

    fun emitText(
        state: SymbolicState,
        entry: LogLineEntry,
        pending: MutableMap<String, PendingText>
    ) {
        val key = entryContentKey(entry)
        val existing = pending[key]
        if (existing != null) {
            existing.audiences.add(state.condition)
        } else {
            pending[key] = PendingText(entry, mutableListOf(state.condition))
        }
    }

    fun withFlushedMultiline(
        state: SymbolicState,
        pending: MutableMap<String, PendingText>
    ): SymbolicState {
        if (state.multiline == null || state.multiline.text.isEmpty()) return state
        emitText(
            state,
            buildLineEntry(
                lineIndex = state.multiline.lastLineIndex,
                speaker = state.multiline.name,
                text = state.multiline.text,
                source = state.multiline.source,
                variables = variables
            ),
            pending
        )
        return state.copy(multiline = null)
    }

    fun mergeStates(pending: MutableMap<String, PendingText>) {
        val byKey = mutableMapOf<String, SymbolicState>()
        for (state in states) {
            val key = stateKey(state)
            val existing = byKey[key]
            if (existing == null) {
                byKey[key] = state
            } else {
                val mergedCond = conditions.or(listOf(existing.condition, state.condition))
                byKey[key] = existing.copy(condition = mergedCond)
            }
        }

        states = byKey.values.map { state ->
            state.copy(condition = conditions.normalizeByDomains(state.condition, domains))
        }
        peakStateCount = maxOf(peakStateCount, states.size)

        if (states.size > maxStates) {
            degraded = true
            for (state in states) {
                withFlushedMultiline(state, pending)
            }
            states = listOf(
                SymbolicState(
                    condition = conditions.trueCondition(),
                    selectedValue = 0,
                    references = null,
                    multiline = null
                )
            )
        }
    }

    fun withAccumulationSource(
        state: SymbolicState,
        source: LogLineSource,
        pending: MutableMap<String, PendingText>
    ): SymbolicState {
        return if (state.multiline != null && state.multiline.source != source) {
            withFlushedMultiline(state, pending)
        } else {
            state
        }
    }

    fun appendLine(
        state: SymbolicState,
        lineIndex: Int,
        speaker: String,
        text: String,
        source: LogLineSource,
        pending: MutableMap<String, PendingText>
    ): SymbolicState {
        if (text.isEmpty()) return state
        val flushed = withFlushedMultiline(state, pending)
        emitText(
            flushed,
            buildLineEntry(lineIndex, speaker, text, source, variables),
            pending
        )
        return flushed
    }

    for (line in lines) {
        val lineIndex = line.lineNumber
        val pending = mutableMapOf<String, PendingText>()
        val nextStates = mutableListOf<SymbolicState>()

        for (state in states) {
            if (!passesGate(state.toDecisionState(), line)) {
                nextStates.add(state)
                continue
            }

            when (line) {
                is ParsedLine.Dialogue -> {
                    nextStates.add(
                        appendLine(
                            state = state,
                            lineIndex = lineIndex,
                            speaker = line.speaker,
                            text = line.text,
                            source = LogLineSource.DIALOGUE,
                            pending = pending
                        )
                    )
                }

                is ParsedLine.Narration -> {
                    nextStates.add(
                        appendLine(
                            state = state,
                            lineIndex = lineIndex,
                            speaker = "",
                            text = line.text,
                            source = LogLineSource.NARRATION,
                            pending = pending
                        )
                    )
                }

                is ParsedLine.Command -> {
                    when (line.command) {
                        "decision" -> {
                            val flushed = withFlushedMultiline(state, pending)
                            val parsed = parseDecision(line, variables)
                            if (parsed == null) {
                                nextStates.add(flushed.copy(references = null, selectedValue = 0))
                            } else {
                                if (!decisions.containsKey(lineIndex)) {
                                    decisions[lineIndex] = DecisionDefinition(
                                        decisionId = lineIndex,
                                        lineIndex = lineIndex,
                                        options = parsed.options
                                    )
                                    domains[lineIndex] = parsed.options.map { it.optionIndex }
                                    decisionCount += 1
                                }
                                for (option in parsed.options) {
                                    nextStates.add(
                                        SymbolicState(
                                            condition = conditions.and(
                                                flushed.condition,
                                                conditions.choice(lineIndex, option.optionIndex)
                                            ),
                                            references = null,
                                            selectedValue = option.value,
                                            multiline = flushed.multiline
                                        )
                                    )
                                }
                            }
                        }

                        "predicate" -> {
                            nextStates.add(
                                state.copy(references = parsePredicateReferences(line))
                            )
                        }

                        "multiline" -> {
                            val text = line.trailingText
                            if (text.isEmpty()) {
                                nextStates.add(state)
                            } else {
                                val current = withAccumulationSource(state, LogLineSource.MULTILINE, pending)
                                val base = current.multiline ?: MultilineAccumulator(
                                    lastLineIndex = lineIndex,
                                    name = line.args["name"]?.toString() ?: "",
                                    source = LogLineSource.MULTILINE,
                                    text = ""
                                )
                                var next = current.copy(
                                    multiline = base.copy(
                                        lastLineIndex = lineIndex,
                                        text = base.text + text
                                    )
                                )
                                if (line.args["end"] == true) {
                                    next = withFlushedMultiline(next, pending)
                                }
                                nextStates.add(next)
                            }
                        }

                        "sticker", "subtitle" -> {
                            if (line.args["hidelog"] == true) {
                                nextStates.add(state)
                            } else {
                                val text = line.args["text"]?.toString() ?: ""
                                if (text.isEmpty()) {
                                    nextStates.add(state)
                                } else {
                                    val source = if (line.command == "sticker") LogLineSource.STICKER else LogLineSource.SUBTITLE
                                    if (line.args["multi"] == true) {
                                        val current = withAccumulationSource(state, source, pending)
                                        val base = current.multiline ?: MultilineAccumulator(
                                            lastLineIndex = lineIndex,
                                            name = "",
                                            source = source,
                                            text = ""
                                        )
                                        nextStates.add(
                                            current.copy(
                                                multiline = base.copy(
                                                    lastLineIndex = lineIndex,
                                                    text = base.text + text
                                                )
                                            )
                                        )
                                    } else {
                                        nextStates.add(
                                            appendLine(state, lineIndex, "", text, source, pending)
                                        )
                                    }
                                }
                            }
                        }

                        "dialog" -> {
                            if (line.content.isNotEmpty()) {
                                nextStates.add(
                                    appendLine(
                                        state = state,
                                        lineIndex = lineIndex,
                                        speaker = line.args["name"]?.toString() ?: "",
                                        text = line.content,
                                        source = LogLineSource.DIALOGUE,
                                        pending = pending
                                    )
                                )
                            } else {
                                nextStates.add(state)
                            }
                        }

                        "aside" -> {
                            nextStates.add(
                                appendLine(
                                    state = state,
                                    lineIndex = lineIndex,
                                    speaker = "",
                                    text = line.content,
                                    source = LogLineSource.NARRATION,
                                    pending = pending
                                )
                            )
                        }

                        else -> {
                            nextStates.add(state)
                        }
                    }
                }
            }
        }

        if (line is ParsedLine.Command && line.command == "decision") {
            val parsed = parseDecision(line, variables)
            val executing = states.filter { passesGate(it.toDecisionState(), line) }
            if (parsed != null && executing.isNotEmpty()) {
                val choiceEmission = LogEmission.Choice(
                    audience = conditions.or(executing.map { it.condition }),
                    decisionId = lineIndex,
                    lineIndex = lineIndex,
                    options = parsed.options
                )
                emissions.add(choiceEmission)
            }
        }

        states = nextStates
        mergeStates(pending)

        for (pt in pending.values) {
            emissions.add(
                LogEmission.Text(
                    audience = conditions.or(pt.audiences),
                    entry = pt.entry,
                    lineIndex = pt.entry.lineIndex
                )
            )
        }
    }

    val tail = mutableMapOf<String, PendingText>()
    for (state in states) {
        withFlushedMultiline(state, tail)
    }
    for (pt in tail.values) {
        emissions.add(
            LogEmission.Text(
                audience = conditions.or(pt.audiences),
                entry = pt.entry,
                lineIndex = pt.entry.lineIndex
            )
        )
    }

    return StoryFlowResult(
        conditions = conditions,
        decisions = decisions,
        emissions = emissions,
        stats = FlowStats(
            conditionCount = conditions.size,
            decisionCount = decisionCount,
            degraded = degraded || conditions.degraded,
            emissionCount = emissions.size,
            lineCount = lines.size,
            peakStateCount = peakStateCount
        )
    )
}