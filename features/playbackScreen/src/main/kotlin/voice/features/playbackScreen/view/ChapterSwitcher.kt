@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

/**
 * Previous / next chapter buttons around a pill naming the current chapter. The name slides in
 * from the direction you are moving through the book. Tapping the pill opens the chapter list.
 */
@Composable
internal fun ChapterSwitcher(
  chapterName: String?,
  chapterNumber: Int,
  chapterCount: Int,
  onSkipToPrevious: () -> Unit,
  onSkipToNext: () -> Unit,
  onChapterClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    FilledTonalIconButton(
      onClick = onSkipToPrevious,
      shapes = IconButtonDefaults.shapes(),
      modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
    ) {
      Icon(
        imageVector = VoiceIcons.SkipPrevious,
        contentDescription = stringResource(R.string.playback_chapter_previous),
      )
    }
    Surface(
      onClick = onChapterClick,
      modifier = Modifier
        .weight(1F)
        .heightIn(min = 56.dp),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85F),
    ) {
      Row(
        modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AnimatedContent(
          modifier = Modifier.weight(1F),
          targetState = ChapterLabel(chapterNumber, chapterName),
          transitionSpec = {
            val forward = targetState.number >= initialState.number
            val enter = slideInHorizontally { width -> if (forward) width / 2 else -width / 2 } + fadeIn()
            val exit = slideOutHorizontally { width -> if (forward) -width / 2 else width / 2 } + fadeOut()
            (enter togetherWith exit).using(SizeTransform(clip = false))
          },
          contentAlignment = Alignment.CenterStart,
          label = "chapterLabel",
        ) { label ->
          Column {
            Text(
              text = stringResource(R.string.playback_chapter_counter, label.number, chapterCount),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            if (label.name != null) {
              Text(
                modifier = Modifier.basicMarquee(),
                text = label.name,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1,
              )
            }
          }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
          imageVector = VoiceIcons.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    FilledTonalIconButton(
      onClick = onSkipToNext,
      shapes = IconButtonDefaults.shapes(),
      modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
    ) {
      Icon(
        imageVector = VoiceIcons.SkipNext,
        contentDescription = stringResource(R.string.playback_chapter_next),
      )
    }
  }
}

private data class ChapterLabel(
  val number: Int,
  val name: String?,
)
