package voice.app.scanner

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.ApicFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import voice.logging.core.Logger
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class CoverExtractor
@Inject constructor(private val context: Context) {

  suspend fun extractCover(
    input: Uri,
    outputFile: File,
  ): Boolean {
    val trackGroups = retrieveMetadata(input)
      ?: return false

    repeat(trackGroups.length) { trackGroupIndex ->
      val trackGroup = trackGroups[trackGroupIndex]
      repeat(trackGroup.length) { formatIndex ->
        val format = trackGroup.getFormat(formatIndex)
        val metadata = format.metadata
        if (metadata != null) {
          repeat(metadata.length()) { metadataIndex ->
            val entry = metadata.get(metadataIndex)
            when (entry) {
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
      MetadataRetriever
        .retrieveMetadata(
          context,
          MediaItem.fromUri(uri),
        )
        .await()
    } catch (e: Exception) {
      if (e is CancellationException) coroutineContext.ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }
}
