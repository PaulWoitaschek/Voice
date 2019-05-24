package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.Reusable
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import java.io.File
import javax.inject.Inject

/**
 * Converts books to media sources.
 */
@Reusable
class DataSourceConverter
@Inject constructor(context: Context) {

  private val mediaSourceFactory: ProgressiveMediaSource.Factory

  init {
    val dataSourceFactory = DefaultDataSourceFactory(context, context.packageName)
    val extractorsFactory = DefaultExtractorsFactory()
      .setConstantBitrateSeekingEnabled(true)
    mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
  }

  private fun Chapter.toMediaSource(): MediaSource = toMediaSource(file)

  private fun toMediaSource(file: File): MediaSource {
    return toMediaSource(file.toUri())
  }

  fun toMediaSource(uri: Uri): MediaSource {
    return mediaSourceFactory.createMediaSource(uri)
  }

  /** convert a content to a media source. If the size is > 1 use a concat media source, else a regular */
  fun toMediaSource(content: BookContent): MediaSource {
    return if (content.chapters.size > 1) {
      val allSources = content.chapters.map {
        it.toMediaSource()
      }
      ConcatenatingMediaSource(*allSources.toTypedArray())
    } else content.currentChapter.toMediaSource()
  }
}
