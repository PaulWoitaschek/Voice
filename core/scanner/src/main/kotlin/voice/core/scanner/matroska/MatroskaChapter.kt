package voice.core.scanner.matroska

internal data class MatroskaChapter(
  val startTime: Long,
  private val names: List<MatroskaChapterName>,
) {

  fun bestName(preferredLanguages: List<String>): String? {
    for (language in preferredLanguages) {
      names.find { language in it.languages }?.let { return it.name }
    }

    return names.firstOrNull()?.name
  }
}
