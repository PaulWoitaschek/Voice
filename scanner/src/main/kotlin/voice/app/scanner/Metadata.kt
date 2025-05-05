package voice.app.scanner

import voice.data.MarkData
import kotlin.time.Duration

data class Metadata(
  val duration: Long,
  val artist: String?,
  val album: String?,
  val title: String?,
  val fileName: String,
  val chapters: List<MarkData>,
) {

  internal class Builder(val fileName: String) {
    var artist: String? = null
    var album: String? = null
    var title: String? = null
    val chapters = mutableListOf<MarkData>()
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
      )
    }
  }
}
