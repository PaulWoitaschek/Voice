package voice.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode

val VoiceBlue = Color(0xFF003b7f)

/**
 * The app theme. Switching the [themeColorScheme] (or the mode, where that doesn't recreate the
 * activity) blends the colors over instead of swapping them in a single frame.
 */
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
  MaterialExpressiveTheme(
    colorScheme = animateColorScheme(rememberThemeColorScheme(themeColorScheme, darkTheme)),
    content = content,
  )
}

/**
 * Shows [content] in the dark colors of [themeColorScheme] while it's [night], whatever the app's
 * theme. Day and night blend over into each other.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NightTheme(
  themeColorScheme: ThemeColorScheme,
  night: Boolean,
  content: @Composable () -> Unit,
) {
  val nightColors = rememberThemeColorScheme(themeColorScheme, dark = true)
  MaterialExpressiveTheme(
    colorScheme = animateColorScheme(if (night) nightColors else MaterialTheme.colorScheme),
    content = content,
  )
}

/**
 * The colors [themeColorScheme] produces, light or [dark]. Dynamic color needs Android 12, below
 * that it falls back to Voice blue.
 */
@Composable
fun rememberThemeColorScheme(
  themeColorScheme: ThemeColorScheme,
  dark: Boolean,
): ColorScheme {
  if (themeColorScheme == ThemeColorScheme.Dynamic && Build.VERSION.SDK_INT >= 31) {
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  }
  return rememberDynamicColorScheme(
    // the seed as primary keeps its full saturation, e.g. the deep Voice blue of the launcher icon
    primary = seedColor(themeColorScheme),
    isDark = dark,
    // derives secondary and tertiary the same way Android does from the wallpaper
    style = PaletteStyle.TonalSpot,
    specVersion = ColorSpec.SpecVersion.SPEC_2025,
  )
}

private fun seedColor(themeColorScheme: ThemeColorScheme): Color = when (themeColorScheme) {
  ThemeColorScheme.VoiceBlue,
  ThemeColorScheme.Dynamic,
  -> VoiceBlue
  ThemeColorScheme.Lagoon -> Color(0xFF00696E)
  ThemeColorScheme.Forest -> Color(0xFF3B6D2A)
  ThemeColorScheme.Sunset -> Color(0xFFC4501F)
  ThemeColorScheme.Berry -> Color(0xFFA3305F)
  ThemeColorScheme.Lavender -> Color(0xFF6B4EB8)
}
