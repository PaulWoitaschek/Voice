package voice.core.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorInput
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.data.MarkData
import voice.core.logging.api.Logger
import java.io.IOException

@Inject
internal class Mp4ChapterExtractor(
  private val context: Context,
  private val boxParser: Mp4BoxParser,
  private val chapterTrackProcessor: ChapterTrackProcessor,
) {

  suspend fun extractChapters(uri: Uri): List<MarkData> = withContext(Dispatchers.IO) {
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    try {
      dataSource.open(DataSpec(uri))
      val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
      val topLevelResult = boxParser(input)
      val trackId = topLevelResult.chapterTrackId
      when {
        topLevelResult.chplChapters.isNotEmpty() -> {
          topLevelResult.chplChapters
        }
        trackId != null -> {
          chapterTrackProcessor(uri, dataSource, trackId, topLevelResult)
        }
        else -> emptyList()
      }
    } catch (e: Exception) {
      Logger.w(e, "Failed to extract MP4 chapters")
      emptyList()
    } finally {
      try {
        dataSource.close()
      } catch (e: IOException) {
        Logger.w(e, "Error closing data source")
      }
    }
  }
}
