package voice.playbackScreen

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.google.accompanist.insets.ProvideWindowInsets

@Composable
fun VoiceTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme {
    ProvideWindowInsets {
      content()
    }
  }
}
