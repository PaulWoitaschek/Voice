package voice.playbackScreen.views

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberImagePainter
import voice.playbackScreen.BookPlayCover

@Composable
internal fun BookPlayCover(modifier: Modifier, cover: BookPlayCover) {
  val context = LocalContext.current
  val (imageData, placeholder) = remember(cover) {
    cover.file(context) to cover.placeholder(context)
  }
  Image(
    modifier = modifier,
    contentScale = ContentScale.Crop,
    painter = rememberImagePainter(
      data = imageData,
      builder = {
        error(placeholder)
      }
    ),
    contentDescription = null
  )
}
