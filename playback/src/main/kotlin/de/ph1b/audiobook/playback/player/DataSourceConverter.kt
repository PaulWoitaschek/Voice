package de.ph1b.audiobook.playback.player

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.Chapter2
import javax.inject.Inject

class DataSourceConverter
@Inject constructor(context: Context) {

  private val mediaSourceFactory: ProgressiveMediaSource.Factory

  init {
    val dataSourceFactory = DefaultDataSourceFactory(context, context.packageName)
    val extractorsFactory = DefaultExtractorsFactory()
      .setConstantBitrateSeekingEnabled(true)
    mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
  }

  fun toMediaSource(content: BookContent): MediaSource {
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
      .setUri(uri)
      .build()
    return mediaSourceFactory.createMediaSource(item)
  }


  fun toMediaSource(content: Book2): MediaSource {
    return if (content.chapters.size > 1) {
      val allSources = content.chapters.map {
        it.toMediaSource()
      }
      ConcatenatingMediaSource(*allSources.toTypedArray())
    } else {
      content.currentChapter.toMediaSource()
    }
  }

  private fun Chapter2.toMediaSource(): MediaSource {
    val item = MediaItem.Builder()
      .setUri(uri)
      .build()
    return mediaSourceFactory.createMediaSource(item)
  }

}
