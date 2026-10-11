package world.eyes.witness.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MvFontFamilies {
    /**
     * 编程源码、终端输出与时钟
     */
    var CodeFont: FontFamily = FontFamily.Monospace
        private set

    /**
     * 动力学大字排版（EXECUTION, PROTECTION, SIMULATION 等）
     */
    var DisplayFont: FontFamily = FontFamily.SansSerif
        private set

    /**
     * 复古 CRT 点阵终端与系统报错
     */
    var TerminalFont: FontFamily = FontFamily.Monospace
        private set

    /**
     * 在 App 启动或进入 Stage 时从 assets/font/ 动态加载真实字体
     * 若 assets 文件未就绪，会自动降级为系统默认 Monospace / SansSerif，保证不崩溃
     */
    fun loadAssetsFonts(assetManager: AssetManager) {
        try {
            CodeFont = FontFamily(
                androidx.compose.ui.text.font.Font("font/JetBrainsMono-Regular.ttf", assetManager, FontWeight.Normal),
                androidx.compose.ui.text.font.Font("font/JetBrainsMono-Bold.ttf", assetManager, FontWeight.Bold)
            )
        } catch (_: Exception) {
            CodeFont = FontFamily.Monospace
        }

        try {
            DisplayFont = FontFamily(
                androidx.compose.ui.text.font.Font("font/Montserrat-Bold.ttf", assetManager, FontWeight.Bold),
                androidx.compose.ui.text.font.Font("font/Montserrat-Black.ttf", assetManager, FontWeight.Black)
            )
        } catch (_: Exception) {
            DisplayFont = FontFamily.SansSerif
        }

        try {
            TerminalFont = FontFamily(
                androidx.compose.ui.text.font.Font("font/VT323-Regular.ttf", assetManager, FontWeight.Normal)
            )
        } catch (_: Exception) {
            TerminalFont = FontFamily.Monospace
        }
    }
}

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)