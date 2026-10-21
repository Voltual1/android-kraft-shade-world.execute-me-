package me.voltual.sa

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.jakewharton.mosaic.Mosaic

@Composable
public fun StillAliveScreen(
	modifier: Modifier = Modifier,
	@RawRes rawAudioResId: Int = R.raw.mili_still_alive,
	@DrawableRes cakeDrawableResId: Int = R.drawable.cake,
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

	// Root Box
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(Color.Black),
		contentAlignment = Alignment.Center,
	) {
		// 横屏自适应计算：80 列 x 24 行终端
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.padding(8.dp),
			contentAlignment = Alignment.Center,
		) {
			val columns = 80f
			val rows = 24f
			val charAspectRatio = 0.60f
			val lineSpacingRatio = 1.15f

			val maxFontSizeFromWidth = (maxWidth.value / (columns * charAspectRatio))
			val maxFontSizeFromHeight = (maxHeight.value / (rows * lineSpacingRatio))

			val dynamicFontSize = minOf(maxFontSizeFromWidth, maxFontSizeFromHeight).coerceAtLeast(6f)
			val dynamicLineHeight = dynamicFontSize * lineSpacingRatio

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

		// 播放完成后淡入展示 Coil 加载的蛋糕图片
		AnimatedVisibility(
			visible = engine.isFinished,
			enter = fadeIn(animationSpec = tween(durationMillis = 2000)),
			exit = fadeOut(animationSpec = tween(durationMillis = 500)),
			modifier = Modifier.fillMaxSize(),
		) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Color.Black),
				contentAlignment = Alignment.Center,
			) {
				AsyncImage(
					model = cakeDrawableResId,
					contentDescription = "The Cake",
					contentScale = ContentScale.Fit,
					modifier = Modifier.fillMaxSize(),
				)
			}
		}
	}
}
