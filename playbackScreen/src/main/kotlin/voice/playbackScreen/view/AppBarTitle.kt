package voice.playbackScreen.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun AppBarTitle(title: String) {
  Text(
    text = title,
    overflow = TextOverflow.Ellipsis,
    maxLines = 2
  )
}
