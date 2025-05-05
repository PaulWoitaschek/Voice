package voice.app.scanner

import android.content.Context
import android.media.MediaFormat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.ParserException
import androidx.media3.common.TrackGroup
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.exoplayer.MediaExtractorCompat
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
    val duration = parseDuration(file)
      ?: return null

    val chapters = mutableListOf<MarkData>()

    var artist: String? = null
    var album: String? = null
    var title: String? = null

    val trackGroups = try {
      MetadataRetriever
        .retrieveMetadata(
          DefaultMediaSourceFactory(
            context,
            DefaultExtractorsFactory(),
          ),
          MediaItem.fromUri(file.uri),
        )
        .await()
    } catch (e: Exception) {
      if (e is CancellationException) coroutineContext.ensureActive()
      Logger.w(e, "Error retrieving metadata")
      return null
    }

    val vorbisChapterNames = mutableMapOf<Int, String>()
    val vorbisChapterStarts = mutableMapOf<Int, Long>()
    for (i in 0 until trackGroups.length) {
      val trackGroup: TrackGroup = trackGroups[i]
      if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
        for (j in 0 until trackGroup.length) {
          val format = trackGroup.getFormat(j)
          format.metadata?.let { metadata ->
            for (k in 0 until metadata.length()) {
              val entry = metadata.get(k)
              when (entry) {
                is TextInformationFrame -> {
                  val value = entry.values.first()
                  when (entry.id) {
                    "TIT2" -> title = value
                    "TPE1" -> artist = value
                    "TALB" -> album = value
                    "TRCK", "TYER", "TXXX", "TSSE", "TCOM" -> {
                    }
                    else -> Logger.v("Unknown frame ID: ${entry.id}, value: $value")
                  }
                }
                is ChapterFrame -> {
                  for (subFrameIndex in 0 until entry.subFrameCount) {
                    val subFrame = entry.getSubFrame(subFrameIndex)
                    if (subFrame is TextInformationFrame) {
                      chapters.add(MarkData(startMs = entry.startTimeMs.toLong(), name = subFrame.values.first()))
                    }
                  }
                }
                is VorbisComment -> {
                  val key = entry.key
                  val value = entry.value
                  when {
                    key == "ARTIST" -> artist = value
                    key == "ALBUM" -> album = value
                    key == "TITLE" -> title = value
                    key.startsWith("CHAPTER") -> {
                      val withoutPrefix = key.removePrefix("CHAPTER")
                      val isName = withoutPrefix.endsWith("NAME")
                      if (isName) {
                        val index = withoutPrefix.removeSuffix("NAME").toIntOrNull()
                        if (index != null) {
                          vorbisChapterNames[index] = value
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
                              vorbisChapterStarts[index] = start
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
                is MdtaMetadataEntry -> {
                  when (entry.key) {
                    "com.apple.quicktime.title" -> {
                      title = entry.value.toString(Charsets.UTF_8)
                    }
                    "com.apple.quicktime.artist" -> {
                      artist = entry.value.toString(Charsets.UTF_8)
                    }
                    "com.apple.quicktime.album" -> {
                      album = entry.value.toString(Charsets.UTF_8)
                    }
                  }
                }
                else -> {
                  Logger.d("Unknown metadata entry: $entry")
                }
              }
            }
          }
        }
      }
    }

    (vorbisChapterNames.keys + vorbisChapterStarts.keys)
      .distinct()
      .sorted()
      .mapNotNullTo(chapters) {
        val name = vorbisChapterNames[it]
        val start = vorbisChapterStarts[it]
        if (name != null && start != null) {
          MarkData(startMs = start, name = name)
        } else {
          null
        }
      }

    return Metadata(
      duration = duration.inWholeMilliseconds,
      artist = artist,
      album = album,
      title = title ?: file.nameWithoutExtension(),
      fileName = file.nameWithoutExtension(),
      chapters = chapters,
    )
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

    for (i in 0 until extractor.trackCount) {
      val format = extractor.getTrackFormat(i)
      if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
        extractor.selectTrack(i)
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

  data class Metadata(
    val duration: Long,
    val artist: String?,
    val album: String?,
    val title: String?,
    val fileName: String,
    val chapters: List<MarkData>,
  )
}
