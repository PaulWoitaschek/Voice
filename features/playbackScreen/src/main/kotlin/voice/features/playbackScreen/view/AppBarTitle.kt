package voice.features.playbackScreen.view

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AppBarTitle(title: String) {
  Text(
    text = title,
    modifier = Modifier
    .fillMaxWidth()
    .padding(end = 16.dp)
    .basicMarquee(),
  )
}
