@file:Suppress("ComposeCompositionLocalUsage")

package voice.core.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import voice.core.data.BookId

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalSharesCovers =
  staticCompositionLocalOf<(initial: Scene<*>, target: Scene<*>) -> Boolean> { { _, _ -> true } }

val LocalSharesSeriesCovers =
  staticCompositionLocalOf<(initial: Scene<*>, target: Scene<*>) -> Boolean> { { _, _ -> true } }

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
private fun rememberScenesConfig(
  shares: (initial: Scene<*>, target: Scene<*>) -> Boolean,
): SharedTransitionScope.SharedContentConfig {
  val animatedContentScope = LocalNavAnimatedContentScope.current
  return remember(animatedContentScope, shares) {
    object : SharedTransitionScope.SharedContentConfig {
      override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
        get() {
          val scenes = animatedContentScope.transition.parentTransition ?: return true
          val initial = scenes.currentState as? Scene<*> ?: return true
          val target = scenes.targetState as? Scene<*> ?: return true
          return shares(initial, target)
        }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCoverElementModifier(bookId: BookId): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
  val config = rememberScenesConfig(LocalSharesCovers.current)
  return with(sharedTransitionScope) {
    sharedElement(
      sharedContentState =
        rememberSharedContentState(key = sharedCoverKey(bookId), config = config),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      boundsTransform = SharedElementBoundsTransform,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.seriesCoverSharedElementModifier(seriesKey: String): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
  val config = rememberScenesConfig(LocalSharesSeriesCovers.current)
  return with(sharedTransitionScope) {
    sharedElement(
      sharedContentState =
        rememberSharedContentState(key = "series-cover-$seriesKey", config = config),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      boundsTransform = SharedElementBoundsTransform,
    )
  }
}

// bounds, as the covers of the author's tile turn into their photo
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.authorSharedBoundsModifier(authorKey: String): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
  return with(sharedTransitionScope) {
    sharedBounds(
      sharedContentState = rememberSharedContentState(key = "author-$authorKey"),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      boundsTransform = SharedElementBoundsTransform,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.playButtonSharedElementModifier(bookId: BookId): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
  return with(sharedTransitionScope) {
    sharedElement(
      sharedContentState = rememberSharedContentState(key = "play-button-${bookId.value}"),
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      boundsTransform = SharedElementBoundsTransform,
    )
  }
}
