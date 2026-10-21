package world.eyes.witness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.voltual.sa.StillAliveScreen

public class StillAliveTestActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			StillAliveScreen()
		}
	}
}