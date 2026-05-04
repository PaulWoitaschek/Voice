package voice.features.playbackScreen.view

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

@Composable
internal fun AppBarTitle(title: String) {
  Text(
    text = title,
    modifier = Modifier
      .fillMaxWidth()
      .clipToBounds()
      .basicMarquee(),
  )
}
