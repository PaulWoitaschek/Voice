@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.ktx.themeColorOrNull
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Re-themes its content with a color scheme seeded from the book [cover].
 *
 * Seeds are cached for the lifetime of the process, so a cover that was already shown somewhere
 * (e.g. on the library screen) themes the next screen from its very first frame. Until a seed is
 * known the surrounding theme is used unchanged, and the switch animates.
 */
@Composable
fun CoverTheme(
  cover: String?,
  content: @Composable () -> Unit,
) {
  val seed = rememberCoverSeedColor(cover)
  val baseScheme = MaterialTheme.colorScheme
  val targetScheme = if (seed != null) {
    rememberDynamicColorScheme(
      seedColor = seed,
      isDark = baseScheme.surface.luminance() < 0.5F,
      style = PaletteStyle.Vibrant,
      specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
  } else {
    baseScheme
  }
  MaterialExpressiveTheme(
    colorScheme = animateColorScheme(targetScheme),
    motionScheme = MaterialTheme.motionScheme,
    shapes = MaterialTheme.shapes,
    typography = MaterialTheme.typography,
    content = content,
  )
}

/** The theme seed for [cover], or null while unknown or if the cover has no usable color. */
@Composable
fun rememberCoverSeedColor(cover: String?): Color? {
  val context = LocalContext.current
  val seed by produceState(initialValue = cover?.let(CoverSeedColors::cached), cover) {
    value = cover?.let(CoverSeedColors::cached)
    if (cover != null) {
      value = CoverSeedColors.load(context, cover)
    }
  }
  return seed?.takeIf { it.isSpecified }
}

/**
 * Process wide cache of cover seed colors. Covers are stored under unique file names, so a changed
 * cover always gets a new key. [Color.Unspecified] marks covers without a usable color.
 */
private object CoverSeedColors {

  private val cache = ConcurrentHashMap<String, Color>()

  fun cached(cover: String): Color? = cache[cover]

  suspend fun load(
    context: Context,
    cover: String,
  ): Color? {
    cache[cover]?.let { return it }
    val request = ImageRequest.Builder(context)
      .data(cover)
      .size(128)
      .allowHardware(false)
      .build()
    // failed loads are not cached so they are retried the next time
    val result = context.imageLoader.execute(request) as? SuccessResult
      ?: return null
    val color = withContext(Dispatchers.Default) {
      result.drawable.toBitmap().asImageBitmap().themeColorOrNull()
    } ?: Color.Unspecified
    cache[cover] = color
    return color
  }
}
