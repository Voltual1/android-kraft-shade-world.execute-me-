package me.voltual.asp.engine.log

fun buildLogDocument(flow: StoryFlowResult): LogDocument {
    val conditions = flow.conditions
    val decisions = flow.decisions
    val emissions = flow.emissions

    val domains = mutableMapOf<DecisionId, List<OptionIndex>>()
    for (decision in decisions.values) {
        domains[decision.decisionId] = decision.options.map { it.optionIndex }
    }

    val normalizeCache = mutableMapOf<Int, Int>()
    fun normalizeAudience(audience: Int): Int {
        normalizeCache[audience]?.let { return it }
        val simplified = conditions.normalizeByDomains(audience, domains)
        val normalized = if (conditions.isTautology(simplified, domains)) TRUE_CONDITION else simplified
        normalizeCache[audience] = normalized
        return normalized
    }

    val sorted = emissions.sortedBy { it.lineIndex }

    sealed class OrderedItem {
        data class ChoiceItem(val emission: LogEmission.Choice) : OrderedItem()
        data class TextItem(val key: String) : OrderedItem()
    }

    val textItems = mutableMapOf<String, Pair<MutableList<Int>, LogLineEntry>>()
    val ordered = mutableListOf<OrderedItem>()

    for (emission in sorted) {
        when (emission) {
            is LogEmission.Choice -> ordered.add(OrderedItem.ChoiceItem(emission))
            is LogEmission.Text -> {
                val key = entryContentKey(emission.entry)
                val existing = textItems[key]
                if (existing != null) {
                    existing.first.add(emission.audience)
                } else {
                    textItems[key] = Pair(mutableListOf(emission.audience), emission.entry)
                    ordered.add(OrderedItem.TextItem(key))
                }
            }
        }
    }

    val flat = mutableListOf<LogBlock>()
    var currentLines: LogBlock.Lines? = null

    fun closeLineBlock() {
        if (currentLines != null && currentLines!!.entries.isNotEmpty()) {
            flat.add(currentLines!!)
        }
        currentLines = null
    }

    for (item in ordered) {
        when (item) {
            is OrderedItem.ChoiceItem -> {
                closeLineBlock()
                flat.add(
                    LogBlock.Choice(
                        decisionId = item.emission.decisionId,
                        lineIndex = item.emission.lineIndex,
                        audience = normalizeAudience(item.emission.audience),
                        options = item.emission.options,
                        inert = false
                    )
                )
            }
            is OrderedItem.TextItem -> {
                val (audienceIds, entry) = textItems[item.key]!!
                val audience = normalizeAudience(conditions.or(audienceIds))
                if (currentLines != null && currentLines!!.audience == audience) {
                    currentLines!!.entries.add(entry)
                } else {
                    closeLineBlock()
                    currentLines = LogBlock.Lines(audience = audience, entries = mutableListOf(entry))
                }
            }
        }
    }
    closeLineBlock()

    val branching = mutableSetOf<DecisionId>()
    for (block in flat) {
        branching.addAll(conditions.referencedDecisions(block.audience))
    }
    for (block in flat) {
        if (block is LogBlock.Choice) {
            block.inert = block.options.size <= 1 || !branching.contains(block.decisionId)
        }
    }

    return LogDocument(
        blocks = wrapConditionalRuns(flat),
        conditions = conditions,
        decisions = decisions,
        degraded = flow.stats.degraded
    )
}

private fun wrapConditionalRuns(flat: List<LogBlock>): List<LogBlock> {
    val output = mutableListOf<LogBlock>()
    var runAudience: Int? = null
    val runBlocks = mutableListOf<LogBlock>()

    fun flush() {
        if (runAudience != null) {
            output.add(LogBlock.Conditional(audience = runAudience!!, blocks = runBlocks.toList()))
            runAudience = null
            runBlocks.clear()
        }
    }

    for (block in flat) {
        if (block.audience == TRUE_CONDITION) {
            flush()
            output.add(block)
            continue
        }
        if (runAudience != null && runAudience == block.audience) {
            runBlocks.add(block)
            continue
        }
        flush()
        runAudience = block.audience
        runBlocks.add(block)
    }
    flush()
    return output
}

fun flattenBlocks(blocks: List<LogBlock>): List<Pair<Int, LogLineEntry>> {
    val output = mutableListOf<Pair<Int, LogLineEntry>>()
    for (block in blocks) {
        when (block) {
            is LogBlock.Lines -> {
                for (entry in block.entries) {
                    output.add(Pair(block.audience, entry))
                }
            }
            is LogBlock.Conditional -> output.addAll(flattenBlocks(block.blocks))
            is LogBlock.Choice -> {}
        }
    }
    return output
}

fun projectVisibleEntries(
    document: LogDocument,
    assignment: Map<DecisionId, OptionIndex>
): List<LogLineEntry> {
    return flattenBlocks(document.blocks)
        .filter { (audience, _) -> document.conditions.evaluate(audience, assignment) }
        .map { it.second }
}

fun collectAllEntries(document: LogDocument): List<LogLineEntry> =
    flattenBlocks(document.blocks).map { it.second }

fun formatConditionLabel(
    display: DisplayCondition,
    decisions: Map<DecisionId, DecisionDefinition>
): String {
    if (display is DisplayCondition.Always) return "全部路线"
    val anyCond = display as DisplayCondition.AnyCondition

    fun optionLabel(decisionId: DecisionId, optionIndex: OptionIndex): String =
        decisions[decisionId]?.options?.find { it.optionIndex == optionIndex }?.label ?: "#${optionIndex + 1}"

    fun formatAlternative(choices: List<DisplayChoice>): String {
        if (choices.isEmpty()) return "全部路线"
        val parts = choices.map { choice ->
            choice.optionIndexes.joinToString(" / ") { optionLabel(choice.decisionId, it) }
        }
        return "选择「" + parts.joinToString("」，随后选择「") + "」"
    }

    if (anyCond.alternatives.isEmpty() || anyCond.alternatives.size > 4) {
        return "部分选择路线"
    }
    return anyCond.alternatives.joinToString("，或") { formatAlternative(it.choices) }
}