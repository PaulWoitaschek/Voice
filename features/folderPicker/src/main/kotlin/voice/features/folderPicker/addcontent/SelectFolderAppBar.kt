package voice.features.folderPicker.addcontent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.core.strings.R

@Composable
internal fun SelectFolderAppBar(onBack: () -> Unit) {
  TopAppBar(
    title = { },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = stringResource(id = R.string.close),
        )
      }
    },
  )
}
