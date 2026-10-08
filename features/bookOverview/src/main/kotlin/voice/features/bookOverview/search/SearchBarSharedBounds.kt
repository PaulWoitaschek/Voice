package voice.features.bookOverview.search

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import voice.core.ui.LocalSharedTransitionScope

/**
 * Lets the library's search pill grow into the search field, and shrink back.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.searchBarSharedBounds(shape: Shape): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current
    ?: return this
  return with(sharedTransitionScope) {
    sharedBounds(
      sharedContentState = rememberSharedContentState(key = "library-search-bar"),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
      clipInOverlayDuringTransition = OverlayClip(shape),
    )
  }
}
