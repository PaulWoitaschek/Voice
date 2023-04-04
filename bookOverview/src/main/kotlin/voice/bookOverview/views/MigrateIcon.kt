package voice.bookOverview.views

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import voice.common.compose.VoiceTheme
import voice.strings.R as StringsR

@Composable
internal fun MigrateIcon(
  withHint: Boolean,
  onClick: () -> Unit,
  onHintClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box {
    IconButton(modifier = modifier, onClick = onClick) {
      Icon(
        imageVector = Icons.Outlined.Restore,
        contentDescription = stringResource(StringsR.string.migration_hint_title),
        tint = MaterialTheme.colorScheme.primary,
      )
    }
    if (withHint) {
      MigrateHint(onHintClick)
    }
  }
}

@Preview
@Composable
private fun MigrateIconPreview() {
  VoiceTheme {
    MigrateIcon(withHint = false, onClick = {}, onHintClick = {})
  }
}
