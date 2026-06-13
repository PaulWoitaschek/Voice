package voice.core.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode

val VoiceBlue = Color(0xFF003b7f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoiceTheme(
  themeMode: ThemeMode = ThemeMode.FollowSystem,
  themeColorScheme: ThemeColorScheme = ThemeColorScheme.VoiceBlue,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.FollowSystem -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
  }
  val themedContent = remember(content) {
    movableContentOf {
      content()
    }
  }
  if (themeColorScheme == ThemeColorScheme.Dynamic && Build.VERSION.SDK_INT >= 31) {
    MaterialExpressiveTheme(
      colorScheme = systemDynamicColorScheme(darkTheme),
    ) {
      themedContent()
    }
  } else {
    DynamicMaterialExpressiveTheme(
      primary = VoiceBlue,
      secondary = Color(0xFF5E6F95),
      isDark = darkTheme,
      style = PaletteStyle.Expressive,
      specVersion = ColorSpec.SpecVersion.SPEC_2025,
    ) {
      themedContent()
    }
  }
}

@RequiresApi(31)
@Composable
private fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme {
  return if (darkTheme) {
    dynamicDarkColorScheme(LocalContext.current)
  } else {
    dynamicLightColorScheme(LocalContext.current)
  }
}
