package voice.app.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException

suspend fun extractChapterCuesDirectly(
  context: Context,
  uri: Uri,
  targetMp4TrackId: Int,
): List<MarkData> {
  val collectedCuesWithStartTimes = mutableListOf<MarkData>()
  val dataSource: DataSource = DefaultDataSource.Factory(context).createDataSource()

  try {
    withContext(Dispatchers.IO) {
      dataSource.open(DataSpec(uri))
    }
    var extractorInput = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())

    val extractor = Mp4Extractor(DefaultSubtitleParserFactory())
    val extractorOutput = ChapterExtractorOutput(targetMp4TrackId, collectedCuesWithStartTimes)
    extractor.init(extractorOutput)

    val positionHolder = PositionHolder()
    while (true) {
      val result = withContext(Dispatchers.IO) {
        extractor.read(extractorInput, positionHolder)
      }
      when (result) {
        Extractor.RESULT_CONTINUE -> {
          // Continue reading
        }
        Extractor.RESULT_SEEK -> {
          dataSource.close()
          var length =
            dataSource.open(
              DataSpec.Builder()
                .setUri(uri)
                .setPosition(positionHolder.position)
                .build(),
            )
          if (length != C.LENGTH_UNSET.toLong()) {
            length += positionHolder.position
          }
          println("seek to ${positionHolder.position}")
          extractorInput = DefaultExtractorInput(dataSource, positionHolder.position, length)
        }
        Extractor.RESULT_END_OF_INPUT -> {
          break
        }
      }
    }
  } catch (e: IOException) {
    Logger.e(e, "IOException during direct subtitle extraction")
  } catch (e: Exception) {
    Logger.e(e, "Exception during direct subtitle extraction")
  } finally {
    try {
      dataSource.close()
    } catch (e: IOException) {
      Logger.w(e, "Error closing datasource")
    }
  }

  return collectedCuesWithStartTimes.sorted()
}
