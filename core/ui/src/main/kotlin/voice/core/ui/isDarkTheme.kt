package voice.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import voice.core.common.rootGraphAs

@Composable
fun isDarkTheme(): Boolean {
  return if (DARK_THEME_SETTABLE) {
    val darkThemeFlow = remember {
      rootGraphAs<SharedGraph>().useDarkThemeStore.data
    }
    darkThemeFlow.collectAsState(initial = false, context = Dispatchers.Unconfined).value
  } else {
    isSystemInDarkTheme()
  }
}
