package voice.common.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.google.accompanist.insets.ProvideWindowInsets
import de.ph1b.audiobook.common.DARK_THEME_SETTABLE
import de.ph1b.audiobook.rootComponentAs
import kotlinx.coroutines.Dispatchers

@Composable
fun VoiceTheme(
  content: @Composable () -> Unit
) {
  val colors = if (isDarkTheme()) {
    darkColors()
  } else {
    lightColors()
  }
  MaterialTheme(colors = colors) {
    ProvideWindowInsets {
      content()
    }
  }
}

@Composable
fun isDarkTheme(): Boolean {
  return if (DARK_THEME_SETTABLE) {
    val darkThemeFlow = remember {
      rootComponentAs<SharedComponent>().useDarkTheme.flow
    }
    darkThemeFlow.collectAsState(initial = false, context = Dispatchers.Unconfined).value
  } else {
    isSystemInDarkTheme()
  }
}
