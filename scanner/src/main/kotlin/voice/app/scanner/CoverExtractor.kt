package voice.app.scanner

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.ApicFrame
import kotlinx.coroutines.guava.await
import voice.logging.core.Logger
import java.io.File
import javax.inject.Inject

class CoverExtractor @Inject constructor(private val context: Context) {

  suspend fun extractCover(
    input: Uri,
    outputFile: File,
  ): Boolean {
    val trackGroups = MetadataRetriever
      .retrieveMetadata(
        context,
        MediaItem.fromUri(input),
      )
      .await()
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
}
