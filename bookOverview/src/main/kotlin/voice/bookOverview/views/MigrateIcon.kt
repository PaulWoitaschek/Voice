package voice.bookOverview.views

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.bookOverview.R

@Composable
internal fun MigrateIcon(
  modifier: Modifier = Modifier,
  withHint: Boolean,
  onClick: () -> Unit,
  onHintClick: () -> Unit,
) {
  Box {
    IconButton(modifier = modifier, onClick = onClick) {
      Icon(
        imageVector = Icons.Outlined.CompareArrows,
        contentDescription = stringResource(R.string.migration_hint_title),
      )
    }
    if (withHint) {
      MigrateHint(onHintClick)
    }
  }
}
