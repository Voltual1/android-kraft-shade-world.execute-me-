package me.voltual.asp.engine.log

data class SeekPlan(
    val choices: Map<DecisionId, OptionIndex>,
    val degraded: Boolean
)

sealed interface SeekPlanResult {
    data class Ok(val plan: SeekPlan) : SeekPlanResult
    data class NotFound(val reason: String = "not_found") : SeekPlanResult
}

fun planChoicesForLine(
    flow: StoryFlowResult,
    targetLineIndex: Int
): SeekPlanResult {
    val audiences = flow.emissions
        .filter { it is LogEmission.Text && it.lineIndex == targetLineIndex }
        .map { it.audience }

    if (audiences.isEmpty()) return SeekPlanResult.NotFound()

    val domains = mutableMapOf<DecisionId, List<OptionIndex>>()
    for ((decisionId, definition) in flow.decisions) {
        domains[decisionId] = definition.options.map { it.optionIndex }
    }

    for (audience in audiences) {
        val assignment = flow.conditions.satisfyingAssignment(audience, domains)
        if (assignment != null) {
            val keysToRemove = assignment.filterValues { it == 0 }.keys
            keysToRemove.forEach { assignment.remove(it) }
            return SeekPlanResult.Ok(
                SeekPlan(choices = assignment, degraded = flow.stats.degraded)
            )
        }
    }

    return SeekPlanResult.NotFound()
}