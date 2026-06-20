package voice.features.playbackScreen.view

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

@Composable
internal fun CloseIcon(onCloseClick: () -> Unit) {
  IconButton(onClick = onCloseClick) {
    Icon(
      imageVector = VoiceIcons.Close,
      contentDescription = stringResource(id = R.string.common_action_close),
    )
  }
}
