package voice.features.playbackScreen.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.formatTime
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState
import voice.features.playbackScreen.UndoSeekViewState
import kotlin.time.Duration

@Composable
internal fun CoverRow(
  bookId: BookId,
  cover: String?,
  sleepTimerState: BookPlayViewState.SleepTimerViewState,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
  dragDelta: Duration? = null,
  duration: Duration = Duration.ZERO,
  undoSeek: UndoSeekViewState? = null,
  onUndoSeek: () -> Unit = {},
) {
  Box(modifier) {
    Cover(bookId = bookId, onDoubleClick = onPlayClick, cover = cover)
    when (sleepTimerState) {
      BookPlayViewState.SleepTimerViewState.Disabled -> {
      }
      is BookPlayViewState.SleepTimerViewState.Enabled -> {
        Text(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp)
            .background(
              color = Color(0x7E000000),
              shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
          text = when (sleepTimerState) {
            is BookPlayViewState.SleepTimerViewState.Enabled.WithDuration -> formatTime(
              timeMs = sleepTimerState.leftDuration.inWholeMilliseconds,
            )
            BookPlayViewState.SleepTimerViewState.Enabled.WithEndOfChapter -> stringResource(R.string.sleep_timer_end_of_chapter)
          },
          color = Color.White,
        )
      }
    }
    SeekOverlay(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp),
      dragDelta = dragDelta,
      undoSeek = undoSeek,
      duration = duration,
      onUndoSeek = onUndoSeek,
    )
  }
}

@Composable
private fun SeekOverlay(
  dragDelta: Duration?,
  undoSeek: UndoSeekViewState?,
  duration: Duration,
  onUndoSeek: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // while dragging again, continue counting from the jump that can still be undone
  val shownDelta = if (dragDelta != null) {
    (undoSeek?.delta ?: Duration.ZERO) + dragDelta
  } else {
    undoSeek?.delta
  }
  // keeps the last value on screen while the overlay fades out
  val lastShownDelta = remember { mutableStateOf(Duration.ZERO) }
  if (shownDelta != null) {
    lastShownDelta.value = shownDelta
  }
  AnimatedVisibility(
    modifier = modifier,
    visible = shownDelta != null,
    enter = fadeIn(),
    exit = fadeOut(animationSpec = tween(durationMillis = 600)),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        modifier = Modifier
          .background(
            color = Color(0x7E000000),
            shape = RoundedCornerShape(20.dp),
          )
          .padding(horizontal = 20.dp, vertical = 12.dp),
        text = formatSeekDelta(lastShownDelta.value, duration),
        style = MaterialTheme.typography.displaySmall,
        color = Color.White,
      )
      // the button always takes up its space so the delta above it never moves
      val undoVisible = undoSeek != null && dragDelta == null
      val undoAlpha by animateFloatAsState(targetValue = if (undoVisible) 1F else 0F)
      FilledTonalButton(
        modifier = Modifier
          .padding(top = 12.dp)
          .alpha(undoAlpha)
          .then(if (undoVisible) Modifier else Modifier.clearAndSetSemantics {}),
        onClick = {
          if (undoVisible) onUndoSeek()
        },
      ) {
        Icon(
          imageVector = VoiceIcons.Undo,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.playback_seek_undo))
      }
    }
  }
}

internal fun formatSeekDelta(
  delta: Duration,
  duration: Duration,
): String {
  val sign = if (delta.isNegative()) "-" else "+"
  return sign + formatTime(
    timeMs = delta.absoluteValue.inWholeMilliseconds,
    durationMs = duration.inWholeMilliseconds,
  )
}
