package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.formatTime
import kotlin.time.Duration

@Composable
internal fun TotalProgressRow(
  totalDuration: Duration,
  totalPlayedTime: Duration,
) {
  val totalDurationMs = totalDuration.inWholeMilliseconds
  val totalPlayedMs = totalPlayedTime.inWholeMilliseconds

  val progress = if (totalDurationMs > 0) {
    (totalPlayedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0F, 1F)
  } else {
    0F
  }

  val percentComplete = (progress * 100).toInt()
  val remainingTimeMs = (totalDurationMs - totalPlayedMs).coerceAtLeast(0L)
  val remainingFormatted = formatTime(remainingTimeMs, remainingTimeMs)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.playback_total_progress_summary, percentComplete, remainingFormatted),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp)
        .height(2.dp),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
  }
}
