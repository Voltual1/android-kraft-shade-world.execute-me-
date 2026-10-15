package me.voltual.asp.engine.log

typealias DecisionId = Int
typealias OptionIndex = Int

data class ChoiceOption(
    val label: String,
    val optionIndex: OptionIndex,
    val value: Int
)

data class DecisionDefinition(
    val decisionId: DecisionId,
    val lineIndex: Int,
    val options: List<ChoiceOption>
)

data class LogTextSpan(
    val text: String,
    val color: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false
)

enum class LogLineSource {
    DIALOGUE,
    MULTILINE,
    NARRATION,
    STICKER,
    SUBTITLE
}

data class LogLineEntry(
    val lineIndex: Int,
    val speaker: String,
    val spans: List<LogTextSpan>,
    val source: LogLineSource
)

sealed interface LogEmission {
    val lineIndex: Int
    val audience: Int

    data class Choice(
        override val lineIndex: Int,
        override val audience: Int,
        val decisionId: DecisionId,
        val options: List<ChoiceOption>
    ) : LogEmission

    data class Text(
        override val lineIndex: Int,
        override val audience: Int,
        val entry: LogLineEntry
    ) : LogEmission
}

data class FlowStats(
    val lineCount: Int,
    val decisionCount: Int,
    val peakStateCount: Int,
    val conditionCount: Int,
    val emissionCount: Int,
    val degraded: Boolean
)

data class StoryFlowResult(
    val emissions: List<LogEmission>,
    val conditions: ConditionStore,
    val decisions: Map<DecisionId, DecisionDefinition>,
    val stats: FlowStats
)

sealed interface LogBlock {
    val audience: Int

    data class Choice(
        val decisionId: DecisionId,
        val lineIndex: Int,
        override val audience: Int,
        val options: List<ChoiceOption>,
        var inert: Boolean
    ) : LogBlock

    data class Lines(
        override val audience: Int,
        val entries: MutableList<LogLineEntry>
    ) : LogBlock

    data class Conditional(
        override val audience: Int,
        val blocks: List<LogBlock>
    ) : LogBlock
}

data class LogDocument(
    val blocks: List<LogBlock>,
    val decisions: Map<DecisionId, DecisionDefinition>,
    val conditions: ConditionStore,
    val degraded: Boolean
)