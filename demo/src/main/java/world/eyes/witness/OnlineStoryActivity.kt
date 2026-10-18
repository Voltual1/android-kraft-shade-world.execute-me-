package world.eyes.witness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import me.voltual.asp.Context as StoryContext
import me.voltual.asp.loadContextFromAssets
import me.voltual.asp.ui.StoryPlayerScreen

class OnlineStoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val androidContext = LocalContext.current
            var scriptText by remember { mutableStateOf<String?>(null) }
            var storyContext by remember { mutableStateOf<StoryContext?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            fun loadLocalStory() {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        // 直接从本地 assets 加载 story/W2G_BEG 剧本包与全套资源
                        val (text, ctx) = loadContextFromAssets(androidContext, "story/W2G_BEG")
                        scriptText = text
                        storyContext = ctx
                        isLoading = false
                    } catch (e: Throwable) {
                        errorMessage = e.message ?: "本地剧本加载失败"
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(Unit) {
                loadLocalStory()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Text(
                                text = "正在读取本地剧本资源...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "剧本加载失败",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage ?: "",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Button(onClick = { loadLocalStory() }) {
                                Text("重试")
                            }
                        }
                    }

                    scriptText != null && storyContext != null -> {
                        StoryPlayerScreen(
                            scriptText = scriptText!!,
                            initialContext = storyContext,
                            modifier = Modifier.fillMaxSize(),
                            onFinished = {
                                finish()
                            }
                        )
                    }
                }
            }
        }

        hideSystemUi()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}