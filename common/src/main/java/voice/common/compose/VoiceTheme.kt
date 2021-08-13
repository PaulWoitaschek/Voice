package voice.common.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.accompanist.insets.ProvideWindowInsets
import de.ph1b.audiobook.common.DARK_THEME_SETTABLE
import de.ph1b.audiobook.rootComponentAs
import kotlinx.coroutines.Dispatchers

object VoiceColors {
  val Red700 = Color(0xFFD32F2F)
}

@Composable
fun VoiceTheme(
  content: @Composable () -> Unit
) {
  val colors = if (isDarkTheme()) {
    darkColors(
      primary = Color.Black,
      onPrimary = Color.White,
      secondary = VoiceColors.Red700,
      secondaryVariant = VoiceColors.Red700
    )
  } else {
    lightColors(
      primary = Color.White,
      onPrimary = Color.Black,
      secondary = VoiceColors.Red700,
      secondaryVariant = VoiceColors.Red700
    )
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
