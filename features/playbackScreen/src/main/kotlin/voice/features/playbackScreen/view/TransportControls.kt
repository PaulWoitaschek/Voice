package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
      modifier = Modifier.fillMaxHeight(0.72F),
    )
    PlayButton(
      playing = playing,
      onClick = onPlayClick,
      modifier = Modifier
        .playButtonSharedElementModifier(bookId)
        .fillMaxHeight()
        .aspectRatio(1F, matchHeightConstraintsFirst = true),
    )
    SkipButton(
      forward = true,
      seconds = skipSeconds,
      onClick = onFastForwardClick,
      modifier = Modifier.fillMaxHeight(0.72F),
    )
  }
}
