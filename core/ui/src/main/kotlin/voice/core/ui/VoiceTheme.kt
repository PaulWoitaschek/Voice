package voice.core.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun VoiceTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = if (isDarkTheme()) {
      if (Build.VERSION.SDK_INT >= 31) {
        dynamicDarkColorScheme(LocalContext.current)
      } else {
        darkColorScheme()
      }
    } else {
      if (Build.VERSION.SDK_INT >= 31) {
        dynamicLightColorScheme(LocalContext.current)
      } else {
        lightColorScheme()
      }
    },
  ) {
    content()
  }
}
