package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.core.data.BookId
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.core.ui.sharedCoverElementModifier
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
) {
  Box(modifier = modifier) {
    AsyncImage(
      modifier = Modifier
        .fillMaxSize()
        .sharedCoverElementModifier(bookId)
        .clip(shape),
      model = rememberCoverThumbnailRequest(cover),
      // a soft fill the cover fades in on, instead of the dark default art flashing up
      placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
      error = painterResource(id = UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )
    if (finished) {
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
  }
}

/** Shapes for a group of connected list items: big outer corners, small inner ones. */
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
