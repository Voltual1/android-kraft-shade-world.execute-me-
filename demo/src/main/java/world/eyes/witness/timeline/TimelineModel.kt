package world.eyes.witness.timeline

data class LrcLine(
    val timeMs: Long,
    val text: String,
    val isTagOrComment: Boolean = false
) {
    val timeSeconds: Float = timeMs / 1000f
}

data class TimelineSnapshot(
    val currentTimeSeconds: Float,
    val currentLineIndex: Int,
    val currentLine: LrcLine?,
    val nextLine: LrcLine?,
    val lineProgress: Float
)