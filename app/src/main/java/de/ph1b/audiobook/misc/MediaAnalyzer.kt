package de.ph1b.audiobook.misc

import androidx.core.net.toUri
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Analyzes media files for meta data and duration.
 */
class MediaAnalyzer @Inject constructor(
  private val exoPlayerDurationParser: DurationAnalyzer,
  private val metaDataAnalyzer: MetaDataAnalyzer
) {

  suspend fun analyze(file: File): Result {
    val metaData = metaDataAnalyzer.parse(file)
    val duration = metaData.duration?.takeIf { it > 0L }
      ?: exoPlayerDurationParser.duration(file.toUri()) ?: 0L
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
      val duration: Long,
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
