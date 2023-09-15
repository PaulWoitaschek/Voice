package voice.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import dev.olshevski.navigation.reimagined.NavAction
import voice.common.navigation.Destination

internal fun navTransition(
  action: NavAction,
  destination: Destination.Compose,
) = when (action) {
  NavAction.Navigate -> {
    val enter = if (destination is Destination.AddContent) {
      // we come from the system activity, don't show a transition here
      EnterTransition.None
    } else {
      fadeIn(tween(700))
        .plus(slideInHorizontally { it / 2 })
    }
    val exit = fadeOut()
    enter togetherWith exit
  }
  NavAction.Pop -> {
    fadeIn(tween(700))
      .togetherWith(
        fadeOut(tween(300))
          .plus(
            slideOutHorizontally(tween(700)) { it / 2 },
          ),
      )
  }
  else -> fadeIn(tween(700)) togetherWith fadeOut(tween(700))
}
