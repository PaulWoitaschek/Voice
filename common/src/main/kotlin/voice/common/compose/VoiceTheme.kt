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
import androidx.compose.ui.platform.LocalContext
import de.ph1b.audiobook.common.DARK_THEME_SETTABLE
import de.ph1b.audiobook.rootComponentAs
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.MaterialTheme as Material3Theme

@Composable
fun VoiceTheme(
  content: @Composable () -> Unit
) {
  Material3Theme(
    colorScheme = if (isDarkTheme()) {
      dynamicDarkColorScheme(LocalContext.current)
    } else {
      dynamicLightColorScheme(LocalContext.current)
    }
  ) {
    val colorScheme = Material3Theme.colorScheme
    MaterialTheme(
      colors = if (isDarkTheme()) {
        darkColors(
          primary = colorScheme.primary,
          primaryVariant = colorScheme.primary,
          secondary = colorScheme.secondary,
          secondaryVariant = colorScheme.secondary,
          background = colorScheme.background,
          surface = colorScheme.surface,
          error = colorScheme.error,
          onPrimary = colorScheme.onPrimary,
          onSecondary = colorScheme.onSecondary,
          onBackground = colorScheme.onBackground,
          onSurface = colorScheme.onSurface,
          onError = colorScheme.onError,
        )
      } else {
        lightColors(
          primary = colorScheme.primary,
          primaryVariant = colorScheme.primary,
          secondary = colorScheme.secondary,
          secondaryVariant = colorScheme.secondary,
          background = colorScheme.background,
          surface = colorScheme.surface,
          error = colorScheme.error,
          onPrimary = colorScheme.onPrimary,
          onSecondary = colorScheme.onSecondary,
          onBackground = colorScheme.onBackground,
          onSurface = colorScheme.onSurface,
          onError = colorScheme.onError,
        )
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
