package voice.playbackScreen.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun ChapterRow(chapterName: String, listener: BookPlayListener) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    PreviousTrack(listener)
    Spacer(modifier = Modifier.width(8.dp))
    ChapterName(chapterName)
    Spacer(modifier = Modifier.width(8.dp))
    NextTrack(listener)
  }
}

@Composable
private fun PreviousTrack(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.previousTrack()
    }
  ) {
    Icon(
      imageVector = Icons.Default.ChevronLeft,
      contentDescription = stringResource(R.string.previous_track)
    )
  }
}

@Composable
private fun NextTrack(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.nextTrack()
    }
  ) {
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = stringResource(R.string.next_track)
    )
  }
}

@Composable
private fun ChapterName(chapterName: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = chapterName
    )
    Icon(
      imageVector = Icons.Default.ArrowDropDown,
      contentDescription = stringResource(R.string.next_track)
    )
  }
}
