package voice.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

private const val THUMBNAIL_SIZE_PX = 480

private fun thumbnailMemoryCacheKey(cover: String): String = "cover-thumbnail:$cover"

/**
 * Image model for small cover renditions (lists, cards). All of them share one memory cache entry
 * per cover, which [rememberFullSizeCoverRequest] shows as its placeholder.
 */
@Composable
fun rememberCoverThumbnailRequest(cover: String?): ImageRequest {
  val context = LocalContext.current
  return remember(cover, context) {
    ImageRequest.Builder(context)
      .data(cover)
      .size(THUMBNAIL_SIZE_PX)
      .memoryCacheKey(cover?.let(::thumbnailMemoryCacheKey))
      // only fades when loaded from disk, covers already in memory show right away
      .crossfade(true)
      .build()
  }
}

/**
 * Image model for a large cover. While it loads, the already decoded thumbnail is shown, so a shared
 * element transition from a list never flashes the empty placeholder.
 */
@Composable
fun rememberFullSizeCoverRequest(cover: String?): ImageRequest {
  val context = LocalContext.current
  return remember(cover, context) {
    ImageRequest.Builder(context)
      .data(cover)
      .placeholderMemoryCacheKey(cover?.let(::thumbnailMemoryCacheKey))
      .placeholder(R.drawable.album_art)
      .build()
  }
}
