package voice.playbackScreen.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.strings.R

@Composable
internal fun CloseIcon(onCloseClick: () -> Unit) {
  IconButton(onClick = onCloseClick) {
    Icon(
      imageVector = Icons.Outlined.Close,
      contentDescription = stringResource(id = R.string.close),
    )
  }
}
