package me.voltual.asp.engine.log

import me.voltual.asp.engine.ParsedLine

/**
 * Log All 分析入口。
 *
 * 数据流：单行 runtime 语义（semantics）→ 符号状态 DAG（symbolicFlow）→
 * 带路径条件的 emissions → 顺序化 LogDocument（document）→ 时间线 UI。
 * DAG 负责正确性，Document 负责阅读体验。
 */
fun buildLogAll(
    lines: List<ParsedLine>,
    variables: Map<String, Any?> = emptyMap()
): LogDocument {
    return buildLogDocument(analyzeStoryFlow(lines, variables))
}