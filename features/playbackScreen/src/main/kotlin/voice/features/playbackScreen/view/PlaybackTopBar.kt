@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

@Composable
internal fun PlaybackTopBar(
  showChapters: Boolean,
  onCloseClick: () -> Unit,
  onChaptersClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CloseButton(onClick = onCloseClick)
    Spacer(Modifier.weight(1F))
    if (showChapters) {
      FilledTonalIconButton(
        onClick = onChaptersClick,
        shapes = IconButtonDefaults.shapes(),
        colors = topBarButtonColors(),
      ) {
        Icon(
          imageVector = VoiceIcons.ViewList,
          contentDescription = stringResource(R.string.playback_chapters_title),
        )
      }
    }
  }
}

@Composable
internal fun CloseButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilledTonalIconButton(
    onClick = onClick,
    modifier = modifier,
    shapes = IconButtonDefaults.shapes(),
    colors = topBarButtonColors(),
  ) {
    Icon(
      imageVector = VoiceIcons.KeyboardArrowDown,
      contentDescription = stringResource(R.string.common_action_close),
    )
  }
}

@Composable
private fun topBarButtonColors(): IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
  containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9F),
)
