package me.voltual.sa

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.jakewharton.mosaic.Mosaic

@Composable
public fun StillAliveScreen(
	modifier: Modifier = Modifier,
	@RawRes rawAudioResId: Int = R.raw.R.raw.mili_still_alive,
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	// Initialize Media3 ExoPlayer for raw resource audio
	val exoPlayer = remember(rawAudioResId) {
		ExoPlayer.Builder(context).build().apply {
			val uri = RawResourceDataSource.buildRawResourceUri(rawAudioResId)
			val mediaItem = MediaItem.fromUri(uri)
			setMediaItem(mediaItem)
			prepare()
		}
	}

	DisposableEffect(exoPlayer) {
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
			terminal = VirtualTerminal(),
		)
	}

	LaunchedEffect(engine) {
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

	// 横屏自适应计算：80 列 x 24 行终端
	BoxWithConstraints(
		modifier = modifier
			.fillMaxSize()
			.background(Color.Black)
			.padding(8.dp),
		contentAlignment = Alignment.Center,
	) {
		// 终端字符维度: 80 列 x 24 行
		val columns = 80f
		val rows = 24f

		// 等宽英文字符宽高比通常约为 0.60
		val charAspectRatio = 0.60f

		// 保证 80 列塞进 maxWidth 的最大字体大小 (fontSize ≈ width / (columns * 0.6))
		val maxFontSizeFromWidth = (maxWidth.value / (columns * charAspectRatio))

		// 保证 24 行塞进 maxHeight 的最大字体大小 (行高通常为 1.15 * fontSize)
		val lineSpacingRatio = 1.15f
		val maxFontSizeFromHeight = (maxHeight.value / (rows * lineSpacingRatio))

		// 选取两者的最小值，避免横向被切或纵向溢出
		val dynamicFontSize = minOf(maxFontSizeFromWidth, maxFontSizeFromHeight).coerceAtLeast(6f)
		val dynamicLineHeight = dynamicFontSize * lineSpacingRatio

		Box(
			contentAlignment = Alignment.Center,
		) {
			Text(
				text = renderedTerminalText,
				style = ComposeTextStyle(
					color = Color(0xFFFFB000), // VT241 经典琥珀黄
					fontFamily = FontFamily.Monospace,
					fontWeight = FontWeight.Bold,
					fontSize = dynamicFontSize.sp,
					lineHeight = dynamicLineHeight.sp,
				),
				softWrap = false,
			)
		}
	}
}