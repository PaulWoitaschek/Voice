package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
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

  private val mediaSourceFactory: ExtractorMediaSource.Factory

  init {
    val dataSourceFactory = DefaultDataSourceFactory(context, context.packageName)
    val extractorsFactory = DefaultExtractorsFactory()
      .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
      .setTsExtractorFlags(TsExtractor.MODE_SINGLE_PMT)
    mediaSourceFactory = ExtractorMediaSource.Factory(dataSourceFactory)
      .setExtractorsFactory(extractorsFactory)
  }


  private fun Chapter.toMediaSource() = toMediaSource(file)

  fun toMediaSource(file: File): ExtractorMediaSource =
    mediaSourceFactory.createMediaSource(Uri.fromFile(file))

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
