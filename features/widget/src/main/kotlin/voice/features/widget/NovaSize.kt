package voice.features.widget

import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.LocalAppWidgetOptions

/**
 * Nova reports widgets taller than it lays them out. Then none of the sizes fits, and Android shows the one with the
 * smallest area, often the landscape one in portrait. So on Nova every size is laid out for the current orientation,
 * a bit shorter than Nova reports, so nothing gets cut off.
 */
@Composable
internal fun ProvideNovaSize(content: @Composable () -> Unit) {
  val size = novaSize(LocalAppWidgetOptions.current, LocalContext.current.resources.configuration.orientation)
  if (size == null) {
    content()
  } else {
    CompositionLocalProvider(LocalSize provides size, content = content)
  }
}

internal fun novaSize(
  options: Bundle,
  orientation: Int,
): DpSize? {
  // before Android 12, Android itself shows the layout for the orientation
  if (Build.VERSION.SDK_INT < 31 || options.keySet().none { it.startsWith(NOVA_OPTION_PREFIX) }) return null
  val landscape = orientation == Configuration.ORIENTATION_LANDSCAPE
  val width = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
  val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
  if (width <= 0 || height <= 0) return null
  return DpSize(width.dp, height.dp * NOVA_HEIGHT_FACTOR)
}

private const val NOVA_OPTION_PREFIX = "com.teslacoilsw.launcher."

// measured 12 to 21% too tall
private const val NOVA_HEIGHT_FACTOR = 0.8F
