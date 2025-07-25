package voice.app.scanner

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.FileTypes
import androidx.media3.common.MediaItem
import androidx.media3.common.ParserException
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.exoplayer.MediaExtractorCompat
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import voice.app.scanner.matroska.MatroskaMetaDataExtractor
import voice.app.scanner.matroska.MatroskaParseException
import voice.app.scanner.mp4.Mp4ChapterExtractor
import voice.data.MarkData
import voice.documentfile.CachedDocumentFile
import voice.documentfile.nameWithoutExtension
import voice.logging.core.Logger
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

@Inject
class MediaAnalyzer(
  private val context: Context,
  private val mp4ChapterExtractor: Mp4ChapterExtractor,
  private val matroskaExtractorFactory: MatroskaMetaDataExtractor.Factory,
) {

  // we use a custom MediaSourceFactory because the default one for the
  // retriever also extracts the covers
  private val mediaSourceFactory = DefaultMediaSourceFactory(
    context,
    DefaultExtractorsFactory(),
  )

  suspend fun analyze(file: CachedDocumentFile): Metadata? {
    val builder = Metadata.Builder(file.nameWithoutExtension())
    val duration = parseDuration(file)
      ?: return null
    if (duration <= Duration.ZERO) {
      Logger.w("Duration is zero or negative for file: ${file.uri}")
      return null
    }

    val trackGroups = retrieveMetadata(file.uri)
      ?: return null

    repeat(trackGroups.length) { trackGroupsIndex ->
      val trackGroup = trackGroups[trackGroupsIndex]
      if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
        repeat(trackGroup.length) { formatIndex ->
          val format = trackGroup.getFormat(formatIndex)
          format.metadata?.let { metadata ->
            repeat(metadata.length()) { metadataIndex ->
              when (val entry = metadata.get(metadataIndex)) {
                is TextInformationFrame -> visitText(entry, builder)
                is ChapterFrame -> visitChapter(entry, builder)
                is VorbisComment -> visitVorbis(entry, builder)
                is MdtaMetadataEntry -> visitMdta(entry, builder)
                else -> Logger.d("Unknown metadata entry: $entry")
              }
            }
          }
        }
      }
    }

    val fileType = FileTypes.inferFileTypeFromUri(file.uri)
    val extension = (file.name ?: "").substringAfterLast(delimiter = ".", missingDelimiterValue = "").lowercase()
    if (fileType == FileTypes.MP4 || extension == "mp4" || extension == "m4a" || extension == "m4b") {
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
      }
    } catch (e: MatroskaParseException) {
      Logger.e(e, "Error parsing Matroska metadata")
    }
  }

  private suspend fun parseMp4Chapters(
    file: CachedDocumentFile,
    builder: Metadata.Builder,
  ) {
    val chapters = mp4ChapterExtractor.extractChapters(file.uri)
    builder.chapters += chapters
  }

  private fun visitMdta(
    entry: MdtaMetadataEntry,
    builder: Metadata.Builder,
  ) {
    when (entry.key) {
      "com.apple.quicktime.title" -> {
        builder.title = entry.value.toString(Charsets.UTF_8)
      }

      "com.apple.quicktime.artist" -> {
        builder.artist = entry.value.toString(Charsets.UTF_8)
      }

      "com.apple.quicktime.album" -> {
        builder.album = entry.value.toString(Charsets.UTF_8)
      }
    }
  }

  private fun visitVorbis(
    entry: VorbisComment,
    builder: Metadata.Builder,
  ) {
    val key = entry.key
    val value = entry.value
    when {
      key == "ARTIST" -> builder.artist = value
      key == "ALBUM" -> builder.album = value
      key == "TITLE" -> builder.title = value
      key.startsWith("CHAPTER") -> {
        val withoutPrefix = key.removePrefix("CHAPTER")
        val isName = withoutPrefix.endsWith("NAME")
        if (isName) {
          val index = withoutPrefix.removeSuffix("NAME").toIntOrNull()
          if (index != null) {
            builder.vorbisChapterNames[index] = value
          }
        } else {
          val index = withoutPrefix.toIntOrNull()
          if (index != null) {
            val duration = parseVorbisDuration(value)
            if (duration != null) {
              builder.vorbisChapterStarts[index] = duration.inWholeMilliseconds
            }
          }
        }
      }

      else -> Logger.d("Unknown comment name: ${entry.key}, value: $value")
    }
  }

  private fun visitChapter(
    entry: ChapterFrame,
    builder: Metadata.Builder,
  ) {
    repeat(entry.subFrameCount) { subFrameIndex ->
      val subFrame = entry.getSubFrame(subFrameIndex)
      if (subFrame is TextInformationFrame) {
        builder.chapters.add(MarkData(startMs = entry.startTimeMs.toLong(), name = subFrame.values.first()))
      }
    }
  }

  private fun visitText(
    entry: TextInformationFrame,
    builder: Metadata.Builder,
  ) {
    val value = entry.values.first()
    when (entry.id) {
      "TIT2" -> builder.title = value
      "TPE1" -> builder.artist = value
      "TALB" -> builder.album = value
      "TRCK", "TYER", "TXXX", "TSSE", "TCOM" -> {}
      else -> Logger.v("Unknown frame ID: ${entry.id}, value: $value")
    }
  }

  private suspend fun retrieveMetadata(uri: Uri): TrackGroupArray? {
    return try {
      MetadataRetriever
        .retrieveMetadata(
          mediaSourceFactory,
          MediaItem.fromUri(uri),
        )
        .await()
    } catch (e: Exception) {
      if (e is CancellationException) coroutineContext.ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }

  private suspend fun parseDuration(file: CachedDocumentFile): Duration? {
    val extractor = MediaExtractorCompat(context)

    val prepared = withContext(Dispatchers.IO) {
      try {
        extractor.setDataSource(file.uri, 0)
        true
      } catch (e: IOException) {
        Logger.w(e, "Error extracting duration")
        false
      } catch (e: ParserException) {
        Logger.w(e, "Error extracting duration")
        false
      } catch (e: IllegalArgumentException) {
        Logger.w(e, "Error extracting duration")
        false
      } catch (e: SecurityException) {
        Logger.w(e, "Error extracting duration due to security exception")
        false
      }
    }
    if (!prepared) return null

    repeat(extractor.trackCount) { trackIndex ->
      val format = extractor.getTrackFormat(trackIndex)
      if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
        extractor.selectTrack(trackIndex)
        if (format.containsKey(MediaFormat.KEY_DURATION)) {
          val durationUs = format.getLong(MediaFormat.KEY_DURATION)
          if (durationUs != C.TIME_UNSET) {
            return durationUs.microseconds
          }
        }
      }
    }
    return null
  }
}
