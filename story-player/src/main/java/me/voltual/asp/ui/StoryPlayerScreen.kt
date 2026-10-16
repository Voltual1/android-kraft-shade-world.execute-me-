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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cardinalblue.kraftshade.compose.KraftShadeEffectView
import com.cardinalblue.kraftshade.compose.rememberKraftShadeEffectState
import com.cardinalblue.kraftshade.shader.buffer.LoadedTexture
import kotlinx.coroutines.launch
import me.voltual.asp.Context
import me.voltual.asp.R
import me.voltual.asp.engine.*
import me.voltual.asp.engine.audio.AndroidStoryAudio
import me.voltual.asp.engine.log.buildLogAll
import me.voltual.asp.engine.rendering.ComposeStoryRenderer
import me.voltual.asp.engine.rendering.shader.ColorEffectKraftShader
import me.voltual.asp.engine.rendering.shader.FocusEffectKraftShader
import me.voltual.asp.engine.rendering.shader.SlideMaskKraftShader
import me.voltual.asp.loadContextByScript
import me.voltual.asp.ui.components.CgItemOverlay
import me.voltual.asp.ui.components.CharacterCutinOverlay
import me.voltual.asp.ui.components.CurtainOverlay
import me.voltual.asp.ui.components.DecisionOverlay
import me.voltual.asp.ui.components.DialogueBox
import me.voltual.asp.ui.components.LogAllPanel
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
    var activeLineIndex by remember { mutableStateOf<Int?>(null) }
    var showLogAll by remember { mutableStateOf(false) }

    val kraftEffectState = rememberKraftShadeEffectState()
    val slideMaskShader = remember { SlideMaskKraftShader() }
    val colorEffectShader = remember { ColorEffectKraftShader() }
    val focusEffectShader = remember { FocusEffectKraftShader() }

    // Cached slide_mask bitmap texture
    val slideMaskTexture = remember {
        val bmp = BitmapFactory.decodeResource(androidContext.resources, R.drawable.slide_mask)
        LoadedTexture(bmp, name = "slide_mask")
    }

    // Initialize context and story engines
    LaunchedEffect(scriptText) {
        isPreloading = true
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

        isPreloading = false
        rt.start()
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
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val state = curRenderer.state

    // Update KraftShade Shader parameters whenever renderer camera, blocker or focus updates
    LaunchedEffect(state.camera, state.blocker, state.focus) {
        kraftEffectState.setEffect { targetBuffer ->
            pipeline(targetBuffer) {
                graphSteps(targetBuffer) {
                    val isSlideBlocker = state.blocker.visible && (state.blocker.style == "slider" || state.blocker.style == "verticalslider")
                    val hasFocus = state.focus.amount > 0f
                    val hasColor = state.camera.grayscale > 0f || state.camera.colorInverse > 0f

                    if (isSlideBlocker) {
                        slideMaskShader.alpha = state.blocker.alpha
                        slideMaskShader.setVertical(state.blocker.style == "verticalslider")
                        slideMaskShader.setFlip(state.blocker.flipX, state.blocker.flipY)
                        slideMaskShader.setSecondInputTexture(slideMaskTexture)
                        step(slideMaskShader, targetBuffer)
                    } else if (hasFocus) {
                        val (passBuffer) = createBufferReferences("focusPass")
                        focusEffectShader.blurAmount = if (state.focus.blur) state.focus.amount else 0f
                        focusEffectShader.grayAmount = if (state.focus.colorMode == FocusColorMode.GRAYSCALE) state.focus.amount else 0f
                        focusEffectShader.inverseAmount = if (state.focus.colorMode == FocusColorMode.COLORINVERSE) state.focus.amount else 0f

                        if (hasColor) {
                            step(focusEffectShader, passBuffer)
                            colorEffectShader.grayAmount = state.camera.grayscale
                            colorEffectShader.inverseAmount = state.camera.colorInverse
                            step(colorEffectShader, targetBuffer)
                        } else {
                            step(focusEffectShader, targetBuffer)
                        }
                    } else if (hasColor) {
                        colorEffectShader.grayAmount = state.camera.grayscale
                        colorEffectShader.inverseAmount = state.camera.colorInverse
                        step(colorEffectShader, targetBuffer)
                    }
                }
            }
        }
        kraftEffectState.requestRender()
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
            // Layer 0: Background Texture
            state.background?.let { bg ->
                val bgUrl = bg.textureKey?.let { resolveStoryAssetByKey(it, StoryAssetFamily.BACKGROUND) }
                if (bgUrl != null) {
                    AsyncImage(
                        model = bgUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(bg.alpha)
                            .scale(bg.scaleX, bg.scaleY)
                            .offset(bg.x.dp, bg.y.dp)
                    )
                }
            }

            // Foreground Images
            state.foregroundImage?.let { img ->
                val imgUrl = img.textureKey?.let { resolveStoryAssetByKey(it, StoryAssetFamily.IMAGE) }
                if (imgUrl != null) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(img.alpha)
                            .rotate(curRenderer.state.imageRotationDeg)
                            .scale(img.scaleX, img.scaleY)
                            .offset(img.x.dp, img.y.dp)
                    )
                }
            }

            // Layer 1: Characters Slots (Left, Middle, Right)
            state.characters.values.forEach { charSlot ->
                val charKey = charSlot.characterKey
                if (charKey != null) {
                    val charUrl = resolveStoryCharacterAssetByKey(charKey)
                    if (charUrl != null) {
                        AsyncImage(
                            model = charUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(charSlot.alpha)
                                .scale(charSlot.scaleX, charSlot.scaleY)
                                .offset(charSlot.x.dp, charSlot.y.dp)
                        )
                    }
                }
            }

            // Layer 2: Character Cutin 特写剪影层
            CharacterCutinOverlay(
                cutinSlots = state.cutinSlots,
                context = curContext,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 3: CG Item 独立物件动画层 (不随相机晃动)
            CgItemOverlay(
                cgItems = state.cgItems,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 4: KraftShade Effect View (Camera & Blocker wipe filters & Focus)
            val isKraftActive = (state.blocker.visible && (state.blocker.style == "slider" || state.blocker.style == "verticalslider")) ||
                    state.camera.grayscale > 0f || state.camera.colorInverse > 0f || state.focus.amount > 0f

            if (isKraftActive) {
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

            // Layer 5: Curtain 黑幕多边形羽化裁切层
            CurtainOverlay(
                curtains = state.curtains,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 6: Subtitles & Stickers
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

            // Layer 7: Dialogue Box
            DialogueBox(
                dialogue = state.dialogue,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 8: Decision Overlay
            DecisionOverlay(
                decision = state.decision,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 9: Fast-Control Top Overlay
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