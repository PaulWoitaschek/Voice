package de.ph1b.audiobook.misc

import kotlinx.coroutines.CoroutineScope
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

  suspend fun CoroutineScope.analyze(file: File): Result {
    val metaData = metaDataAnalyzer.parse(file)
    val metaDataDuration = metaData.duration ?: 0
    return if (metaDataDuration > 0) {
      Result.Success(
        metaDataDuration,
        metaData.chapterName,
        metaData.author,
        metaData.bookName
      )
    } else {
      Timber.d("Invalid duration from meta data for $file. Try exoPlayer")
      val exoDuration = with(exoPlayerDurationParser) { duration(file) } ?: -1
      if (exoDuration > 0) {
        Result.Success(exoDuration, metaData.chapterName, metaData.author, metaData.bookName)
      } else {
        Timber.d("ExoPlayer failed to parse $file too.")
        Result.Failure
      }
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
