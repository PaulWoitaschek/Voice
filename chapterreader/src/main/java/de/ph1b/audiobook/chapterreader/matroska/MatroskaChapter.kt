package de.ph1b.audiobook.chapterreader.matroska

internal data class MatroskaChapter(
    val startTime: Long,
    val names: List<MatroskaChapterName>,
    val children: List<MatroskaChapter>
) {

  fun getName(preferredLanguages: List<String> = emptyList<String>()): String? = preferredLanguages
      .mapNotNull { language ->
        names.find { language in it.languages }
            ?.name
      }
      .firstOrNull()
      ?: names.firstOrNull()?.name
}
