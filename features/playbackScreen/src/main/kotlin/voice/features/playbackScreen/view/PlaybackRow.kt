package voice.features.playbackScreen.view

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import voice.core.ui.LocalSharedTransitionScope
import voice.core.ui.PLAY_BUTTON_SHARED_ELEMENT_KEY
import voice.core.ui.PlayButton

@Composable
internal fun PlaybackRow(
  playing: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    SkipButton(forward = false, onClick = onRewindClick)
    Spacer(modifier = Modifier.size(16.dp))

    PlayButton(
      playing = playing,
      fabSize = 80.dp,
      iconSize = 36.dp,
      onPlayClick = onPlayClick,
      sharedElementModifier = playButtonSharedElementModifier(),
    )
    Spacer(modifier = Modifier.size(16.dp))
    SkipButton(forward = true, onClick = onFastForwardClick)
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun playButtonSharedElementModifier(): Modifier {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  return if (sharedTransitionScope != null) {
    with(sharedTransitionScope) {
      Modifier.sharedElement(
        sharedContentState = rememberSharedContentState(key = PLAY_BUTTON_SHARED_ELEMENT_KEY),
        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      )
    }
  } else {
    Modifier
  }
}
