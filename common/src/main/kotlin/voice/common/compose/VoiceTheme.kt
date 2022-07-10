package voice.common.compose

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import voice.common.DARK_THEME_SETTABLE
import voice.common.rootComponentAs
import androidx.compose.material3.MaterialTheme as Material3Theme

@Composable
fun VoiceTheme(
  content: @Composable () -> Unit
) {
  Material3Theme(
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
    }
  ) {
    MaterialTheme(
      colors = if (isDarkTheme()) {
        darkColors()
      } else {
        lightColors()
      }
    ) {
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
