package de.ph1b.audiobook.misc

import androidx.documentfile.provider.DocumentFile
import timber.log.Timber
import javax.inject.Inject

/**
 * Analyzes media files for meta data and duration.
 */
class MediaAnalyzer @Inject constructor(
  private val exoPlayerDurationParser: DurationAnalyzer,
  private val metaDataAnalyzer: MetaDataAnalyzer
) {

  suspend fun analyze(file: DocumentFile): Result {
    val metaData = metaDataAnalyzer.parse(file)
    val duration = metaData.duration?.takeIf { it > 0 }
      ?: exoPlayerDurationParser.duration(file.uri) ?: 0
    return if (duration > 0) {
      Result.Success(
        duration = duration,
        chapterName = metaData.chapterName,
        author = metaData.author,
        bookName = metaData.bookName
      )
    } else {
      Timber.d("ExoPlayer failed to parse $file too.")
      Result.Failure
    }
  }

  sealed class Result {
    data class Success(
      val duration: Int,
      val chapterName: String,
      val author: String?,
      val bookName: String?
    ) : Result() {
      init {
        require(duration > 0)
      }
    }

    object Failure : Result()
  }
}
