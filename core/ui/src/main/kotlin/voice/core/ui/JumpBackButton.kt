@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

/**
 * "Back to …" after a big jump. The player shows it right after the jump, and the history later on,
 * so both look the same.
 *
 * @param chapterNumber shown when the way back leads into another chapter.
 */
@Composable
fun JumpBackButton(
  chapterNumber: Int?,
  time: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: @Composable () -> Unit = {
    Icon(
      imageVector = VoiceIcons.Undo,
      contentDescription = null,
      modifier = Modifier.size(18.dp),
    )
  },
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = colors.tertiaryContainer,
    contentColor = colors.onTertiaryContainer,
    modifier = modifier.heightIn(min = 32.dp),
  ) {
    Row(
      modifier = Modifier.padding(start = 6.dp, end = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
        icon()
      }
      Spacer(Modifier.width(6.dp))
      Text(
        text = if (chapterNumber != null) {
          stringResource(R.string.playback_jump_back_chapter, chapterNumber, time)
        } else {
          stringResource(R.string.playback_jump_back, time)
        },
        style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
