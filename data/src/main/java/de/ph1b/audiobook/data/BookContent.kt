package de.ph1b.audiobook.data

import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import java.io.File

data class BookContent(
  val id: Long,
  val currentFile: File,
  val positionInChapter: Int,
  val chapters: List<Chapter>,
  val playbackSpeed: Float = 1F,
  val loudnessGain: Int = 0
) {

  init {
    require(playbackSpeed >= Book.SPEED_MIN) { "speed $playbackSpeed must be >= ${Book.SPEED_MIN}" }
    require(playbackSpeed <= Book.SPEED_MAX) { "speed $playbackSpeed must be <= ${Book.SPEED_MAX}" }
    require(positionInChapter >= 0) { "positionInChapter must not be negative" }
    require(loudnessGain >= 0) { "loudnessGain must not be negative" }
  }

  val currentChapter = chapters.first { it.file == currentFile }
  val currentChapterIndex = chapters.indexOf(currentChapter)
  val previousChapter = chapters.getOrNull(currentChapterIndex - 1)
  val nextChapter = chapters.getOrNull(currentChapterIndex + 1)
  val nextChapterMarkPosition: Int? by lazy {
    currentChapter.marks.forEachIndexed { _, start, _ ->
      if (start > positionInChapter) return@lazy start
    }
    null
  }
  val duration = chapters.sumBy { it.duration }
  val position: Int = chapters.take(currentChapterIndex).sumBy { it.duration } + positionInChapter
}
