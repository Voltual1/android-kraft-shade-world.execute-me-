package me.voltual.sa

import com.jakewharton.mosaic.terminal.AnsiLevel
import com.jakewharton.mosaic.terminal.Event
import com.jakewharton.mosaic.terminal.Terminal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class VirtualTerminal : Terminal {
	override val name: String = "StillAliveVirtualTerminal"
	override val interactive: Boolean = true
	override val state: Terminal.State = State()
	override val capabilities: Terminal.Capabilities = Capabilities()
	override val events: ReceiveChannel<Event> = Channel<Event>().apply { close() }

	override fun close() {}

	internal class State : Terminal.State {
		override val focused: StateFlow<Boolean> = MutableStateFlow(true)
		override val theme: StateFlow<Terminal.Theme> = MutableStateFlow(Terminal.Theme.Dark)
		override val size: StateFlow<Terminal.Size> = MutableStateFlow(Terminal.Size(80, 24))
	}

	internal class Capabilities : Terminal.Capabilities {
		override val ansiLevel: AnsiLevel = AnsiLevel.NONE
		override val cursorVisibility: Boolean = false
		override val focusEvents: Boolean = false
		override val inBandResizeEvents: Boolean = false
		override val kittyGraphics: Boolean = false
		override val kittyKeyboard: Boolean = false
		override val kittyNotifications: Boolean = false
		override val kittyPointerShape: Boolean = false
		override val kittyTextSizingScale: Boolean = false
		override val kittyTextSizingWidth: Boolean = false
		override val kittyUnderline: Boolean = false
		override val synchronizedOutput: Boolean = false
		override val themeEvents: Boolean = false
	}
}