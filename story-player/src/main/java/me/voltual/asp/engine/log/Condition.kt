package me.voltual.asp.engine.log

typealias Product = Map<DecisionId, OptionIndex>

const val TRUE_CONDITION: Int = 0
private const val MAX_PRODUCTS = 256
private const val MAX_MERGE_PRODUCTS = 32

sealed interface DisplayCondition {
    data object Always : DisplayCondition
    data class AnyCondition(val alternatives: List<DisplayAlternative>) : DisplayCondition
}

data class DisplayChoice(
    val decisionId: DecisionId,
    val optionIndexes: List<OptionIndex>
)

data class DisplayAlternative(
    val choices: List<DisplayChoice>
)

class ConditionStore {
    private val conditions: MutableList<List<Product>> = mutableListOf(listOf(emptyMap()))
    private val cache: MutableMap<String, Int> = mutableMapOf("" to TRUE_CONDITION)
    var degraded: Boolean = false
        private set

    val size: Int get() = conditions.size

    fun trueCondition(): Int = TRUE_CONDITION

    fun choice(decisionId: DecisionId, optionIndex: OptionIndex): Int =
        intern(listOf(mapOf(decisionId to optionIndex)))

    fun and(left: Int, right: Int): Int {
        if (left == TRUE_CONDITION) return right
        if (right == TRUE_CONDITION) return left
        if (left == right) return left
        return intern(crossJoin(products(left), products(right)))
    }

    fun or(operands: List<Int>): Int {
        val merged = mutableListOf<Product>()
        for (id in operands) {
            if (id == TRUE_CONDITION) return TRUE_CONDITION
            merged.addAll(products(id))
        }
        return intern(merged)
    }

    fun referencedDecisions(id: Int): Set<DecisionId> {
        val result = mutableSetOf<DecisionId>()
        for (product in products(id)) {
            result.addAll(product.keys)
        }
        return result
    }

    fun evaluate(id: Int, assignment: Map<DecisionId, OptionIndex>): Boolean {
        return products(id).any { product ->
            product.all { (decisionId, optionIndex) ->
                assignment[decisionId] == optionIndex
            }
        }
    }

    fun evaluatePartial(id: Int, assignment: Map<DecisionId, OptionIndex>): Boolean? {
        var unknown = false
        for (product in products(id)) {
            var productUnknown = false
            var contradicted = false
            for ((decisionId, optionIndex) in product) {
                val chosen = assignment[decisionId]
                if (chosen == null) {
                    productUnknown = true
                } else if (chosen != optionIndex) {
                    contradicted = true
                    break
                }
            }
            if (contradicted) continue
            if (!productUnknown) return true
            unknown = true
        }
        return if (unknown) null else false
    }

    fun satisfyingAssignment(
        id: Int,
        domains: Map<DecisionId, List<OptionIndex>>
    ): MutableMap<DecisionId, OptionIndex>? {
        var remaining = products(id)
        if (remaining.isEmpty()) return null

        val decisionIds = remaining.flatMap { it.keys }.distinct().sorted()
        val assignment = mutableMapOf<DecisionId, OptionIndex>()

        for (decisionId in decisionIds) {
            val domain = (domains[decisionId] ?: emptyList()).sorted()
            val candidates = if (domain.isNotEmpty()) {
                domain
            } else {
                remaining.mapNotNull { it[decisionId] }.distinct().sorted()
            }
            var chosen: OptionIndex? = null
            for (optionIndex in candidates) {
                val next = remaining.filter { !it.containsKey(decisionId) || it[decisionId] == optionIndex }
                if (next.isNotEmpty()) {
                    chosen = optionIndex
                    remaining = next
                    break
                }
            }
            if (chosen == null) return null
            assignment[decisionId] = chosen
        }
        return assignment
    }

    fun isTautology(
        id: Int,
        reachableOptions: Map<DecisionId, List<OptionIndex>>
    ): Boolean {
        if (id == TRUE_CONDITION) return true
        val decisions = referencedDecisions(id).toList()
        val domains = decisions.map { reachableOptions[it] ?: emptyList() }

        fun consistent(product: Product, decisionId: DecisionId, optionIndex: OptionIndex?): Boolean =
            if (optionIndex == null) !product.containsKey(decisionId)
            else !product.containsKey(decisionId) || product[decisionId] == optionIndex

        fun dfs(index: Int, remaining: List<Product>): Boolean {
            if (remaining.isEmpty()) return false
            if (index == decisions.size) return true
            val decisionId = decisions[index]
            val domain = domains[index]
            val options: List<OptionIndex?> = if (domain.isNotEmpty()) domain else listOf(null)
            return options.all { optionIndex ->
                dfs(index + 1, remaining.filter { consistent(it, decisionId, optionIndex) })
            }
        }

        return dfs(0, products(id))
    }

    fun normalizeByDomains(
        id: Int,
        domains: Map<DecisionId, List<OptionIndex>>
    ): Int {
        var current: List<MutableMap<DecisionId, OptionIndex>> = products(id).map { it.toMutableMap() }
        var projected = false
        var changed = true

        while (changed) {
            changed = false
            val allDecisions = current.flatMap { it.keys }.distinct()
            for (decisionId in allDecisions) {
                val domain = domains[decisionId]
                if (domain.isNullOrEmpty()) continue
                if (current.any { !it.containsKey(decisionId) }) continue

                val groups = mutableMapOf<String, MutableSet<OptionIndex>>()
                for (product in current) {
                    val rest = product.toMutableMap()
                    val own = rest.remove(decisionId)!!
                    val key = productKey(rest)
                    groups.getOrPut(key) { mutableSetOf() }.add(own)
                }

                val full = domain.toSet()
                val coverable = groups.values.all { covered ->
                    covered.size == full.size && covered.containsAll(full)
                }
                if (!coverable) continue

                current = current.map { product ->
                    val rest = product.toMutableMap()
                    rest.remove(decisionId)
                    rest
                }
                projected = true
                changed = true
            }
        }

        return if (projected) intern(current) else id
    }

    fun describe(id: Int): DisplayCondition {
        if (id == TRUE_CONDITION) return DisplayCondition.Always
        return DisplayCondition.AnyCondition(mergeProducts(products(id)))
    }

    private fun products(id: Int): List<Product> = conditions[id]

    private fun intern(products: List<Product>): Int {
        val canonical = canonicalize(products) ?: run {
            degraded = true
            return TRUE_CONDITION
        }
        val key = conditionKey(canonical)
        cache[key]?.let { return it }
        val id = conditions.size
        conditions.add(canonical)
        cache[key] = id
        return id
    }
}

private fun canonicalize(products: List<Product>): List<Product>? {
    val unique = mutableMapOf<String, Product>()
    for (product in products) {
        if (product.isEmpty()) return listOf(emptyMap())
        unique[productKey(product)] = product
    }

    val kept = mutableListOf<Product>()
    val sortedCandidates = unique.values.sortedBy { it.size }
    for (candidate in sortedCandidates) {
        if (kept.none { weaker -> covers(candidate, weaker) }) {
            kept.add(candidate)
        }
    }

    if (kept.size > MAX_PRODUCTS) return null
    return kept.sortedBy { productKey(it) }
}

private fun covers(product: Product, weaker: Product): Boolean {
    if (weaker.size > product.size) return false
    for ((decisionId, optionIndex) in weaker) {
        if (product[decisionId] != optionIndex) return false
    }
    return true
}

private fun crossJoin(left: List<Product>, right: List<Product>): List<Product> {
    val output = mutableListOf<Product>()
    for (l in left) {
        for (r in right) {
            val product = l.toMutableMap()
            var contradiction = false
            for ((decisionId, optionIndex) in r) {
                val existing = product[decisionId]
                if (existing != null && existing != optionIndex) {
                    contradiction = true
                    break
                }
                product[decisionId] = optionIndex
            }
            if (!contradiction) output.add(product)
        }
    }
    return output
}

private fun productKey(product: Product): String =
    product.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }

private fun conditionKey(products: List<Product>): String =
    if (products.isEmpty()) "\u0000" else products.joinToString("|") { productKey(it) }

private class MutableChoice(
    val decisionId: DecisionId,
    val optionIndexes: MutableList<OptionIndex>
)

private fun mergeProducts(products: List<Product>): List<DisplayAlternative> {
    var alternatives: List<List<MutableChoice>> = products.map { product ->
        product.entries.sortedBy { it.key }.map { (decisionId, optionIndex) ->
            MutableChoice(decisionId, mutableListOf(optionIndex))
        }
    }

    var changed = alternatives.size <= MAX_MERGE_PRODUCTS
    while (changed) {
        changed = false
        outer@ for (i in alternatives.indices) {
            for (j in i + 1 until alternatives.size) {
                val merged = tryMerge(alternatives[i], alternatives[j])
                if (merged != null) {
                    alternatives = alternatives.filterIndexed { index, _ -> index != i && index != j } + listOf(merged)
                    changed = true
                    break@outer
                }
            }
        }
    }

    return alternatives.map { choices ->
        DisplayAlternative(
            choices.map { choice ->
                DisplayChoice(choice.decisionId, choice.optionIndexes.sorted())
            }
        )
    }
}

private fun tryMerge(left: List<MutableChoice>, right: List<MutableChoice>): List<MutableChoice>? {
    if (left.size != right.size) return null
    var differing: Pair<DecisionId, List<OptionIndex>>? = null

    for (i in left.indices) {
        val a = left[i]
        val b = right[i]
        if (a.decisionId != b.decisionId) return null
        if (a.optionIndexes.toSet() == b.optionIndexes.toSet()) continue
        if (differing != null) return null
        differing = a.decisionId to (a.optionIndexes + b.optionIndexes).distinct()
    }
    if (differing == null) return null
    val (diffId, diffOptions) = differing
    return left.map { choice ->
        if (choice.decisionId == diffId) {
            MutableChoice(choice.decisionId, diffOptions.toMutableList())
        } else {
            choice
        }
    }
}