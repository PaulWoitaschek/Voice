@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package voice.core.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import voice.core.data.BookId

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

fun sharedCoverKey(bookId: BookId): String = "book-cover-${bookId.value}"

/**
 * A stiff, critically damped spring: shared elements settle together with the screen cross-fade
 * instead of creeping into place after it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private val SharedElementBoundsTransform = BoundsTransform { _, _ ->
  spring(stiffness = Spring.StiffnessMedium, visibilityThreshold = Rect.VisibilityThreshold)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCoverElementModifier(bookId: BookId): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current
    ?: return this
  return with(sharedTransitionScope) {
    sharedElement(
      sharedContentState = rememberSharedContentState(key = sharedCoverKey(bookId)),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      boundsTransform = SharedElementBoundsTransform,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.playButtonSharedElementModifier(bookId: BookId): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current
    ?: return this
  return with(sharedTransitionScope) {
    sharedElement(
      sharedContentState = rememberSharedContentState(key = "play-button-${bookId.value}"),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      boundsTransform = SharedElementBoundsTransform,
    )
  }
}
