package voice.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import voice.strings.R

@Composable
internal fun ChapterRow(
  chapterName: String,
  nextPreviousVisible: Boolean,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (nextPreviousVisible) {
      IconButton(onClick = onSkipToPrevious) {
        Icon(
          modifier = Modifier.size(36.dp),
          imageVector = Icons.Outlined.ChevronLeft,
          contentDescription = stringResource(id = R.string.previous_track),
        )
      }
    }
    Text(
      modifier = Modifier
        .weight(1F)
        .clickable(onClick = onCurrentChapterClick)
        .padding(vertical = 16.dp),
      text = chapterName,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
    if (nextPreviousVisible) {
      IconButton(onClick = onSkipToNext) {
        Icon(
          modifier = Modifier.size(36.dp),
          imageVector = Icons.Outlined.ChevronRight,
          contentDescription = stringResource(id = R.string.next_track),
        )
      }
    }
  }
}
