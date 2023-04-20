package voice.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import dev.olshevski.navigation.reimagined.NavAction

internal fun navTransition(action: NavAction) = when (action) {
  NavAction.Navigate -> {
    fadeIn(tween(700))
      .plus(slideInHorizontally { it / 2 })
      .with(fadeOut())
  }
  NavAction.Pop -> {
    fadeIn(tween(700))
      .with(
        fadeOut(tween(300))
          .plus(
            slideOutHorizontally(tween(700)) { it / 2 },
          ),
      )
  }
  else -> fadeIn(tween(700)) with fadeOut(tween(700))
}
