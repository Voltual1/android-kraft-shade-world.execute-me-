package world.eyes.witness.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MvFonts {
    lateinit var CodeRegular: FontFamily
        private set
    lateinit var CodeBold: FontFamily
        private set
    lateinit var DisplayBold: FontFamily
        private set
    lateinit var DisplayBlack: FontFamily
        private set
    lateinit var Terminal: FontFamily
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val assets = context.assets

        CodeRegular = try {
            FontFamily(Font("font/JetBrainsMono-Regular.ttf", assets, FontWeight.Normal))
        } catch (_: Exception) {
            FontFamily.Monospace
        }

        CodeBold = try {
            FontFamily(Font("font/JetBrainsMono-Bold.ttf", assets, FontWeight.Bold))
        } catch (_: Exception) {
            FontFamily.Monospace
        }

        DisplayBold = try {
            FontFamily(Font("font/Montserrat-Bold.ttf", assets, FontWeight.Bold))
        } catch (_: Exception) {
            FontFamily.SansSerif
        }

        DisplayBlack = try {
            FontFamily(Font("font/Montserrat-Black.ttf", assets, FontWeight.Black))
        } catch (_: Exception) {
            FontFamily.SansSerif
        }

        Terminal = try {
            FontFamily(Font("font/VT323-Regular.ttf", assets, FontWeight.Normal))
        } catch (_: Exception) {
            FontFamily.Monospace
        }

        isInitialized = true
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