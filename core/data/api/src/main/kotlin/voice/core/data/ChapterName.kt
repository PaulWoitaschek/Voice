package voice.core.data

private val audioExtension =
  Regex("""\.(mp3|m4a|m4b|aac|ogg|oga|opus|flac|wav|wma|mka|mp4)$""", RegexOption.IGNORE_CASE)

private const val GENERIC_WORDS =
  "chapter|track|part|disc|cd|kapitel|teil|chapitre|partie|capitulo|capítulo|capitolo|parte|" +
    "hoofdstuk|rozdział|kapitola|bölüm|глава|часть|розділ"

private val genericName =
  Regex(
    """^(?:(?:$GENERIC_WORDS)[\s._:#-]*(\d*)|(\d+)[\s._:-]*(?:$GENERIC_WORDS))$""",
    RegexOption.IGNORE_CASE,
  )

// The leading zero or the separator tells a track number apart from a title like "1 Mose" or
// "5-Minute Stories". A digit after the separator, like in "1-01", means disc and track.
private val trackNumber = Regex("""^(?:0+(\d+)[\s._:)\]-]++|(\d+)(?:_+|\s*[:)\]-]+\s+))(?!\d)""")

/**
 * Only removes what Voice shows anyway, so that no information gets lost. A number that differs
 * from the chapter number, like "Chapter 1" after a prologue, stays.
 */
public fun ChapterMark.displayName(
  number: Int,
  chapterCount: Int,
): String? {
  if (chapterCount <= 1) return null
  val cleaned = name?.trim()?.replace(audioExtension, "")?.trim() ?: return null
  if (cleaned.none { it.isLetter() }) return null
  val withoutNumber =
    trackNumber
      .find(cleaned)
      ?.takeIf { it.number() == number }
      ?.let { cleaned.substring(it.range.last + 1) } ?: cleaned
  val generic = genericName.matchEntire(withoutNumber) ?: return withoutNumber
  val genericNumber = generic.number()
  return withoutNumber.takeIf { genericNumber != null && genericNumber != number }
}

private fun MatchResult.number(): Int? =
  groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.toIntOrNull()
