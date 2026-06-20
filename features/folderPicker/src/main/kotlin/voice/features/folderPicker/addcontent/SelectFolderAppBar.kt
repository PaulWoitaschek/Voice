package voice.features.folderPicker.addcontent

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.core.strings.R
import voice.core.ui.icons.ArrowBack
import voice.core.ui.icons.VoiceIcons

@Composable
internal fun SelectFolderAppBar(onBack: () -> Unit) {
  TopAppBar(
    title = { },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = VoiceIcons.ArrowBack,
          contentDescription = stringResource(id = R.string.common_action_close),
        )
      }
    },
  )
}
