package voice.app.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException
import javax.inject.Inject

class ChapterTrackExtractor @Inject constructor(private val context: Context) {

  fun extractFromTrackId(
    uri: Uri,
    trackId: Int,
    output: Mp4ChapterExtractor.BoxParseOutput,
  ): List<MarkData> {
    val chapters = mutableListOf<MarkData>()
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    val chunkOffsets = output.chunkOffsets.getOrNull(trackId - 1)
    if (chunkOffsets == null) {
      Logger.w("No chunk offsets found for track ID $trackId")
      return chapters
    }
    val timeScale = output.timeScales.getOrNull(trackId - 1)
    if (timeScale == null) {
      Logger.w("No time scale found for track ID $trackId")
      return chapters
    }
    val durations = output.durations.getOrNull(trackId - 1)
    if (durations == null) {
      Logger.w("No durations found for track ID $trackId")
      return chapters
    }

    if (chunkOffsets.size != durations.size) {
      Logger.w("Chunk offsets and durations size mismatch for track ID $trackId")
      return chapters
    }

    val names = chunkOffsets.map { offset ->
      try {
        dataSource.close()
        dataSource.open(
          DataSpec.Builder()
            .setUri(uri)
            .setPosition(offset)
            .build(),
        )
        val buffer = ParsableByteArray()
        buffer.reset(2)
        dataSource.read(buffer.data, 0, 2)
        val textLength = buffer.readShort().toInt()
        buffer.reset(textLength)
        dataSource.read(buffer.data, 0, textLength)
        val text = buffer.readString(textLength)
        Logger.w("Extracted chapter text: $text")
        text
      } catch (e: IOException) {
        Logger.e(e, "IO error during chapter track extraction")
        return emptyList()
      } finally {
        try {
          dataSource.close()
        } catch (e: IOException) {
          Logger.w(e, "Error closing data source")
        }
      }
    }

    if (names.size != chunkOffsets.size) {
      Logger.w("Mismatch in names size and chunk offsets size for track ID $trackId")
      return chapters
    }
    var position = 0L
    return names
      .mapIndexed { index, chapterName ->
        MarkData(
          startMs = position / timeScale,
          name = chapterName,
        ).also {
          val dai = durations[index]
          position += dai
        }
      }.sorted()
  }
}
