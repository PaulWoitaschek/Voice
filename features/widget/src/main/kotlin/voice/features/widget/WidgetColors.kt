package voice.features.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.unit.ColorProvider
import voice.core.data.ThemeColorScheme
import voice.core.ui.coverColorScheme
import voice.core.ui.themeColorScheme
import androidx.glance.material3.ColorProviders as Material3ColorProviders

/**
 * Colors the widget like the player: from the cover when it has a usable color, otherwise from the
 * app's color scheme. At [night], the widget is dark whatever the system theme.
 */
@Composable
internal fun WidgetColors(
  theme: WidgetTheme,
  night: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colors = remember(theme, night) { widgetColors(context, theme, night) }
  GlanceTheme(colors = colors, content = content)
}

private fun widgetColors(
  context: Context,
  theme: WidgetTheme,
  night: Boolean,
): ColorProviders {
  val seed = theme.seed?.let(::Color)
  fun scheme(dark: Boolean): ColorScheme = if (seed != null) {
    coverColorScheme(seed, dark)
  } else {
    themeColorScheme(context, theme.themeColorScheme, dark)
  }
  return when {
    night -> Material3ColorProviders(scheme(dark = true))
    // these follow wallpaper changes without the widget having to update
    seed == null && theme.themeColorScheme == ThemeColorScheme.Dynamic && Build.VERSION.SDK_INT >= 31 -> DynamicThemeColorProviders
    else -> Material3ColorProviders(light = scheme(dark = false), dark = scheme(dark = true))
  }
}

/**
 * The widget's background, rounded like the launcher rounds widgets, or a [pill] for widgets one row
 * high. Before Android 12 the system doesn't round widgets, so the background does.
 */
internal fun GlanceModifier.widgetSurface(
  color: ColorProvider,
  pill: Boolean = false,
): GlanceModifier {
  return when {
    pill -> pillBackground(color)
    Build.VERSION.SDK_INT >= 31 -> background(color).cornerRadius(android.R.dimen.system_app_widget_background_radius)
    else -> background(ImageProvider(R.drawable.widget_surface), colorFilter = ColorFilter.tint(color))
  }
}

/** A background with fully rounded ends, for chips, buttons and one row widgets. */
internal fun GlanceModifier.pillBackground(color: ColorProvider): GlanceModifier {
  return background(ImageProvider(R.drawable.widget_pill), colorFilter = ColorFilter.tint(color))
}

internal val White = ColorProvider(Color.White)
