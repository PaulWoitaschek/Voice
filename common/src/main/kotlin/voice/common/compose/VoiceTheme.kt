package voice.common.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
  MaterialTheme(colors = if (isDarkTheme()) {
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
  }) {
    androidx.compose.material3.MaterialTheme(colorScheme = if (isDarkTheme()) {
      dynamicDarkColorScheme(LocalContext.current)
    } else {
      dynamicLightColorScheme(LocalContext.current)
    }) {
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
