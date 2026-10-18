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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.asp.ui.StoryPlayerScreen
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val DEFAULT_PAGE_URL = "https://m.prts.wiki/w/W2G/BEG"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
private const val REFERER = "https://prts.wiki/"

class OnlineStoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val targetUrl = intent?.dataString
            ?: intent?.getStringExtra("url")
            ?: DEFAULT_PAGE_URL

        setContent {
            var scriptText by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            fun loadStory() {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        val text = fetchDatasTxtFromPage(targetUrl)
                        scriptText = text
                        isLoading = false
                    } catch (e: Throwable) {
                        errorMessage = e.message ?: "剧本获取失败"
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(targetUrl) {
                loadStory()
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
                                text = "正在连接 PRTS Wiki 获取剧本...",
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
                            Button(onClick = { loadStory() }) {
                                Text("重试")
                            }
                        }
                    }

                    scriptText != null -> {
                        StoryPlayerScreen(
                            scriptText = scriptText!!,
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

    private suspend fun fetchUrlText(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Referer", REFERER)
        }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode} 连接失败: $urlString")
        }
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /**
     * 安全解码 HTML 实体字符，保留剧情标签（不破坏 <color> 和 [command] 等标记）
     */
    private fun unescapeHtmlEntities(raw: String): String {
        return raw
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", "\u00A0")
            .replace("&amp;", "&")
    }

    /**
     * 参考示例爬虫原型抽取 #datas_txt
     */
    private suspend fun fetchDatasTxtFromPage(pageUrlOrPath: String): String {
        val pageUrl = if (!pageUrlOrPath.startsWith("http://") && !pageUrlOrPath.startsWith("https://")) {
            val pageName = pageUrlOrPath.trimStart('/')
            val path = if (!pageName.startsWith("w/")) "w/$pageName" else pageName
            "https://prts.wiki/$path"
        } else {
            pageUrlOrPath
        }

        val pageHtml = fetchUrlText(pageUrl)

        // 1. 尝试匹配 <div id="datas_txt"...> 或 <textarea id="datas_txt"...>
        val directRegex = Regex("""<[^>]+id=["']datas_txt["'][^>]*>([\s\S]*?)</(?:div|textarea)>""", RegexOption.IGNORE_CASE)
        val directMatch = directRegex.find(pageHtml)
        if (directMatch != null) {
            return unescapeHtmlEntities(directMatch.groupValues[1]).trim()
        }

        // 2. 备选方案：MediaWiki Parse API 查询
        val pageTitle = pageUrl.substringAfter("/w/")
        val apiUrl = "https://prts.wiki/api.php?action=parse&page=${URLEncoder.encode(pageTitle, "UTF-8")}&prop=text&format=json"
        val apiJsonText = fetchUrlText(apiUrl)
        val apiJson = JSONObject(apiJsonText)
        val parsedHtml = apiJson.optJSONObject("parse")?.optJSONObject("text")?.optString("*") ?: ""

        val apiMatch = directRegex.find(parsedHtml)
        if (apiMatch != null) {
            return unescapeHtmlEntities(apiMatch.groupValues[1]).trim()
        }

        throw IllegalStateException("未能在页面中找到 #datas_txt 剧本文本: $pageUrl")
    }
}