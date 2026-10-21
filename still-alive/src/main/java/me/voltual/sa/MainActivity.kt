package me.voltual.sa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.jakewharton.mosaic.Mosaic
import com.jakewharton.mosaic.testing.TestTerminal

public class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MaterialTheme {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = Color.Black,
				) {
					StillAliveScreen()
				}
			}
		}
	}
}

@Composable
public fun StillAliveScreen() {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	// Initialize Media3 ExoPlayer for sa1.mp3
	val exoPlayer = remember {
		ExoPlayer.Builder(context).build().apply {
			val mediaItem = MediaItem.fromUri("asset:///music/sa1.mp3")
			setMediaItem(mediaItem)
			prepare()
		}
	}

	DisposableEffect(Unit) {
		onDispose {
			exoPlayer.release()
		}
	}

	// Initialize StillAlive Engine
	val engine = remember {
		StillAliveEngine(scope = scope) {
			exoPlayer.play()
		}
	}

	// Mosaic Canvas State rendering
	var renderedTerminalText by remember { mutableStateOf("") }

	val mosaic = remember {
		Mosaic(
			coroutineContext = scope.coroutineContext,
			onDraw = { mosaicInstance ->
				val canvas = mosaicInstance.draw()
				renderedTerminalText = canvas.render(
					com.jakewharton.mosaic.terminal.AnsiLevel.NONE,
					supportsKittyUnderlines = false,
				)
			},
			terminal = TestTerminal(),
		)
	}

	LaunchedEffect(Unit) {
		mosaic.setContent {
			StillAliveTerminalUI(engine = engine)
		}
		engine.start()
	}

	DisposableEffect(Unit) {
		onDispose {
			engine.stop()
			mosaic.cancel()
		}
	}

	// Render retro terminal output to screen with Monospace font
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black)
			.padding(16.dp),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = renderedTerminalText,
			color = Color(0xFFFFB000), // Amber Yellow
			fontFamily = FontFamily.Monospace,
			fontWeight = FontWeight.Bold,
			fontSize = 12.sp,
			lineHeight = 14.sp,
		)
	}
}