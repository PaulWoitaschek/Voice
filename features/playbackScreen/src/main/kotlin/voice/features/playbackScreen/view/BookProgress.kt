@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.BookBar
import voice.core.ui.formatTime
import voice.features.playbackScreen.BookPlayViewState
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * Progress through the whole book, drawn as one segment per chapter so the book's shape is
 * visible at a glance. The current chapter's segment stands out and bookmarks sit on top as pins;
 * below sits the percentage and the listening time left at the current playback speed.
 */
@Composable
internal fun BookProgress(
  viewState: BookPlayViewState,
  modifier: Modifier = Modifier,
) {
  val progress by animateFloatAsState(
    targetValue = viewState.bookProgress,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "bookProgress",
  )
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  val speedFormat = remember { DecimalFormat("0.0#") }
  val percent = percentFormat.format(viewState.bookProgress.toDouble())
  val remaining = (viewState.bookDuration - viewState.bookPlayedTime) / viewState.playbackSpeed.coerceAtLeast(0.1F).toDouble()
  val colors = MaterialTheme.colorScheme
  val segments = viewState.chapterSegments
  val currentSegment = viewState.chapterNumber - 1

  Column(
    modifier = modifier.semantics(mergeDescendants = true) {
      progressBarRangeInfo = ProgressBarRangeInfo(current = viewState.bookProgress, range = 0F..1F)
    },
  ) {
    BookBar(
      segments = segments,
      currentSegment = currentSegment,
      progress = progress,
      pins = viewState.bookmarkPins,
      popPin = viewState.poppedPin,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(R.string.playback_book_progress_read, percent),
        style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
        color = colors.primary,
      )
      Spacer(Modifier.weight(1F))
      val remainingText = formatTime(remaining.inWholeMilliseconds)
      Text(
        text = if (viewState.playbackSpeed in 0.99F..1.01F) {
          stringResource(R.string.playback_book_progress_remaining, remainingText)
        } else {
          stringResource(
            R.string.playback_book_progress_remaining_at_speed,
            remainingText,
            speedFormat.format(viewState.playbackSpeed) + "×",
          )
        },
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
