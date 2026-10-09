package voice.features.bookOverview.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.core.audiobookshelf.download.WaitingFor
import voice.core.data.BookId
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.core.ui.sharedCoverElementModifier
import voice.features.bookOverview.overview.DownloadBadge
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

/** Percentage based so covers keep their look while shared element transitions resize them. */
internal val CoverShape = RoundedCornerShape(percent = 12)

@Composable
internal fun BookCover(
  bookId: BookId,
  cover: String?,
  modifier: Modifier = Modifier,
  shape: Shape = CoverShape,
  finished: Boolean = false,
  unavailable: Boolean = false,
  download: DownloadBadge? = null,
) {
  Box(modifier = modifier) {
    AsyncImage(
      modifier = Modifier
        .fillMaxSize()
        .sharedCoverElementModifier(bookId)
        .clip(shape)
        .alpha(if (unavailable) 0.4F else 1F),
      model = rememberCoverThumbnailRequest(cover),
      // a soft fill the cover fades in on, instead of the dark default art flashing up
      placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
      error = painterResource(id = UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )
    // one badge per cover, what a download or the connection says matters more than being finished
    if (finished && download == null && !unavailable) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 4.dp, y = 4.dp)
          .size(24.dp)
          .background(MaterialTheme.colorScheme.surface, CircleShape)
          .padding(2.dp)
          .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          modifier = Modifier.size(14.dp),
          imageVector = VoiceIcons.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
    if (download != null) {
      DownloadBadgeIcon(
        badge = download,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 4.dp, y = 4.dp),
      )
    } else if (unavailable) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 4.dp, y = 4.dp)
          .size(24.dp)
          .background(MaterialTheme.colorScheme.surface, CircleShape)
          .padding(2.dp)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          modifier = Modifier.size(14.dp),
          imageVector = VoiceIcons.CloudOff,
          contentDescription = stringResource(StringsR.string.library_book_offline),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun DownloadBadgeIcon(
  badge: DownloadBadge,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  when (badge) {
    is DownloadBadge.Running -> {
      val waiting = badge.waitingFor != null
      val description = when (badge.waitingFor) {
        WaitingFor.Wifi -> stringResource(StringsR.string.book_download_waiting_for_wifi)
        WaitingFor.Connection -> stringResource(StringsR.string.book_download_waiting_for_connection)
        null -> stringResource(StringsR.string.library_book_downloading)
      }
      val progress by animateFloatAsState(badge.progress, label = "downloadBadgeProgress")
      Box(
        modifier = modifier
          .size(24.dp)
          .background(colors.surface, CircleShape)
          .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          progress = { progress },
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = if (waiting) colors.onSurfaceVariant else colors.primary,
        )
        Icon(
          modifier = Modifier.size(12.dp),
          // a clock, so a waiting download doesn't look stuck
          imageVector = if (waiting) VoiceIcons.Schedule else VoiceIcons.Download,
          contentDescription = null,
          tint = if (waiting) colors.onSurfaceVariant else colors.primary,
        )
      }
    }
    DownloadBadge.Failed -> Box(
      modifier = modifier
        .size(24.dp)
        .background(colors.surface, CircleShape)
        .padding(2.dp)
        .background(colors.errorContainer, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        modifier = Modifier.size(14.dp),
        imageVector = VoiceIcons.Error,
        contentDescription = stringResource(StringsR.string.book_download_failed),
        tint = colors.onErrorContainer,
      )
    }
  }
}

internal fun segmentedShape(
  index: Int,
  count: Int,
): Shape {
  val large = 24.dp
  val small = 6.dp
  return when {
    count == 1 -> RoundedCornerShape(large)
    index == 0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
    index == count - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
    else -> RoundedCornerShape(small)
  }
}
