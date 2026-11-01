package voice.core.scanner

import kotlin.time.Duration
import voice.core.data.MarkData

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
    val vorbisChapterNames = mutableMapOf<Int, String>()
    val vorbisChapterStarts = mutableMapOf<Int, Long>()
    private val tags = mutableMapOf<String, String>()

    fun tag(
      name: String,
      value: String,
    ) {
      val key = name.uppercase()
      val trimmed = value.trim()
      if (trimmed.isNotEmpty() && key !in tags) {
        tags[key] = trimmed
      }
    }

    fun build(duration: Duration): Metadata {
      vorbisChapterNames.keys.toList().sorted().mapNotNullTo(chapters) { index ->
        val name = vorbisChapterNames[index]
        val start = vorbisChapterStarts[index]
        if (name != null && start != null) {
          MarkData(startMs = start, name = name)
        } else {
          null
        }
      }

      val (series, part) = seriesAndPart()

      return Metadata(
        duration = duration.inWholeMilliseconds,
        artist = artist,
        album = album,
        title = title,
        fileName = fileName,
        chapters = chapters,
        genre = genre,
        narrator = narrator,
        series = series?.firstOfSeveral(),
        part = part?.firstOfSeveral(),
      )
    }

    private fun seriesAndPart(): Pair<String?, String?> {
      val tagged = seriesAndPartTags.firstNotNullOfOrNull { (seriesTag, partTag) ->
        val series = tags[seriesTag] ?: return@firstNotNullOfOrNull null
        val part =
          tags[partTag]
            // Mp3tag writes the movement with its count, like 3/7
            ?.let { if (partTag == "MVIN") it.substringBefore('/') else it }
            ?: tags["PART"]
            ?: tags["TXXX:PART"]
        series to part
      }
      return tagged
        ?: groupingTags.firstNotNullOfOrNull { tags[it]?.let(::parseGrouping) }
        ?: (null to null)
    }
  }
}

// the series tags Audiobookshelf reads, in its order, each with its part
private val seriesAndPartTags =
  listOf(
    "SERIES" to "SERIES-PART",
    "SHOW" to "EPISODE_ID",
    "MVNM" to "MVIN",
    "MOVEMENTNAME" to "MOVEMENT",
  )

// iTunes moved the grouping of MP3 files from TIT1 to GRP1, and TIT1 became the work
private val groupingTags = listOf("GROUPING", "GRP1", "TIT1")

private fun String.firstOfSeveral(): String? = substringBefore(';').trim().ifEmpty { null }

private val seriesWithPart = Regex("""^(.+) #([^#\s]+)$""")

// Audiobookshelf writes a book's series into the grouping, like "Cosmere; The Stormlight Archive
// #2". A grouping
// without a part is often a genre or "Audiobook", so it doesn't count.
private fun parseGrouping(grouping: String): Pair<String, String>? {
  val match =
    grouping.split(';').firstNotNullOfOrNull { seriesWithPart.find(it.trim()) } ?: return null
  val (series, part) = match.destructured
  return series.trim() to part
}
