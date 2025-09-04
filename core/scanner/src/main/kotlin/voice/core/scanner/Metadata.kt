package voice.core.scanner

import voice.core.data.MarkData
import kotlin.time.Duration

internal data class Metadata(
  val duration: Long,
  val artist: String?,
  val album: String?,
  val title: String?,
  val fileName: String,
  val chapters: List<MarkData>,
  val genre: String?,
  val narrator: String?,
  val series: String?,
  val part: String?,
) {

  internal class Builder(val fileName: String) {
    var artist: String? = null
    var album: String? = null
    var title: String? = null
    val chapters = mutableListOf<MarkData>()
    var genre: String? = null
    var narrator: String? = null
    var series: String? = null
    var part: String? = null
    val vorbisChapterNames = mutableMapOf<Int, String>()
    val vorbisChapterStarts = mutableMapOf<Int, Long>()

    fun build(duration: Duration): Metadata {
      vorbisChapterNames.keys.toList()
        .sorted()
        .mapNotNullTo(chapters) { index ->
          val name = vorbisChapterNames[index]
          val start = vorbisChapterStarts[index]
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
        title = title ?: fileName,
        fileName = fileName,
        chapters = chapters,
        genre = genre,
        narrator = narrator,
        series = series,
        part = part,
      )
    }
  }
}
