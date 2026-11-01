package voice.core.scanner

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.FileTypes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.inspector.MetadataRetriever
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.nameWithoutExtension
import voice.core.logging.api.Logger
import voice.core.scanner.matroska.MatroskaMetaDataExtractor
import voice.core.scanner.matroska.MatroskaParseException
import voice.core.scanner.mp4.Mp4ChapterExtractor

@Inject
internal class MediaAnalyzer(
  private val context: Context,
  private val mp4ChapterExtractor: Mp4ChapterExtractor,
  private val matroskaExtractorFactory: MatroskaMetaDataExtractor.Factory,
) {

  suspend fun analyze(file: CachedDocumentFile): Metadata? {
    val builder = Metadata.Builder(file.nameWithoutExtension())
    val (duration, trackGroups) = retrieveDurationAndTrackGroups(file.uri) ?: return null
    val extension =
      (file.name ?: "").substringAfterLast(delimiter = ".", missingDelimiterValue = "").lowercase()
    if (duration <= Duration.ZERO) {
      Logger.w("Duration is zero or negative for a .$extension file")
      return null
    }

    repeat(trackGroups.length) { trackGroupsIndex ->
      val trackGroup = trackGroups[trackGroupsIndex]
      if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
        repeat(trackGroup.length) { formatIndex ->
          val format = trackGroup.getFormat(formatIndex)
          format.metadata?.let { metadata ->
            repeat(metadata.length()) { metadataIndex ->
              builder.read(metadata.get(metadataIndex))
            }
          }
        }
      }
    }

    val fileType = FileTypes.inferFileTypeFromUri(file.uri)
    if (
      fileType == FileTypes.MP4 || extension == "mp4" || extension == "m4a" || extension == "m4b"
    ) {
      parseMp4Chapters(file, builder)
    }
    if (fileType == FileTypes.MATROSKA || extension == "mka" || extension == "mkv") {
      parseMatroskaMetaData(file, builder)
    }

    return builder.build(duration)
  }

  private fun parseMatroskaMetaData(
    file: CachedDocumentFile,
    builder: Metadata.Builder,
  ) {
    try {
      matroskaExtractorFactory.create(file.uri).use { extractor ->
        val mediaInfo = extractor.readMediaInfo()
        builder.chapters.addAll(mediaInfo.chapters)
        builder.artist = builder.artist ?: mediaInfo.artist
        builder.album = builder.album ?: mediaInfo.album
        builder.title = builder.title ?: mediaInfo.title
        mediaInfo.tags.forEach { (name, value) -> builder.tag(name, value) }
      }
    } catch (e: MatroskaParseException) {
      Logger.w(e, "Error parsing Matroska metadata")
    }
  }

  private suspend fun parseMp4Chapters(
    file: CachedDocumentFile,
    builder: Metadata.Builder,
  ) {
    val chapters = mp4ChapterExtractor.extractChapters(file.uri)
    builder.chapters += chapters
  }

  // Keep the retriever's default MediaSourceFactory: its extractor flags are tuned for metadata
  // retrieval, e.g. skipping the MP4 sample table and estimating the duration of raw AAC files.
  // A single retriever prepares the file once for both the duration and the track groups.
  private suspend fun retrieveDurationAndTrackGroups(uri: Uri): Pair<Duration, TrackGroupArray>? {
    return try {
      MetadataRetriever.Builder(context, MediaItem.fromUri(uri)).build().use {
        it.retrieveDurationUs().await().microseconds to it.retrieveTrackGroups().await()
      }
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }
}
