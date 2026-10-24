package voice.core.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme

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
  pureBlack: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.FollowSystem -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
  }
  MaterialExpressiveTheme(
    colorScheme = animateColorScheme(rememberThemeColorScheme(themeColorScheme, darkTheme, pureBlack)),
    typography = VoiceTypography,
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
 * The colors [themeColorScheme] produces, light or [dark], with a black background in the dark
 * theme if [pureBlack]. Dynamic color needs Android 12, below that it falls back to Voice blue.
 */
@Composable
fun rememberThemeColorScheme(
  themeColorScheme: ThemeColorScheme,
  dark: Boolean,
  pureBlack: Boolean = false,
): ColorScheme {
  val context = LocalContext.current
  return remember(context, themeColorScheme, dark, pureBlack) {
    themeColorScheme(context, themeColorScheme, dark, pureBlack)
  }
}

/** The colors [themeColorScheme] produces outside of composition, see [rememberThemeColorScheme]. */
fun themeColorScheme(
  context: Context,
  themeColorScheme: ThemeColorScheme,
  dark: Boolean,
  pureBlack: Boolean = false,
): ColorScheme {
  val colors = if (themeColorScheme == ThemeColorScheme.Dynamic && Build.VERSION.SDK_INT >= 31) {
    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else {
    dynamicColorScheme(
      // the seed as primary keeps its full saturation, e.g. the deep Voice blue of the launcher icon
      primary = seedColor(themeColorScheme),
      isDark = dark,
      // derives secondary and tertiary the same way Android does from the wallpaper
      style = PaletteStyle.TonalSpot,
      specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
  }
  return if (dark && pureBlack) colors.withPureBlack() else colors
}

/**
 * Only the background turns black. Containers keep their tones, so cards, sheets and bars still
 * stand out from it.
 */
internal fun ColorScheme.withPureBlack(): ColorScheme = copy(
  background = Color.Black,
  surface = Color.Black,
  surfaceDim = Color.Black,
  surfaceContainerLowest = Color.Black,
)

internal val ColorScheme.isPureBlack: Boolean
  get() = surface == Color.Black

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
