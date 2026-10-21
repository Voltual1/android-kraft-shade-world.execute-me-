package me.voltual.sa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

public class StillAliveEngine(
	private val scope: CoroutineScope,
	private val onStartMusic: () -> Unit,
) {
	public var currentAsciiIndex: Int by mutableIntStateOf(0)
		private set

	public val lyricLines: MutableList<String> = mutableStateListOf("")

	public var creditsTextBuffer: List<String> by mutableStateOf(emptyList())
		private set

	private var creditsStarted = false
	private var timelineJob: Job? = null
	private var creditsJob: Job? = null

	public fun start() {
		stop()
		lyricLines.clear()
		lyricLines.add("")
		currentAsciiIndex = 0
		creditsStarted = false
		creditsTextBuffer = emptyList()

		timelineJob = scope.launch {
			val lyrics = StillAliveData.rawLyrics
			var currentLyricIndex = 0
			val startTime = System.currentTimeMillis()

			while (isActive && currentLyricIndex < lyrics.size) {
				val item = lyrics[currentLyricIndex]
				val elapsed = System.currentTimeMillis() - startTime

				if (elapsed >= item.timeMs) {
					processLyricItem(item, lyrics, currentLyricIndex)
					currentLyricIndex++
				}
				delay(10)
			}
		}
	}

	private suspend fun processLyricItem(
		item: LyricItem,
		lyrics: List<LyricItem>,
		currentIndex: Int,
	) {
		when (item.mode) {
			0, 1 -> { // Typewriter text
				val text = item.words as String
				val wordCount = if (text.isEmpty()) 1 else text.length
				val intervalMs = if (item.intervalSec < 0) {
					if (currentIndex + 1 < lyrics.size) {
						((lyrics[currentIndex + 1].timeMs - item.timeMs) / wordCount.toDouble())
					} else {
						50.0
					}
				} else {
					(item.intervalSec * 1000.0) / wordCount.toDouble()
				}

				for (ch in text) {
					if (lyricLines.isEmpty()) {
						lyricLines.add("")
					}
					val lastIdx = lyricLines.size - 1
					lyricLines[lastIdx] = lyricLines[lastIdx] + ch
					delay(intervalMs.toLong().coerceAtLeast(1))
				}

				if (item.mode == 0) {
					lyricLines.add("")
				}
			}
			2 -> { // Display ASCII Art
				val artIdx = item.words as Int
				if (artIdx in StillAliveData.asciiArt.indices) {
					currentAsciiIndex = artIdx
				}
			}
			3 -> { // Clear lyrics
				lyricLines.clear()
				lyricLines.add("")
			}
			4 -> { // Start Music
				onStartMusic()
			}
			5 -> { // Start Credits
				if (!creditsStarted) {
					creditsStarted = true
					startCreditsScroll()
				}
			}
			9 -> { // End
				// Sequence complete
			}
		}
	}

	private fun startCreditsScroll() {
		creditsJob = scope.launch {
			val fullCredits = StillAliveData.CREDITS_TEXT
			val length = fullCredits.length
			val totalDurationMs = 174000L // 174 seconds
			val startTime = System.currentTimeMillis()

			val creditsHeight = 22 // Height allocated in 80x24 layout
			val lines = mutableListOf("")

			var i = 0
			while (isActive && i < length) {
				val ch = fullCredits[i]
				val targetTime = startTime + (totalDurationMs.toDouble() / length * i).toLong()

				if (ch == '\n') {
					lines.add("")
					if (lines.size > creditsHeight) {
						lines.removeAt(0)
					}
				} else {
					val lastIdx = lines.size - 1
					lines[lastIdx] = lines[lastIdx] + ch
				}

				creditsTextBuffer = ArrayList(lines)
				i++

				val now = System.currentTimeMillis()
				if (now < targetTime) {
					delay(targetTime - now)
				}
			}
		}
	}

	public fun stop() {
		timelineJob?.cancel()
		creditsJob?.cancel()
		timelineJob = null
		creditsJob = null
	}
}