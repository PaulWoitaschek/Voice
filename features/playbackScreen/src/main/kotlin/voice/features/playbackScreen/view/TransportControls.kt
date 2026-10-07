package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.ui.PlayButton
import voice.core.ui.playButtonSharedElementModifier

@Composable
internal fun TransportControls(
  bookId: BookId,
  playing: Boolean,
  skipSeconds: Int,
  onRewindClick: () -> Unit,
  onPlayClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  modifier: Modifier = Modifier,
  playButtonSize: Dp = 104.dp,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SkipButton(
      forward = false,
      seconds = skipSeconds,
      onClick = onRewindClick,
      height = playButtonSize * 0.72F,
    )
    PlayButton(
      playing = playing,
      onClick = onPlayClick,
      modifier = Modifier.playButtonSharedElementModifier(bookId),
      size = playButtonSize,
    )
    SkipButton(
      forward = true,
      seconds = skipSeconds,
      onClick = onFastForwardClick,
      height = playButtonSize * 0.72F,
    )
  }
}
