package voice.playback.player

import androidx.media3.common.MediaItem
import voice.data.Book
import voice.data.Chapter
import voice.data.toUri

internal fun Book.toMediaItems(): List<MediaItem> = chapters.map { it.toMediaItem() }

private fun Chapter.toMediaItem(): MediaItem {
  return MediaItem.Builder()
    .setUri(id.toUri())
    .build()
}
