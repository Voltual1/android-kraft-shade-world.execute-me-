package me.voltual.sa

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.MosaicComposable
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.jakewharton.mosaic.ui.unit.IntSize

private val TerminalAmber = Color(255, 176, 0) // Classic VT241 Amber Terminal Color (#FFB000)

@Composable
@MosaicComposable
public fun StillAliveTerminalUI(
	engine: StillAliveEngine,
) {
	// Terminal dimensions: 80 cols x 24 rows
	val lyricWidth = 38
	val creditsWidth = 38
	val creditsHeight = 2
	val asciiHeight = 20

	Box {
		Column {
			// Top Frame Line: +--------------------------------------+  +--------------------------------------+
			Row {
				Text("+", color = TerminalAmber)
				Text("-".repeat(lyricWidth), color = TerminalAmber)
				Text("+", color = TerminalAmber)
				Text("  ", color = TerminalAmber)
				Text("+", color = TerminalAmber)
				Text("-".repeat(creditsWidth), color = TerminalAmber)
				Text("+", color = TerminalAmber)
			}

			// Main Content Split Area (22 lines high)
			Row {
				// Left Panel: Lyrics Area
				Column {
					val currentLines = engine.lyricLines
					val displayLines = currentLines.takeLast(22)
					for (i in 0 until 22) {
						val lineText = if (i < displayLines.size) displayLines[i] else ""
						val paddedText = lineText.take(lyricWidth).padEnd(lyricWidth, ' ')
						Row {
							Text("|", color = TerminalAmber)
							Text(paddedText, color = TerminalAmber, textStyle = TextStyle.Bold)
							Text("|", color = TerminalAmber)
						}
					}
				}

				Text("  ", color = TerminalAmber)

				// Right Panel: Credits & ASCII Art
				Column {
					// Credits Sub-panel (2 lines)
					val creditsBuffer = engine.creditsTextBuffer
					val displayCredits = creditsBuffer.takeLast(creditsHeight)
					for (i in 0 until creditsHeight) {
						val lineText = if (i < displayCredits.size) displayCredits[i] else ""
						val paddedText = lineText.take(creditsWidth).padEnd(creditsWidth, ' ')
						Row {
							Text("|", color = TerminalAmber)
							Text(paddedText, color = TerminalAmber)
							Text("|", color = TerminalAmber)
						}
					}

					// Separator between credits and ASCII art
					Row {
						Text("+", color = TerminalAmber)
						Text("-".repeat(creditsWidth), color = TerminalAmber)
						Text("+", color = TerminalAmber)
					}

					// ASCII Art Sub-panel (20 lines)
					val currentArt = StillAliveData.asciiArt.getOrElse(engine.currentAsciiIndex) {
						StillAliveData.asciiArt[0]
					}
					for (i in 0 until asciiHeight) {
						val artLine = if (i < currentArt.size) currentArt[i] else ""
						val paddedArt = artLine.take(creditsWidth).padEnd(creditsWidth, ' ')
						Row {
							Text("|", color = TerminalAmber)
							Text(paddedArt, color = TerminalAmber, textStyle = TextStyle.Bold)
							Text("|", color = TerminalAmber)
						}
					}
				}
			}

			// Bottom Frame Line: +--------------------------------------+  +--------------------------------------+
			Row {
				Text("+", color = TerminalAmber)
				Text("-".repeat(lyricWidth), color = TerminalAmber)
				Text("+", color = TerminalAmber)
				Text("  ", color = TerminalAmber)
				Text("+", color = TerminalAmber)
				Text("-".repeat(creditsWidth), color = TerminalAmber)
				Text("+", color = TerminalAmber)
			}
		}
	}
}