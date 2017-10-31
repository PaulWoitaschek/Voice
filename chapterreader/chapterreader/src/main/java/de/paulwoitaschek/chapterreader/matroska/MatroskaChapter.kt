package de.paulwoitaschek.chapterreader.matroska

internal data class MatroskaChapter(
  val startTime: Long,
  private val names: List<MatroskaChapterName>,
  val children: List<MatroskaChapter>
) {

  fun name(preferredLanguages: List<String> = emptyList()): String? = preferredLanguages
    .mapNotNull { language ->
      names.find { language in it.languages }
        ?.name
    }
    .firstOrNull()
    ?: names.firstOrNull()?.name
}
