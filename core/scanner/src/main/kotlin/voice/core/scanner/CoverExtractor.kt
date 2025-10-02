package voice.core.scanner

import android.content.Context
import android.net.Uri
import androidx.media3.common.FileTypes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.ApicFrame
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import voice.core.logging.api.Logger
import voice.core.scanner.matroska.MatroskaCoverExtractor
import java.io.File
import kotlin.coroutines.coroutineContext

@Inject
internal class CoverExtractor(
  private val context: Context,
  private val matroskaCoverExtractor: MatroskaCoverExtractor,
) {

  suspend fun extractCover(
    input: Uri,
    outputFile: File,
  ): Boolean {
    val fileType = FileTypes.inferFileTypeFromUri(input)
    val extension = (input.path ?: "").substringAfterLast(delimiter = ".", missingDelimiterValue = "").lowercase()
    if (fileType == FileTypes.MATROSKA || extension == "mka" || extension == "mkv") {
      return matroskaCoverExtractor.extract(input, outputFile)
    }

    val trackGroups = retrieveMetadata(input)
      ?: return false

    repeat(trackGroups.length) { trackGroupIndex ->
      val trackGroup = trackGroups[trackGroupIndex]
      repeat(trackGroup.length) { formatIndex ->
        val format = trackGroup.getFormat(formatIndex)
        val metadata = format.metadata
        if (metadata != null) {
          repeat(metadata.length()) { metadataIndex ->
            when (val entry = metadata.get(metadataIndex)) {
              is ApicFrame -> {
                Logger.w("Found image frame in ${trackGroup.type}")
                outputFile.outputStream().use { output ->
                  output.write(entry.pictureData)
                }
                return true
              }
              is PictureFrame -> {
                Logger.w("Found image frame in ${trackGroup.type}")
                outputFile.outputStream().use { output ->
                  output.write(entry.pictureData)
                }
                return true
              }
              else -> {
                Logger.v("Unknown metadata entry: $entry")
              }
            }
          }
        }
      }
    }
    return false
  }

  private suspend fun retrieveMetadata(uri: Uri): TrackGroupArray? {
    return try {
      MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
        .build()
        .retrieveTrackGroups()
        .await()
    } catch (e: Exception) {
      if (e is CancellationException) coroutineContext.ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }
}
