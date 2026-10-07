@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package voice.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Counts the screens that are not ready to be shown yet. While there are any, the activity keeps the
 * splash screen up, so the app opens straight onto a finished screen instead of a half loaded one.
 */
class SplashScreenHolds {

  private var count = 0

  val holding: Boolean get() = count > 0

  internal fun add() {
    count++
  }

  internal fun remove() {
    count--
  }
}

/** Provided by the activity. Holds only matter until it has drawn its first frame. */
val LocalSplashScreenHolds = staticCompositionLocalOf<SplashScreenHolds?> { null }

/** Keeps the splash screen up while [loading]. Only the screen the app opens on is affected. */
@Composable
fun HoldSplashScreenWhile(loading: Boolean) {
  val holds = LocalSplashScreenHolds.current ?: return
  if (loading) {
    // registered when the composition is applied, so the hold is in place before the frame is drawn
    DisposableEffect(holds) {
      holds.add()
      onDispose { holds.remove() }
    }
  }
}
