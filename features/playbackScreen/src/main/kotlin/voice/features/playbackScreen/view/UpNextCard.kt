@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.bookText
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.playbackScreen.BookPlayViewState

@Composable
internal fun UpNextCard(
  upNext: BookPlayViewState.UpNextViewState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85F),
  ) {
    if (LocalDensity.current.fontScale >= 1.5F) {
      Column(Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Cover(upNext)
          Spacer(Modifier.weight(1F))
          PlayCircle()
        }
        Spacer(Modifier.height(8.dp))
        Words(upNext)
      }
    } else {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Cover(upNext)
        Spacer(Modifier.width(12.dp))
        Words(upNext, Modifier.weight(1F))
        Spacer(Modifier.width(12.dp))
        PlayCircle()
      }
    }
  }
}

@Composable
private fun Cover(upNext: BookPlayViewState.UpNextViewState) {
  val colors = MaterialTheme.colorScheme
  Box {
    AsyncImage(
      model = rememberCoverThumbnailRequest(upNext.cover),
      placeholder = ColorPainter(colors.surfaceContainerHighest),
      error = painterResource(UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
      modifier =
        Modifier.size(56.dp)
          .clip(RoundedCornerShape(percent = 28))
          .alpha(if (upNext.unavailable) 0.4F else 1F),
    )
    if (upNext.unavailable) {
      Icon(
        painter = painterResource(UiR.drawable.ic_cloud_off),
        contentDescription = stringResource(StringsR.string.library_book_offline),
        tint = colors.onSurfaceVariant,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .offset(x = 4.dp, y = 4.dp)
            .size(24.dp)
            .background(colors.surface, CircleShape)
            .padding(2.dp)
            .background(colors.surfaceContainerHighest, CircleShape)
            .padding(3.dp),
      )
    }
  }
}

@Composable
private fun Words(
  upNext: BookPlayViewState.UpNextViewState,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Text(
      text = stringResource(StringsR.string.series_up_next),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = upNext.title,
      style =
        MaterialTheme.typography.titleMediumEmphasized.bookText().copy(hyphens = Hyphens.Auto),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (upNext.series != null) {
      Text(
        text = upNext.series,
        style = MaterialTheme.typography.bodyMedium.bookText(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        // the part at the end says which book is next
        overflow = TextOverflow.MiddleEllipsis,
      )
    }
  }
}

@Composable
private fun PlayCircle() {
  Box(
    modifier =
      Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(UiR.drawable.ic_play_arrow_rounded_filled),
      contentDescription = stringResource(StringsR.string.playback_action_play),
      tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
  }
}
