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
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException
import javax.inject.Inject

class ChapterTrackExtractor @Inject constructor(private val context: Context) {

  fun extractFromTrackId(
    uri: Uri,
    trackId: Int,
  ): List<MarkData> {
    val chapters = mutableListOf<MarkData>()
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    try {
      dataSource.open(DataSpec(uri))
      processTrack(dataSource, uri, trackId, chapters)
    } catch (e: IOException) {
      Logger.e(e, "IO error during chapter track extraction")
    } finally {
      try {
        dataSource.close()
      } catch (e: IOException) {
        Logger.w(e, "Error closing data source")
      }
    }

    return chapters.sorted()
  }

  private fun processTrack(
    dataSource: DataSource,
    uri: Uri,
    trackId: Int,
    outputChapters: MutableList<MarkData>,
  ) {
    var extractorInput = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
    val positionHolder = PositionHolder()

    val extractor = Mp4Extractor(DefaultSubtitleParserFactory())
    val extractorOutput = ChapterTrackOutput(trackId, outputChapters)
    extractor.init(extractorOutput)

    while (true) {
      val result = extractor.read(extractorInput, positionHolder)
      when (result) {
        Extractor.RESULT_CONTINUE -> {
          // Continue reading
        }
        Extractor.RESULT_SEEK -> {
          dataSource.close()
          var length = dataSource.open(
            DataSpec.Builder()
              .setUri(uri)
              .setPosition(positionHolder.position)
              .build(),
          )

          if (length != C.LENGTH_UNSET.toLong()) {
            length += positionHolder.position
          }

          Logger.d("Seeking to position ${positionHolder.position}")
          extractorInput = DefaultExtractorInput(dataSource, positionHolder.position, length)
        }
        Extractor.RESULT_END_OF_INPUT -> break
      }
    }
  }
}
