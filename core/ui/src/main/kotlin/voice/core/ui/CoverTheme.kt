@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme

/**
 * Seeds are cached (see [CoverSeedColors]), so a cover that was analyzed before themes the screen
 * from its very first frame. Until a seed is known the surrounding theme is used unchanged, and the
 * switch animates.
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
  val seedColors = LocalCoverSeedColors.current
  val seed by produceState(initialValue = cover?.let { seedColors?.cached(it) }, cover, seedColors) {
    value = cover?.let { seedColors?.cached(it) }
    if (cover != null && seedColors != null) {
      value = seedColors.load(cover)
    }
  }
  return seed?.takeIf { it.isSpecified }
}
