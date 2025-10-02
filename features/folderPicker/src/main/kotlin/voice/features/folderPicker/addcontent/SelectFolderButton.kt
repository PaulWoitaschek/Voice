package voice.features.folderPicker.addcontent

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun SelectFolderButton(
  icon: ImageVector,
  text: String,
  onClick: () -> Unit,
) {
  FilledTonalButton(
    onClick = onClick,
  ) {
    Icon(
      modifier = Modifier.size(ButtonDefaults.IconSize),
      imageVector = icon,
      contentDescription = text,
    )
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text(text = text)
  }
}
