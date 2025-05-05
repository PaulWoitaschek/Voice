package voice.app.scanner

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.ParserException
import androidx.media3.common.TrackGroup
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.exoplayer.MediaExtractorCompat
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import voice.data.MarkData
import voice.documentfile.CachedDocumentFile
import voice.documentfile.nameWithoutExtension
import voice.logging.core.Logger
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class MediaAnalyzer
@Inject constructor(private val context: Context) {

  suspend fun analyze(file: CachedDocumentFile): Metadata? {
    val builder = Metadata.Builder(file.nameWithoutExtension())
    val duration = parseDuration(file)
      ?: return null

    val trackGroups = retrieveMetadata(file.uri)
      ?: return null

    for (i in 0 until trackGroups.length) {
      val trackGroup: TrackGroup = trackGroups[i]
      if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
        for (j in 0 until trackGroup.length) {
          val format = trackGroup.getFormat(j)
          format.metadata?.let { metadata ->
            for (k in 0 until metadata.length()) {
              val entry = metadata.get(k)
              when (entry) {
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

    return builder.build(duration)
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
            val split = value.split(":")
            if (split.size == 3) {
              val hour = split[0].toLongOrNull()
              val minute = split[1].toLongOrNull()
              val seconds = split[2].toDoubleOrNull()
              if (hour != null && minute != null && seconds != null) {
                val start = TimeUnit.HOURS.toMillis(hour) +
                  TimeUnit.MINUTES.toMillis(minute) +
                  (seconds * 60).roundToLong()
                builder.vorbisChapterStarts[index] = start
              } else {
                Logger.w("Invalid vorbis chapter format: $value")
              }
            } else {
              Logger.w("Invalid vorbis chapter format: $value")
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
    for (subFrameIndex in 0 until entry.subFrameCount) {
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
      "TRCK", "TYER", "TXXX", "TSSE", "TCOM" -> {
      }
      else -> Logger.v("Unknown frame ID: ${entry.id}, value: $value")
    }
  }

  private suspend fun retrieveMetadata(uri: Uri): TrackGroupArray? {
    return try {
      MetadataRetriever
        .retrieveMetadata(
          DefaultMediaSourceFactory(
            context,
            DefaultExtractorsFactory(),
          ),
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
      }
    }
    if (!prepared) return null

    for (trackIndex in 0 until extractor.trackCount) {
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
