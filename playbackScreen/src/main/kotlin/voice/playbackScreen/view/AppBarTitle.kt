package voice.playbackScreen.view

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AppBarTitle(title: String) {
  Text(
    text = title,
    modifier = Modifier.basicMarquee(),
  )
}
