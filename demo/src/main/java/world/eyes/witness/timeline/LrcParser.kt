package world.eyes.witness.timeline

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object LrcParser {
    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

    fun parse(inputStream: InputStream): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { sequence ->
            sequence.forEach { rawLine ->
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) return@forEach

                val matcher = TIME_TAG_PATTERN.matcher(trimmed)
                var lastMatchEnd = 0
                val timestamps = mutableListOf<Long>()

                while (matcher.find()) {
                    val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                    val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                    val fractionStr = matcher.group(3) ?: "00"
                    val fractionMs = if (fractionStr.length == 2) {
                        fractionStr.toLong() * 10L
                    } else {
                        fractionStr.toLong()
                    }
                    val totalMs = minutes * 60_000L + seconds * 1_000L + fractionMs
                    timestamps.add(totalMs)
                    lastMatchEnd = matcher.end()
                }

                val content = trimmed.substring(lastMatchEnd).trim()
                val isComment = content.startsWith("(") && content.endsWith(")")

                timestamps.forEach { ts ->
                    lines.add(
                        LrcLine(
                            timeMs = ts,
                            text = content,
                            isTagOrComment = isComment
                        )
                    )
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun findSnapshot(lines: List<LrcLine>, currentTimeSeconds: Float): TimelineSnapshot {
        if (lines.isEmpty()) {
            return TimelineSnapshot(
                currentTimeSeconds = currentTimeSeconds,
                currentLineIndex = -1,
                currentLine = null,
                nextLine = null,
                lineProgress = 0f
            )
        }

        val currentTimeMs = (currentTimeSeconds * 1000f).toLong()

        var low = 0
        var high = lines.size - 1
        var activeIndex = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= currentTimeMs) {
                activeIndex = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        if (activeIndex == -1) {
            val first = lines.first()
            return TimelineSnapshot(
                currentTimeSeconds = currentTimeSeconds,
                currentLineIndex = -1,
                currentLine = null,
                nextLine = first,
                lineProgress = 0f
            )
        }

        val current = lines[activeIndex]
        val next = lines.getOrNull(activeIndex + 1)
        val progress = if (next != null) {
            val duration = (next.timeMs - current.timeMs).coerceAtLeast(1L)
            ((currentTimeMs - current.timeMs).toFloat() / duration).coerceIn(0f, 1f)
        } else {
            1f
        }

        return TimelineSnapshot(
            currentTimeSeconds = currentTimeSeconds,
            currentLineIndex = activeIndex,
            currentLine = current,
            nextLine = next,
            lineProgress = progress
        )
    }
}