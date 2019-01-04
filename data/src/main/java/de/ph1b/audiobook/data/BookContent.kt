package de.ph1b.audiobook.data

import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import java.util.UUID

data class BookContent(
  val id: UUID,
  val chapters: List<Chapter>,
  val settings: BookSettings
) {

  inline fun updateSettings(update: BookSettings.() -> BookSettings) = copy(
    settings = update(settings)
  )

  init {
    if (BuildConfig.DEBUG) {
      chapters.forEach {
        require(it.bookId == id) { "Wrong chapter book id in $this" }
      }
    }
  }

  val currentChapter = chapters.first { it.file == settings.currentFile }
  val currentChapterIndex = chapters.indexOf(currentChapter)
  val previousChapter = chapters.getOrNull(currentChapterIndex - 1)
  val nextChapter = chapters.getOrNull(currentChapterIndex + 1)
  val nextChapterMarkPosition: Int? by lazy {
    currentChapter.marks.forEachIndexed { _, start, _ ->
      if (start > settings.positionInChapter) return@lazy start
    }
    null
  }
  val duration = chapters.sumBy { it.duration }
  val position: Int = chapters.take(currentChapterIndex).sumBy { it.duration } + settings.positionInChapter
  val currentFile = settings.currentFile
  val positionInChapter = settings.positionInChapter
  val loudnessGain = settings.loudnessGain
  val skipSilence = settings.skipSilence
  val playbackSpeed = settings.playbackSpeed
}
