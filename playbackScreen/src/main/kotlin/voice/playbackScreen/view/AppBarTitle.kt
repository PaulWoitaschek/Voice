package voice.playbackScreen.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import voice.playbackScreen.BookPlayViewState

@Composable
internal fun AppBarTitle(viewState: BookPlayViewState) {
  Text(text = viewState.title)
}
