package de.ph1b.audiobook.misc

import io.reactivex.Single
import timber.log.Timber
import java.io.File

/**
 * Analyzes media files for meta data and duration.
 */
class MediaAnalyzer(
  private val exoPlayerDurationParser: DurationAnalyzer,
  private val metaDataAnalyzer: MetaDataAnalyzer
) {

  fun analyze(file: File) = Single.fromCallable { metaDataAnalyzer.parse(file) }
    .flatMap { metaData ->
      val metaDataDuration = metaData.duration ?: 0
      if (metaDataDuration > 0) {
        Single.just(
          Result.Success(
            metaDataDuration,
            metaData.chapterName,
            metaData.author,
            metaData.bookName
          )
        )
      } else {
        exoPlayerDurationParser.duration(file)
          .map { exoDuration ->
            Timber.d("Invalid duration from meta data for $file. Try exoPlayer")
            if (exoDuration > 0) {
              Result.Success(exoDuration, metaData.chapterName, metaData.author, metaData.bookName)
            } else {
              Timber.d("ExoPlayer failed to parse $file too.")
              Result.Failure
            }
          }
      }
    }!!

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

