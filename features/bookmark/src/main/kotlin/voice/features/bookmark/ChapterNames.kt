package voice.features.bookmark

private val audioExtension = Regex("""\.(mp3|m4a|m4b|aac|ogg|oga|opus|flac|wav|wma|mka|mp4)$""", RegexOption.IGNORE_CASE)
private val leadingNumber = Regex("""^\d+\s*[-_.:)\]]*\s*""")
private val digits = Regex("""\d+""")
private val genericWords = setOf(
  "chapter",
  "track",
  "part",
  "disc",
  "cd",
  "kapitel",
  "teil",
  "chapitre",
  "partie",
  "capitulo",
  "capítulo",
  "parte",
  "hoofdstuk",
  "rozdział",
  "глава",
  "часть",
)

/**
 * Keeps only the chapter names that tell something about the chapter.
 *
 * Names like "Chapter 11", "Track 03" or a file name repeated with a running number are dropped,
 * while "03 - The Signal" becomes "The Signal". A book with a single chapter has no chapter names,
 * as that name is usually the book's or the file's.
 */
internal fun meaningfulChapterNames(names: List<String?>): List<String?> {
  if (names.size <= 1) return names.map { null }
  val cleaned = names.map { name ->
    name?.trim()
      ?.replace(audioExtension, "")
      ?.replace(leadingNumber, "")
      ?.trim()
      ?.takeIf { cleanedName -> cleanedName.any { it.isLetter() } }
  }
  val skeletons = cleaned.map { it?.replace(digits, "#")?.lowercase() }
  val skeletonCounts = skeletons.filterNotNull().groupingBy { it }.eachCount()
  return cleaned.mapIndexed { index, name ->
    name?.takeIf { skeletonCounts[skeletons[index]] == 1 && !it.isGeneric() }
  }
}

private fun String.isGeneric(): Boolean {
  val words = lowercase().split(Regex("""[^\p{L}]+""")).filter { it.isNotEmpty() }
  return words.all { it in genericWords }
}
