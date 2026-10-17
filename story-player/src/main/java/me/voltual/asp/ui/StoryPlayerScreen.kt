package me.voltual.asp.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import com.cardinalblue.kraftshade.model.GlColor
import com.cardinalblue.kraftshade.shader.buffer.LoadedTexture
import kotlinx.coroutines.launch
import me.voltual.asp.Context
import me.voltual.asp.R
import me.voltual.asp.engine.*
import me.voltual.asp.engine.audio.AndroidStoryAudio
import me.voltual.asp.engine.log.buildLogAll
import me.voltual.asp.engine.rendering.ComposeStoryRenderer
import me.voltual.asp.engine.rendering.buildColorEffectColorMatrix
import me.voltual.asp.engine.rendering.shader.SlideMaskKraftShader
import me.voltual.asp.loadContextByScript
import me.voltual.asp.ui.components.*
import kotlin.math.roundToInt

@Composable
fun StoryPlayerScreen(
    scriptText: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    val androidContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var storyContext by remember { mutableStateOf<Context?>(null) }
    var runtime by remember { mutableStateOf<StoryRuntime?>(null) }
    var renderer by remember { mutableStateOf<ComposeStoryRenderer?>(null) }
    var audio by remember { mutableStateOf<AndroidStoryAudio?>(null) }

    var isPreloading by remember { mutableStateOf(true) }
    var preloadProgress by remember { mutableStateOf(0f) }
    var preloadError by remember { mutableStateOf<String?>(null) }

    var activeLineIndex by remember { mutableStateOf<Int?>(null) }
    var showLogAll by remember { mutableStateOf(false) }

    val kraftEffectState = rememberKraftShadeEffectState()
    val slideMaskShader = remember { SlideMaskKraftShader() }

    // Cached slide_mask bitmap texture
    val slideMaskTexture = remember {
        val bmp = BitmapFactory.decodeResource(androidContext.resources, R.drawable.slide_mask)
        LoadedTexture(bmp, name = "slide_mask")
    }

    // Initialize context, preload media assets, and start story engine
    LaunchedEffect(scriptText) {
        isPreloading = true
        preloadProgress = 0f
        preloadError = null

        try {
            val ctx = loadContextByScript(scriptText)
            storyContext = ctx

            val sndAudio = AndroidStoryAudio(androidContext)
            audio = sndAudio

            val rdr = ComposeStoryRenderer(ctx, coroutineScope = coroutineScope)
            renderer = rdr

            val rt = StoryRuntime(ctx, rdr, sndAudio)
            runtime = rt

            rt.onDisplayedLineChange { line ->
                activeLineIndex = line
            }

            // Execute full asset scan and pre-cache resources (images to Coil3, audio via Ktor)
            preloadContextAssets(
                context = ctx,
                androidContext = androidContext,
                maxConcurrency = 8,
                onProgress = { progress ->
                    preloadProgress = progress
                }
            )

            isPreloading = false
            rt.start()
        } catch (e: Throwable) {
            preloadError = e.message ?: "资源加载失败"
            isPreloading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runtime?.destroy()
            audio?.destroy()
            renderer?.destroy()
        }
    }

    val curRenderer = renderer
    val curRuntime = runtime
    val curContext = storyContext

    if (isPreloading || curRenderer == null || curRuntime == null || curContext == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (preloadError != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "资源加载失败",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preloadError ?: "",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { preloadProgress.coerceIn(0f, 1f) },
                        color = Color.White,
                        trackColor = Color.DarkGray
                    )
                    val percent = (preloadProgress.coerceIn(0f, 1f) * 100f).roundToInt()
                    Text(
                        text = "正在预加载资源 $percent%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        return
    }

    val state = curRenderer.state

    val isSlideBlocker = state.blocker.visible && (state.blocker.style == "slider" || state.blocker.style == "verticalslider")

    // Update KraftShade Shader only for blocker slider/verticalslider wipe
    LaunchedEffect(isSlideBlocker) {
        if (!isSlideBlocker) return@LaunchedEffect

        kraftEffectState.setEffect { targetBuffer ->
            pipeline(targetBuffer) {
                graphSteps(targetBuffer) {
                    step(slideMaskShader, targetBuffer) { shader ->
                        val currentBlocker = state.blocker
                        shader.alpha = currentBlocker.alpha
                        shader.setVertical(currentBlocker.style == "verticalslider")
                        shader.setFlip(currentBlocker.flipX, currentBlocker.flipY)
                        shader.color = GlColor(
                            r = currentBlocker.color.r.coerceIn(0f, 1f),
                            g = currentBlocker.color.g.coerceIn(0f, 1f),
                            b = currentBlocker.color.b.coerceIn(0f, 1f),
                            a = 1f
                        )
                        shader.useTexture = false
                        shader.setSecondInputTexture(slideMaskTexture)
                    }
                }
            }
        }
        kraftEffectState.requestRender()
    }

    LaunchedEffect(state.blocker.alpha) {
        if (isSlideBlocker) {
            kraftEffectState.requestRender()
        }
    }

    // Build scene-level color effect filter
    val sceneColorFilter = remember(state.camera.grayscale, state.camera.colorInverse) {
        if (state.camera.grayscale > 0f || state.camera.colorInverse > 0f) {
            ColorFilter.colorMatrix(
                buildColorEffectColorMatrix(
                    gray = state.camera.grayscale.coerceIn(0f, 1f),
                    inverse = state.camera.colorInverse.coerceIn(0f, 1f)
                )
            )
        } else null
    }

    // Focus parameters per layer
    val bgFocusAmount = state.focus.getAmount("bg")
    val lbgFocusAmount = state.focus.getAmount("lbg")
    val charFocusAmount = state.focus.getAmount("char")
    val cgFocusAmount = state.focus.getAmount("cg")

    val bgBlurRadius = if (state.focus.blur) maxOf(bgFocusAmount, lbgFocusAmount) * 16f else 0f
    val charBlurRadius = if (state.focus.blur) charFocusAmount * 16f else 0f
    val cgBlurRadius = if (state.focus.blur) cgFocusAmount * 16f else 0f

    val bgFocusColorFilter = remember(state.focus.colorMode, bgFocusAmount, lbgFocusAmount) {
        val amt = maxOf(bgFocusAmount, lbgFocusAmount)
        if (amt > 0f && state.focus.colorMode != FocusColorMode.NONE) {
            val gray = if (state.focus.colorMode == FocusColorMode.GRAYSCALE) amt else 0f
            val inv = if (state.focus.colorMode == FocusColorMode.COLORINVERSE) amt else 0f
            ColorFilter.colorMatrix(buildColorEffectColorMatrix(gray, inv))
        } else null
    }

    val charFocusColorFilter = remember(state.focus.colorMode, charFocusAmount) {
        if (charFocusAmount > 0f && state.focus.colorMode != FocusColorMode.NONE) {
            val gray = if (state.focus.colorMode == FocusColorMode.GRAYSCALE) charFocusAmount else 0f
            val inv = if (state.focus.colorMode == FocusColorMode.COLORINVERSE) charFocusAmount else 0f
            ColorFilter.colorMatrix(buildColorEffectColorMatrix(gray, inv))
        } else null
    }

    val cgFocusColorFilter = remember(state.focus.colorMode, cgFocusAmount) {
        if (cgFocusAmount > 0f && state.focus.colorMode != FocusColorMode.NONE) {
            val gray = if (state.focus.colorMode == FocusColorMode.GRAYSCALE) cgFocusAmount else 0f
            val inv = if (state.focus.colorMode == FocusColorMode.COLORINVERSE) cgFocusAmount else 0f
            ColorFilter.colorMatrix(buildColorEffectColorMatrix(gray, inv))
        } else null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    curRuntime.advance()
                    if (curRuntime.getState() == PlayerState.FINISHED) {
                        onFinished()
                    }
                }
            }
    ) {
        // Stage Scale: calculate 16:9 1280x720 aspect ratio fitting
        val viewWidth = maxWidth
        val viewHeight = maxHeight
        val scale = minOf(viewWidth / 1280.dp, viewHeight / 720.dp)
        val stageWidth = 1280.dp * scale
        val stageHeight = 720.dp * scale

        Box(
            modifier = Modifier
                .size(stageWidth, stageHeight)
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        state.camera.shakeX.roundToInt(),
                        state.camera.shakeY.roundToInt()
                    )
                }
        ) {
            // Scene Layer Container (Subject to Camera Effects)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        sceneColorFilter?.let { filter ->
                            colorFilter = filter
                        }
                    }
            ) {
                // Layer 0: Background Texture (Aligned with PPU & ScreenAdapt)
                BackgroundOverlay(
                    background = state.background,
                    context = curContext,
                    family = StoryAssetFamily.BACKGROUND,
                    colorFilter = bgFocusColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (bgBlurRadius > 0f) Modifier.blur(bgBlurRadius.dp) else Modifier)
                )

                // Layer 0.5: Large / Grid Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (bgBlurRadius > 0f) Modifier.blur(bgBlurRadius.dp) else Modifier)
                        .graphicsLayer {
                            bgFocusColorFilter?.let { colorFilter = it }
                        }
                ) {
                    GridBackgroundOverlay(
                        gridBg = state.gridBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Layer 0.8: AVG Display Background Overlay (bgover)
                AvgDisplayOverlay(
                    slotName = "bgover",
                    avgDisplays = state.avgDisplays,
                    context = curContext,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 1: Foreground Images (Aligned with PPU, ScreenAdapt & Rotation)
                BackgroundOverlay(
                    background = state.foregroundImage,
                    context = curContext,
                    family = StoryAssetFamily.IMAGE,
                    rotationDeg = curRenderer.state.imageRotationDeg,
                    colorFilter = cgFocusColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (cgBlurRadius > 0f) Modifier.blur(cgBlurRadius.dp) else Modifier)
                )

                // Layer 1.2: AVG Display CG Overlay (cgover)
                AvgDisplayOverlay(
                    slotName = "cgover",
                    avgDisplays = state.avgDisplays,
                    context = curContext,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 1.5: Characters Slots
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (charBlurRadius > 0f) Modifier.blur(charBlurRadius.dp) else Modifier)
                        .graphicsLayer {
                            charFocusColorFilter?.let { colorFilter = it }
                        }
                ) {
                    state.characters.values.forEach { charSlot ->
                        CharacterSlotVisualView(
                            charSlot = charSlot,
                            context = curContext,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Layer 1.8: AVG Display Character Overlay (charover)
                AvgDisplayOverlay(
                    slotName = "charover",
                    avgDisplays = state.avgDisplays,
                    context = curContext,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 2: Character Cutin 特写剪影层
                CharacterCutinOverlay(
                    cutinSlots = state.cutinSlots,
                    context = curContext,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 2.5: Interlude 过场遮罩层
                InterludeOverlay(
                    interludes = state.interludes,
                    context = curContext,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Layer 3: CG Item 独立物件动画层 (不随相机后处理及晃动)
            CgItemOverlay(
                cgItems = state.cgItems,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 4: ShowItem 拍立得照片卡片与遮罩层
            ShowItemOverlay(
                showItem = state.showItem,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 5: AnimText 地区邮戳动画层
            AnimTextOverlay(
                animTexts = state.animTexts,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 6: SpellSticker 萨米/火焰咒术双语贴纸层
            SpellStickerOverlay(
                spellStickers = state.spellStickers,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 7: KraftShade Blocker Slider Wipe Effect View
            if (isSlideBlocker) {
                KraftShadeEffectView(
                    modifier = Modifier.fillMaxSize(),
                    state = kraftEffectState
                )
            }

            // Blocker standard color overlay
            if (state.blocker.visible && state.blocker.style == "default") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(
                                red = state.blocker.color.r.coerceIn(0f, 1f),
                                green = state.blocker.color.g.coerceIn(0f, 1f),
                                blue = state.blocker.color.b.coerceIn(0f, 1f),
                                alpha = state.blocker.alpha.coerceIn(0f, 1f)
                            )
                        )
                )
            }

            // Layer 8: Curtain 黑幕多边形羽化裁切层
            CurtainOverlay(
                curtains = state.curtains,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 9: Subtitles
            if (state.subtitle.visible && state.subtitle.text.isNotEmpty()) {
                Text(
                    text = state.subtitle.text,
                    color = Color.White,
                    fontSize = state.subtitle.sizePx.sp,
                    modifier = Modifier
                        .offset(state.subtitle.x.dp, state.subtitle.y.dp)
                        .width(state.subtitle.widthPx.dp)
                )
            }

            // Layer 9.5: Stickers (贴纸及 stickertween 复合动效)
            StickerOverlay(
                stickers = state.stickers,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 9.6: Timer Sticker (正向秒表贴纸)
            TimerStickerOverlay(
                timer = state.timerSticker,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 10: Dialogue Box
            DialogueBox(
                dialogue = state.dialogue,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 11: Decision Overlay
            DecisionOverlay(
                decision = state.decision,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 11.5: Video Player Overlay (全屏置顶播放)
            VideoPlayerOverlay(
                video = state.video,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 12: Fast-Control Top Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showLogAll = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Text("文本记录", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val nextMode = when (curRuntime.getAutoPlayState().mode) {
                            AutoPlayMode.DEFAULT -> AutoPlayMode.BUTTON_AUTO
                            AutoPlayMode.BUTTON_AUTO -> AutoPlayMode.QUICK_PLAY
                            AutoPlayMode.QUICK_PLAY -> AutoPlayMode.DEFAULT
                        }
                        curRuntime.setAutoPlayMode(nextMode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    val modeText = when (curRuntime.getAutoPlayState().mode) {
                        AutoPlayMode.DEFAULT -> "手动"
                        AutoPlayMode.BUTTON_AUTO -> "自动"
                        AutoPlayMode.QUICK_PLAY -> "快速"
                    }
                    Text(modeText, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = curRuntime.canSkipNode(),
                    onClick = {
                        coroutineScope.launch {
                            curRuntime.skipNode()
                            if (curRuntime.getState() == PlayerState.FINISHED) {
                                onFinished()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Text("跳过", fontSize = 12.sp)
                }
            }
        }

        // Drawer: Log All Panel
        if (showLogAll) {
            val logDoc = remember(curContext.scriptText, curContext.audioVariables) {
                val parsedLines = curContext.scriptText?.let { parseStory(it).first }
                    ?: curContext.script.mapIndexed { index, line -> parseLine(line, index + 1) }
                buildLogAll(parsedLines, curContext.audioVariables)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showLogAll = false }
            ) {
                LogAllPanel(
                    document = logDoc,
                    activeLineIndex = activeLineIndex,
                    selections = curRuntime.getLogPosition().selections,
                    onClose = { showLogAll = false },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}