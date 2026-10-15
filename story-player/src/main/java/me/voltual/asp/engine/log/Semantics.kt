package me.voltual.asp.engine.log

import me.voltual.asp.engine.ParsedLine
import me.voltual.asp.engine.expandStoryText

data class RuntimeDecisionState(
    val selectedValue: Int,
    val references: List<Int>?
)

fun initialRuntimeDecisionState(): RuntimeDecisionState =
    RuntimeDecisionState(selectedValue = 0, references = null)

private fun toIntList(value: Any?): List<Int> {
    if (value == null) return emptyList()
    return value.toString().split(";").map { segment ->
        segment.trim().toIntOrNull() ?: 0
    }
}

fun passesGate(
    state: RuntimeDecisionState,
    line: ParsedLine
): Boolean {
    if (line is ParsedLine.Command && line.command == "predicate") return true
    if (state.selectedValue == 0) return true
    if (state.references == null || state.references.isEmpty()) return true
    return state.references.contains(state.selectedValue)
}

data class ParsedDecision(
    val options: List<ChoiceOption>
)

fun parseDecision(
    line: ParsedLine.Command,
    variables: Map<String, Any?> = emptyMap()
): ParsedDecision? {
    val optionsValue = line.args["options"] ?: return null
    val labels = optionsValue.toString().split(";")
    val values = toIntList(line.args["values"])
    val options = labels.mapIndexed { optionIndex, label ->
        ChoiceOption(
            label = expandStoryText(label, variables),
            optionIndex = optionIndex,
            value = values.getOrNull(optionIndex) ?: 0
        )
    }
    return ParsedDecision(options)
}

fun parsePredicateReferences(line: ParsedLine.Command): List<Int>? {
    val refVal = line.args["references"] ?: return null
    return toIntList(refVal)
}