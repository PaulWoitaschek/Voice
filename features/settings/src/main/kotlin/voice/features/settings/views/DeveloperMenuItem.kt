@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons

@Composable
internal fun DeveloperMenuItem(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = IslandShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    IslandRow(
      modifier = Modifier.padding(vertical = 8.dp),
      title = "Developer Menu",
      onClick = onClick,
      leading = {
        ShapedIcon(
          icon = VoiceIcons.Laptop,
          shape = MaterialShapes.PixelCircle,
          containerColor = MaterialTheme.colorScheme.inverseSurface,
          contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        )
      },
    )
  }
}
