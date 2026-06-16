@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package voice.core.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.playButtonSharedBoundsModifier(animatedVisibilityScope: AnimatedVisibilityScope): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current
    ?: return this
  return with(sharedTransitionScope) {
    sharedBounds(
      sharedContentState = rememberSharedContentState(key = "play-button"),
      animatedVisibilityScope = animatedVisibilityScope,
      enter = EnterTransition.None,
      exit = ExitTransition.None,
    )
  }
}
