package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.Reusable
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import javax.inject.Inject

/**
 * Converts books to media sources.
 *
 * @author Paul Woitaschek
 */
@Reusable class DataSourceConverter @Inject constructor(context: Context) {

  private val dataSourceFactory = DefaultDataSourceFactory(context, context.packageName)
  private val extractorsFactory = DefaultExtractorsFactory()

  private fun Chapter.toMediaSource() = ExtractorMediaSource(Uri.fromFile(file), dataSourceFactory, extractorsFactory, null, null)

  /** convert a book to a media source. If the size is > 1 use a concat media source, else a regular */
  fun toMediaSource(book: Book) = if (book.chapters.size > 1) {
    val allSources = book.chapters.map {
      it.toMediaSource()
    }
    ConcatenatingMediaSource(*allSources.toTypedArray())
  } else book.currentChapter().toMediaSource()
}