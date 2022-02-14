package de.ph1b.audiobook.playback.player

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.toUri
import javax.inject.Inject

class DataSourceConverter
@Inject constructor(
  private val mediaSourceFactory: MediaSourceFactory
) {

  fun toMediaSource(content: Book): MediaSource {
    return if (content.chapters.size > 1) {
      val allSources = content.chapters.map {
        it.toMediaSource()
      }
      ConcatenatingMediaSource(*allSources.toTypedArray())
    } else {
      content.currentChapter.toMediaSource()
    }
  }

  private fun Chapter.toMediaSource(): MediaSource {
    val item = MediaItem.Builder()
      .setUri(id.toUri())
      .build()
    return mediaSourceFactory.createMediaSource(item)
  }
}
