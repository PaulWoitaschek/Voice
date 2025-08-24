package voice.scanner.matroska

internal data class MatroskaChapter(
  val startTime: Long,
  private val names: List<MatroskaChapterName>,
) {

  fun bestName(preferredLanguages: List<String>): String? {
    // Try preferred languages first
    for (language in preferredLanguages) {
      names.find { language in it.languages }?.let { return it.name }
    }

    // Fall back to first available name or default
    return names.firstOrNull()?.name
  }
}
